package com.projecturanus.betterp2p.network.data

import appeng.api.config.SecurityPermissions
import appeng.api.networking.IGrid
import appeng.api.networking.security.ISecurityGrid
import appeng.api.parts.IPart
import appeng.api.parts.PartItemStack
import appeng.api.util.AEPartLocation
import appeng.helpers.IInterfaceHost
import appeng.me.GridAccessException
import appeng.me.cache.P2PCache
import appeng.parts.automation.UpgradeInventory
import appeng.parts.p2p.PartP2PTunnel
import appeng.tile.inventory.AppEngInternalInventory
import appeng.util.Platform
import com.projecturanus.betterp2p.BetterP2P
import com.projecturanus.betterp2p.util.p2p.TunnelInfo
import com.projecturanus.betterp2p.util.p2p.getTypeIndex
import com.projecturanus.betterp2p.util.p2p.outputProperty
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumHand
import net.minecraft.util.text.TextComponentTranslation

/**
 * When the player uses the adv memory card, this is cached on the server side
 * to provide access to the Grid when the player performs actions in the GUI
 * Each player has a list of p2ps that will be sent. These are tracked in [listP2P].
 */
class GridServerCache(private val grid: IGrid, val player: EntityPlayer, var type: Int) {
    /**
     * The P2P list. On init, this is the full list.
     */
    private val listP2P: MutableMap<P2PLocation, PartP2PTunnel<*>> = mutableMapOf()

    /**
     * The dirty P2P list. Updates are accumulated here and sent altogether.
     */
    private val dirtyP2P: MutableSet<P2PLocation> = mutableSetOf()

    init {
        rebuildList(type)
    }

    /**
     * Refreshes the global p2p list
     */
    private fun rebuildList(type: Int) {
        synchronized(listP2P) {
            listP2P.clear()
            dirtyP2P.clear()
            grid.machinesClasses.forEach {
                // Find all P2P tunnels...
                if (it.superclass == PartP2PTunnel::class.java &&
                    (type == TUNNEL_ANY || BetterP2P.proxy.getP2PFromIndex(type)?.clazz == it)) {
                    grid.getMachines(it).forEach { gridNode ->
                        val p2p = gridNode.machine as PartP2PTunnel<*>
                        listP2P[p2p.toLoc()] = p2p
                    }
                }
            }
        }
    }

    /**
     * Refreshes and gets the p2p list of the targeted type
     * If [type] is [TUNNEL_ANY] or invalid, returns the full list; else filters the list to the
     * targeted type
     */
    fun retrieveP2PList(): List<P2PInfo> {
        if (grid is ISecurityGrid && !grid.hasPermission(player, SecurityPermissions.BUILD)) {
            return emptyList()
        }
        rebuildList(type)

        return listP2P.values.mapNotNull {
            val index = BetterP2P.proxy.getP2PFromClass(it.javaClass)?.index ?: TUNNEL_ANY
            if (type == TUNNEL_ANY || index == type) {
                it.toInfo()
            } else null
        }
    }

    /**
     * Sets the entry to the p2p tunnel and marks it as dirty to be sent in the next network update.
     * Only p2ps of the currently targeted type can be shown
     */
    fun markDirty(key: P2PLocation, p2p: PartP2PTunnel<*>) {
        synchronized(listP2P) {
            if (type == TUNNEL_ANY || p2p.getTypeIndex() == type) {
                listP2P[key] = p2p
                dirtyP2P.add(key)
            }
        }
    }

    /**
     * Returns the list of P2Ps that are currently marked dirty, and clears the dirty list.
     */
    fun getP2PUpdates(): List<P2PInfo> {
        val result = dirtyP2P.mapNotNull {
            listP2P[it]?.toInfo()
        }

        dirtyP2P.clear()

        return result;
    }

