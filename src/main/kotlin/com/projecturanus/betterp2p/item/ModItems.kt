package com.projecturanus.betterp2p.item

import com.projecturanus.betterp2p.Tags
import net.minecraft.item.Item
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Mod.EventBusSubscriber(modid = Tags.MODID)
object ModItems {
  @JvmStatic
  @SubscribeEvent
  fun registerItems(event: RegistryEvent.Register<Item>) {
    event.registry.register(
        ItemAdvancedMemoryCard.setRegistryName(Tags.MODID, "advanced_memory_card"))
  }
}
