package com.projecturanus.betterp2p

import appeng.api.AEApi
import appeng.api.config.TunnelType
import appeng.api.definitions.IItemDefinition
import appeng.parts.p2p.*
import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo
import com.projecturanus.betterp2p.util.p2p.TunnelInfo
import net.minecraft.util.ResourceLocation

/**
 * A proxy for the server
 */
open class CommonProxy {
    /**
     * Tunnels available in this instance. These are used to communicate p2p
     * information between server/client.
     */
    protected val tunnelTypes = mutableMapOf<Class<*>, TunnelInfo>()

    /**
     * Same as above, but maps ints -> tunnel info.
     */
    protected val tunnelIndices = mutableMapOf<Int, TunnelInfo>()

    open fun postInit() {
        initTunnels()
    }
    /**
     * Discover what tunnels are available.
     */
    open fun initTunnels() {
        val partDefs = AEApi.instance().definitions().parts()
        registerTunnel(
            def = partDefs.p2PTunnelME(),
            type = TunnelType.ME,
            classType = PartP2PTunnelME::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelEU(),
            type = TunnelType.IC2_POWER,
            classType = PartP2PIC2Power::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelFE(),
            type = TunnelType.FE_POWER,
            classType = PartP2PFEPower::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelRedstone(),
            type = TunnelType.REDSTONE,
            classType = PartP2PRedstone::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelFluids(),
            type = TunnelType.FLUID,
            classType = PartP2PFluids::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelItems(),
            type = TunnelType.ITEM,
            classType = PartP2PItems::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelLight(),
            type = TunnelType.LIGHT,
            classType = PartP2PLight::class.java)
        registerTunnel(
            def = partDefs.p2PTunnelGTEU(),
            type = TunnelType.GTEU_POWER,
            classType = PartP2PGTCEPower::class.java)
    }

    private fun registerTunnel(def: IItemDefinition, type: TunnelType, classType: Class<out PartP2PTunnel<*>>) {
        if (def.isEnabled) {
            val stack = def.maybeStack(1).get()
            val info = TunnelInfo(type.ordinal, stack, classType)
            tunnelTypes[classType] = info
            tunnelIndices[type.ordinal] = info
        }
    }

    fun getP2PFromIndex(index: Int): TunnelInfo? {
        return tunnelIndices[index]
    }

    fun getP2PFromClass(clazz: Class<*>): TunnelInfo? {
        return tunnelTypes[clazz]
    }

    fun getP2PTypeList(): List<TunnelInfo> {
        return tunnelIndices.values.toList()
    }
}

/**
 * A proxy for the client
 */
class ClientProxy: CommonProxy() {

    /**
     * Keeps a cache of icons to use in GUI.
     */
    override fun initTunnels() {
        val partDefs = AEApi.instance().definitions().parts()
        registerTunnel(
            def = partDefs.p2PTunnelME(),
            type = TunnelType.ME,
            classType = PartP2PTunnelME::class.java,
            icon = ResourceLocation("appliedenergistics2", "textures/blocks/quartz_block.png"))
        registerTunnel(
            def = partDefs.p2PTunnelEU(),
            type = TunnelType.IC2_POWER,
            classType = PartP2PIC2Power::class.java,
            icon = ResourceLocation("minecraft", "textures/blocks/diamond_block.png"))
        registerTunnel(
            def = partDefs.p2PTunnelFE(),
            type = TunnelType.FE_POWER,
            classType = PartP2PFEPower::class.java,
            icon = ResourceLocation("minecraft", "textures/blocks/gold_block.png"))
        registerTunnel(
            def = partDefs.p2PTunnelRedstone(),
            type = TunnelType.REDSTONE,
            classType = PartP2PRedstone::class.java,
            icon = ResourceLocation("appliedenergistics2", "textures/blocks/redstone_p2p.png"))
        registerTunnel(
            def = partDefs.p2PTunnelFluids(),
            type = TunnelType.FLUID,
            classType = PartP2PFluids::class.java,
            icon = ResourceLocation("minecraft", "textures/blocks/lapis_block.png"))
        registerTunnel(
            def = partDefs.p2PTunnelItems(),
            type = TunnelType.ITEM,
            classType = PartP2PItems::class.java,
            icon = ResourceLocation("minecraft", "textures/blocks/hopper_outside.png"))
        registerTunnel(
            def = partDefs.p2PTunnelLight(),
            type = TunnelType.LIGHT,
            classType = PartP2PLight::class.java,
            icon = ResourceLocation("minecraft", "textures/blocks/quartz_block_top.png"))
        registerTunnel(
            def = partDefs.p2PTunnelGTEU(),
            type = TunnelType.GTEU_POWER,
            classType = PartP2PGTCEPower::class.java,
            icon = ResourceLocation("appliedenergistics2", "textures/parts/p2p/power_gteu.png"))
    }

    private inline fun registerTunnel(def: IItemDefinition,
                                      type: TunnelType,
                                      classType: Class<out PartP2PTunnel<*>>,
                                      icon: ResourceLocation) {
        if (def.isEnabled) {
            val stack = def.maybeStack(1).get()
            val info = ClientTunnelInfo(type.ordinal, stack, classType) { icon }
            tunnelTypes[classType] = info
            tunnelIndices[type.ordinal] = info
            BetterP2P.logger.info("p2p {} is enabed", type)
        }
    }
}
