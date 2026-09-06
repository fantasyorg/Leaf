package org.dreeam.leaf.network.tunnel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Backend side of a tunnel socket: every OPEN becomes a stream channel registered on one of the
 * server's network event loops and initialised exactly like an accepted socket, so the rest of the
 * server (Connection, handshake, login, play) never knows the difference.
 */
public final class TunnelServerMultiplexer extends TunnelMultiplexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TunnelServerMultiplexer.class);

    private final EventLoopGroup streamLoops;
    private final InetSocketAddress localAddress;
    private final Consumer<Channel> connectionInitializer;
    private final BooleanSupplier accepting;

    public TunnelServerMultiplexer(EventLoopGroup streamLoops, InetSocketAddress localAddress, Consumer<Channel> connectionInitializer, BooleanSupplier accepting, int flushIntervalMillis, int windowBytes) {
        super(false, flushIntervalMillis, windowBytes);
        this.streamLoops = streamLoops;
        this.localAddress = localAddress;
        this.connectionInitializer = connectionInitializer;
        this.accepting = accepting;
    }

    @Override
    protected void onOpen(int streamId, InetSocketAddress remote) {
        if (!this.accepting.getAsBoolean()) {
            send(TunnelFrame.close(streamId));
            return;
        }

        EventLoop loop = this.streamLoops.next();
        TunnelChildChannel child = new TunnelChildChannel(context().channel(), this, streamId, remote, this.localAddress, loop, this.windowBytes);
        registerStream(child);

        child.pipeline().addLast(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                TunnelServerMultiplexer.this.connectionInitializer.accept(channel);
            }
        });

        loop.register(child).addListener(future -> {
            if (!future.isSuccess()) {
                LOGGER.warn("Could not register tunnel stream {} for {}", streamId, remote, future.cause());
                child.close();
            }
        });
    }

    @Override
    protected void onSocketClosed() {
        LOGGER.info("Proxy tunnel from {} closed", context().channel().remoteAddress());
    }
}
