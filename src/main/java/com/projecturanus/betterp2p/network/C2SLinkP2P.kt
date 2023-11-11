package com.projecturanus.betterp2p.network

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

class C2SLinkP2P(var input: Long = NONE, var output: Long = NONE): IMessage {
    override fun fromBytes(buf: ByteBuf) {
        input = buf.readLong()
        output = buf.readLong()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeLong(input)
        buf.writeLong(output)
    }
}
