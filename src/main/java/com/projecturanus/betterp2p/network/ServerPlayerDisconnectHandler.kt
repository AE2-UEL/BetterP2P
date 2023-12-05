package com.projecturanus.betterp2p.network

import com.projecturanus.betterp2p.MODID
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.relauncher.Side

@Mod.EventBusSubscriber(modid = MODID, value = [Side.SERVER])
object ServerPlayerDisconnectHandler {
    @JvmStatic
    @SubscribeEvent
    fun onLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        ModNetwork.removeConnection(event.player)
    }
}
