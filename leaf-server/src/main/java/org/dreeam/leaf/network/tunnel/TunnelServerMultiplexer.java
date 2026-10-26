package org.dreeam.leaf.network.tunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Backend side of a tunnel socket: every OPEN becomes a stream channel handed to the server's own
 * accept path, so it is registered, initialised and seen by plugins exactly like an accepted socket
 * and the rest of the server (Connection, handshake, login, play) never knows the difference.
 */
public final class TunnelServerMultiplexer extends TunnelMultiplexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TunnelServerMultiplexer.class);

    private final InetSocketAddress localAddress;
    private final Predicate<TunnelChildChannel> acceptor;
    private final BooleanSupplier accepting;

    /**
     * @param acceptor puts the stream through the server's accept path; false when there is no
     *                 listener to accept it, and the stream is refused
     */
    public TunnelServerMultiplexer(InetSocketAddress localAddress, Predicate<TunnelChildChannel> acceptor, BooleanSupplier accepting, int flushIntervalMillis, int windowBytes) {
        super(false, flushIntervalMillis, windowBytes);
        this.localAddress = localAddress;
        this.acceptor = acceptor;
        this.accepting = accepting;
    }

    @Override
    protected void onOpen(int streamId, InetSocketAddress remote) {
        if (!this.accepting.getAsBoolean()) {
            send(TunnelFrame.close(streamId));
            return;
        }

        TunnelChildChannel child = new TunnelChildChannel(context().channel(), this, streamId, remote, this.localAddress, this.windowBytes);
        registerStream(child);

        if (!this.acceptor.test(child)) {
            LOGGER.warn("No listener to accept tunnel stream {} for {}", streamId, remote);
            forgetStream(streamId);
            send(TunnelFrame.close(streamId));
        }
    }

    @Override
    protected void onSocketClosed() {
        LOGGER.info("Proxy tunnel from {} closed", context().channel().remoteAddress());
    }
}
