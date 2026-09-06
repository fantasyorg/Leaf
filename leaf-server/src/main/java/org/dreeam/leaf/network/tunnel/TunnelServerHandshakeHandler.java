package org.dreeam.leaf.network.tunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.List;
import java.util.function.Supplier;

/**
 * First handler of a tunnel socket on the backend: reads the proxy's handshake, checks the shared
 * secret and swaps itself for the frame codec and the multiplexer. Anything else on this port is
 * closed without an answer.
 */
public final class TunnelServerHandshakeHandler extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(TunnelServerHandshakeHandler.class);

    private final byte[] secret;
    private final Supplier<TunnelMultiplexer> multiplexerFactory;

    public TunnelServerHandshakeHandler(byte[] secret, Supplier<TunnelMultiplexer> multiplexerFactory) {
        this.secret = secret;
        this.multiplexerFactory = multiplexerFactory;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 7) {
            return;
        }

        int start = in.readerIndex();
        if (in.getInt(start) != TunnelProtocol.MAGIC || in.getByte(start + 4) != TunnelProtocol.VERSION) {
            LOGGER.warn("Tunnel port received something that is not a tunnel handshake from {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        int secretLength = in.getUnsignedShort(start + 5);
        if (secretLength > TunnelProtocol.MAX_SECRET_LENGTH) {
            ctx.close();
            return;
        }
        if (in.readableBytes() < 7 + secretLength) {
            return;
        }

        in.skipBytes(7);
        byte[] offered = new byte[secretLength];
        in.readBytes(offered);

        if (!MessageDigest.isEqual(offered, this.secret)) {
            LOGGER.warn("Tunnel handshake from {} with a wrong secret", ctx.channel().remoteAddress());
            ctx.writeAndFlush(TunnelProtocol.encodeHandshakeReply(ctx.alloc(), TunnelProtocol.STATUS_REJECTED)).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        ctx.pipeline().addLast("tunnel-decoder", new TunnelFrameCodec.Decoder());
        ctx.pipeline().addLast("tunnel-encoder", new TunnelFrameCodec.Encoder());
        ctx.pipeline().addLast("tunnel-multiplexer", this.multiplexerFactory.get());
        ctx.pipeline().remove(this);

        ctx.writeAndFlush(TunnelProtocol.encodeHandshakeReply(ctx.alloc(), TunnelProtocol.STATUS_OK));
        LOGGER.info("Proxy tunnel established from {}", ctx.channel().remoteAddress());
    }
}