    /**
     * Link the two P2P tunnels together. Returns the pair of P2P tunnels on success, or null otherwise.
     */
    fun linkP2P(inputIndex: P2PLocation, outputIndex: P2PLocation):
        Pair<PartP2PTunnel<*>, PartP2PTunnel<*>>? {
        // If these calls mess up we have bigger problems...
        val input = listP2P[inputIndex] ?: return null
        var output = listP2P[outputIndex] ?: return null

        // Double check security permissions. In theory, the cache won't be set up in the first place
        // for intruders.
        if (grid is ISecurityGrid && !grid.hasPermission(player, SecurityPermissions.BUILD)) {
            return null
        }

        //change type if necessary
        if (input.javaClass != output.javaClass) {
            output = changeP2PType(output, BetterP2P.proxy.getP2PFromClass(input.javaClass)!!) ?: return null
        }

        // Network loop
        if (input == output) {
            return null
        }

        var frequency = input.frequency
        val cache = input.proxy.p2P

        // Generate a new frequency if needed
        if (input.frequency == 0.toShort() || input.isOutput) {
            frequency = System.currentTimeMillis().toShort()
        }


        // If tunnel was already bound, unbind that one
        if (cache.getInputs(frequency, input::class.java).first() != null) {
            val originalInput = cache.getInputs(frequency, input::class.java).first()
            if (originalInput != input) {
                updateP2P(originalInput.toLoc(), originalInput, frequency, true, input.customInventoryName)
            }
        }

        // Perform the link
        val inputResult: PartP2PTunnel<*> = updateP2P(inputIndex, input, frequency, false, input.customInventoryName)
        val outputResult: PartP2PTunnel<*> = updateP2P(outputIndex, output, frequency, true, input.customInventoryName)

        // Special case for interfaces
        if (input is IInterfaceHost && output is IInterfaceHost) {
            val drops = mutableListOf<ItemStack>()
            handleInterface(input, output, inputResult as IInterfaceHost, outputResult as IInterfaceHost, drops)
            Platform.spawnDrops(player.world, output.location.pos, drops)
        }

        return inputResult to outputResult
    }

    fun unlinkP2P(p2pIndex: P2PLocation): PartP2PTunnel<*>? {
        val tunnel = listP2P[p2pIndex] ?: return null
        val oldFreq = tunnel.frequency
        if (oldFreq == 0.toShort()) {
            return tunnel
        }

        return updateP2P(p2pIndex, tunnel, 0.toShort(), false, tunnel.customInventoryName)
    }

    /**
     * Sets the p2p tunnel to the frequency, output, and custom name. Removes the old one and replaces it, which lets
     * AE2 trigger the Grid refresh for us (though we need to update the tunnels ourselves)
     */
    private fun updateP2P(key: P2PLocation, tunnel: PartP2PTunnel<*>, frequency: Short, output: Boolean, name: String): PartP2PTunnel<*> {
        val side = tunnel.side
        val data = NBTTagCompound()

        tunnel.host.removePart(side, true)

        val p2pItem: ItemStack = tunnel.getItemStack(PartItemStack.WRENCH)


        p2pItem.writeToNBT(data)
        data.setShort("freq", frequency)

        val newType = ItemStack(data)
        val dir: AEPartLocation = tunnel.host?.addPart(newType, side, player, EnumHand.MAIN_HAND) ?: throw RuntimeException("Cannot bind")
        val newBus: IPart = tunnel.host.getPart(dir)
        if (newBus is PartP2PTunnel<*>) {
            newBus.outputProperty = output
            if (name.isNotBlank()) {
                newBus.setCustomName(name)
            }
            try {
                val p2p = newBus.proxy.p2P
                p2p.updateFreq(newBus, frequency)
            } catch (e: GridAccessException) {
                // :P
            }
            newBus.onTunnelNetworkChange()
            markDirty(key, newBus)
            return newBus
        } else {
            throw IllegalStateException("Cannot bind")
        }
    }

    /**
     * Converts one P2P into the type
     */
    private fun changeP2PType(tunnel: PartP2PTunnel<*>, newType: TunnelInfo): PartP2PTunnel<*>? {
        if (BetterP2P.proxy.getP2PFromClass(tunnel.javaClass) == newType) {
            player.sendMessage(TextComponentTranslation("gui.advanced_memory_card.error.same_type"))
            return null
        }
        /* TODO Static P2Ps
        // Change to a static P2P type = require p2p in inventory
        if (newType.clazz.superclass == PartP2PTunnelStatic::class.java) {
            var canConvert = false
            for ((i, stack) in player.inventory.mainInventory.withIndex()) {
                if (stack?.isItemEqual(newType.stack) == true) {
                    player.inventory.decrStackSize(i, 1)
                    canConvert = true
                    break
                }
            }
            if (!canConvert) {
                player.addChatMessage(ChatComponentTranslation("gui.advanced_memory_card.error.missing_items", 1, newType.stack.displayName))
                return null
            }
        }
        // Change from a static P2P type = give p2p back to the player
        if (tunnel is PartP2PTunnelStatic<*>) {
            val drop = ItemStack.copyItemStack(tunnel.itemStack)
            drop.stackSize = 1
            if (!player.inventory.addItemStackToInventory(drop)) {
                val drops = mutableListOf<ItemStack>(drop)
                tunnel.getDrops(drops, false)
                Platform.spawnDrops(player.worldObj, player.serverPosX, player.serverPosY, player.serverPosZ, drops)
            }
        }
         */

        val host = tunnel.host
        host.removePart(tunnel.side, false)
        val dir = host.addPart(newType.stack, tunnel.side, player, EnumHand.MAIN_HAND)
        val newPart = host.getPart(dir)
        if (newPart is PartP2PTunnel<*>) {
            newPart.outputProperty = tunnel.isOutput
            try {
                val p2p: P2PCache = newPart.proxy.p2P
                p2p.updateFreq(newPart, tunnel.frequency)
                return newPart
            } catch (e: GridAccessException) {
                // :P
            }
        }
        return null
    }

