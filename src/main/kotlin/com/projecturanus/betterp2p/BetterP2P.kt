package com.projecturanus.betterp2p

import com.projecturanus.betterp2p.config.BetterP2PConfig
import com.projecturanus.betterp2p.network.ModNetwork
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.Logger


@Mod(modid = Tags.MODID, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter", dependencies = "required-after: appliedenergistics2; required-after: forgelin;")
object BetterP2P {
    lateinit var logger: Logger
    @SidedProxy(serverSide = "com.projecturanus.betterp2p.CommonProxy", clientSide = "com.projecturanus.betterp2p.ClientProxy")
    lateinit var proxy: CommonProxy

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        ModNetwork.registerNetwork()

        BetterP2PConfig.loadConfig(Configuration(event.suggestedConfigurationFile))
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        proxy.postInit()
    }
}
