package org.dreeam.leaf.network.tunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lets the tunnel share the game port. The channel is accepted as an ordinary player connection,
 * with the whole game pipeline behind this handler, because plugins that inject at accept time
 * (PacketEvents, ViaVersion) expect that pipeline to exist right away. The first four bytes then
 * tell the two apart: a tunnel starts with {@link TunnelProtocol#MAGIC}, which no Minecraft handshake
 * (length varint, then packet id 0) nor legacy ping (0xFE) can produce. A player just loses this
 * handler; a tunnel loses everything else and gets the handshake instead.
 */
public final class TunnelPortSniffer extends ByteToMessageDecoder {
    public static final String NAME = "tunnel-sniffer";

    private final Supplier<TunnelServerHandshakeHandler> tunnelHandshake;
    private final Consumer<Channel> gameTeardown;

    public TunnelPortSniffer(Supplier<TunnelServerHandshakeHandler> tunnelHandshake, Consumer<Channel> gameTeardown) {
        this.tunnelHandshake = tunnelHandshake;
        this.gameTeardown = gameTeardown;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }

        if (in.getInt(in.readerIndex()) == TunnelProtocol.MAGIC) {
            becomeTunnel(ctx);
        }

        ctx.pipeline().remove(this);
    }

    private void becomeTunnel(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        this.gameTeardown.accept(ctx.channel());

        for (String name : pipeline.names()) {
            ChannelHandler handler = pipeline.get(name);
            if (handler != null && handler != this) {
                pipeline.remove(handler);
            }
        }

        pipeline.addLast("tunnel-handshake", this.tunnelHandshake.get());
    }
}
