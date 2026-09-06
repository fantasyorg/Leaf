package org.dreeam.leaf.network.tunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lets the tunnel share the game port. The first four bytes tell the two apart: a tunnel starts with
 * {@link TunnelProtocol#MAGIC}, which no Minecraft handshake (length varint, then packet id 0) nor
 * legacy ping (0xFE) can produce. Whatever it is, the bytes are replayed into the pipeline chosen.
 */
public final class TunnelPortSniffer extends ByteToMessageDecoder {
    private final Consumer<Channel> gameInitializer;
    private final Supplier<TunnelServerHandshakeHandler> tunnelHandshake;

    public TunnelPortSniffer(Consumer<Channel> gameInitializer, Supplier<TunnelServerHandshakeHandler> tunnelHandshake) {
        this.gameInitializer = gameInitializer;
        this.tunnelHandshake = tunnelHandshake;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }

        if (in.getInt(in.readerIndex()) == TunnelProtocol.MAGIC) {
            ctx.pipeline().addLast("tunnel-handshake", this.tunnelHandshake.get());
        } else {
            // The game handlers arrive after the channel went active: they get the event they missed
            // (Connection learns its channel there) before the buffered bytes reach them.
            this.gameInitializer.accept(ctx.channel());
            ctx.fireChannelActive();
        }

        ctx.pipeline().remove(this);
    }
}
