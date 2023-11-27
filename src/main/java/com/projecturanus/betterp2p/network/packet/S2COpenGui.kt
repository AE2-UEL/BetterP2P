package com.projecturanus.betterp2p.network.packet

import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard
import com.projecturanus.betterp2p.network.data.*
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class S2COpenGui(var infos: List<P2PInfo> = emptyList(),
                 var memoryInfo: MemoryInfo = MemoryInfo()) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        val length = buf.readInt()
        val list = ArrayList<P2PInfo>(length)

        for (i in 0 until length) {
            val info = readP2PInfo(buf)

            if (info != null) {
                list.add(info)
            }
        }

        infos = list
        memoryInfo = readMemoryInfo(buf)
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(infos.size)
        for (info in infos) {
            writeP2PInfo(buf, info)
        }
        writeMemoryInfo(buf, memoryInfo)
    }
}

class ClientOpenGuiHandler : IMessageHandler<S2COpenGui, IMessage?> {
    @SideOnly(Side.CLIENT)
    override fun onMessage(message: S2COpenGui, ctx: MessageContext): IMessage? {
        val gui = Minecraft.getMinecraft().currentScreen
        if (gui is GuiAdvancedMemoryCard) {
            gui.refreshInfo(message.infos)
        } else {
            Minecraft.getMinecraft().addScheduledTask {
                Minecraft.getMinecraft().displayGuiScreen(GuiAdvancedMemoryCard(message))
            }
        }
        return null
    }
}