    /**
     * Converts all connected P2Ps to a new type
     */
    fun changeAllP2Ps(p2p: P2PLocation, newType: TunnelInfo): Boolean {
        if (grid is ISecurityGrid && !grid.hasPermission(player, SecurityPermissions.BUILD)) {
            return false
        }

        var tunnel = listP2P[p2p] ?: return false

        try {
            if (tunnel.isOutput && tunnel.inputs.first() != null) {
                tunnel = tunnel.inputs.first()
            }

            val outputs = tunnel.outputs.toMutableList()
            /* TODO: static p2p
            if (newType.clazz.superclass == PartP2PTunnelStatic::class.java) {
                val amt = outputs.size + 1
                var hasItems = 0
                for (stack in player.inventory.mainInventory) {
                    if (stack?.isItemEqual(newType.stack) == true) {
                        hasItems += stack.stackSize
                        if (hasItems >= amt) {
                            break
                        }
                    }
                }
                if (hasItems < amt) {
                    player.addChatMessage(ChatComponentTranslation("gui.advanced_memory_card.error.missing_items", amt, newType.stack.displayName))
                    return false
                }
            }
             */
            changeP2PType(tunnel, newType)
            for (o in outputs) {
                changeP2PType(o, newType)
            }
            return true
        } catch (e: GridAccessException) {
            // :P
        }
        return false
    }

    /**
     * Handles a p2p interface/dual interface case.
     */
    private fun handleInterface(oldIn: IInterfaceHost, oldOut: IInterfaceHost,
                                newIn: IInterfaceHost, newOut: IInterfaceHost,
                                drops: MutableList<ItemStack>) {
        // For input and output, retain upgrades, items, and settings.
        val upgradesIn = oldIn.interfaceDuality.getInventoryByName("upgrades") as UpgradeInventory
        upgradesIn.forEachIndexed { index, stack ->
            (newIn.interfaceDuality.getInventoryByName("upgrades") as UpgradeInventory).setStackInSlot(index, stack)
        }
        val upgradesOut = oldOut.interfaceDuality.getInventoryByName("upgrades") as UpgradeInventory
        upgradesOut.forEachIndexed { index, stack ->
            (newOut.interfaceDuality.getInventoryByName("upgrades") as UpgradeInventory).setStackInSlot(index, stack)
        }
        val itemsIn = oldIn.interfaceDuality.storage as AppEngInternalInventory
        itemsIn.forEachIndexed { index, stack ->
            (newIn.interfaceDuality.storage as AppEngInternalInventory).setStackInSlot(index, stack)
        }
        val itemsOut = oldOut.interfaceDuality.storage as AppEngInternalInventory
        itemsOut.forEachIndexed { index, stack ->
            (newOut.interfaceDuality.storage as AppEngInternalInventory).setStackInSlot(index, stack)
        }
        val settingsIn = oldIn.interfaceDuality.configManager
        settingsIn.settings.forEach {
            newIn.configManager.putSetting(it, settingsIn.getSetting(it))
        }
        val settingsOut = oldOut.interfaceDuality.configManager
        settingsOut.settings.forEach {
            newOut.configManager.putSetting(it, settingsOut.getSetting(it))
        }

        // For input, just copy the patterns over
        val patternsIn = oldIn.interfaceDuality.patterns as AppEngInternalInventory
        patternsIn.forEachIndexed { index, stack ->
            (newIn.interfaceDuality.patterns as AppEngInternalInventory).setStackInSlot(index, stack)
        }
        // For output, drop items
        val patternsOut = oldOut.interfaceDuality.patterns as AppEngInternalInventory
        drops.addAll(patternsOut)
    }
}
