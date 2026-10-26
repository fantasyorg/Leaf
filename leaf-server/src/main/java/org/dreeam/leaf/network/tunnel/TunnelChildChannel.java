package org.dreeam.leaf.network.tunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * One player's stream on the tunnel, seen by the rest of the server as an ordinary Netty channel:
 * it has its own pipeline and event loop, reads deliver the stream's bytes, and writes become DATA
 * frames handed to the tunnel socket without a flush of their own. Closing it closes the stream,
 * never the socket.
 *
 * <p>The channel may live on a different event loop than the socket, and which loop is decided by
 * whoever registers it (the server's accept path picks one like it does for a socket). Everything
 * here runs on the channel's loop; the multiplexer crosses to the socket's loop when it forwards a
 * frame, and hands deliveries to this loop through {@link #onLoop}, which holds them until the loop
 * is known.
 *
 * <p>Flow control is a byte window per stream. Writes stop when the peer's credits run out and
 * resume when a WINDOW frame arrives; delivered bytes are credited back to the peer as the pipeline
 * consumes them, so a stream whose reader paused ({@code autoRead=false}) stops the peer instead of
 * the whole socket.
 */
public final class TunnelChildChannel extends AbstractChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private final TunnelMultiplexer multiplexer;
    private final int streamId;
    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;
    private final ChannelConfig config = new DefaultChannelConfig(this);
    private final ArrayDeque<ByteBuf> inbound = new ArrayDeque<>();
    private final int windowBytes;
    private final Object handoffLock = new Object();
    /** Deliveries that arrived before registration; null once the loop is known. */
    private List<Runnable> beforeRegister = new ArrayList<>();

    private volatile boolean open = true;
    private volatile boolean registeredActive;
    private boolean peerClosed;
    private boolean readPending;
    private boolean writeBlocked;
    private int sendCredits;
    private int unreturnedCredits;

    public TunnelChildChannel(Channel socket, TunnelMultiplexer multiplexer, int streamId, InetSocketAddress remoteAddress, InetSocketAddress localAddress, int windowBytes) {
        super(socket);
        this.multiplexer = multiplexer;
        this.streamId = streamId;
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        this.windowBytes = windowBytes;
        this.sendCredits = windowBytes;
    }

    public int streamId() {
        return this.streamId;
    }

    /**
     * Runs a task on this channel's loop, in order with the other tasks handed over this way. Before
     * the channel is registered the loop is unknown, so the task waits and runs right after the
     * registration completes.
     */
    public void onLoop(Runnable task) {
        synchronized (this.handoffLock) {
            if (this.beforeRegister != null) {
                this.beforeRegister.add(task);
                return;
            }
        }

        EventLoop loop = eventLoop();
        if (loop.inEventLoop()) {
            task.run();
        } else {
            loop.execute(task);
        }
    }

    // ---- called by the multiplexer, always on this channel's loop ----

    void deliver(List<ByteBuf> payloads) {
        if (!this.open) {
            payloads.forEach(ReferenceCountUtil::release);
            return;
        }
        this.inbound.addAll(payloads);
        drainInbound();
    }

    void addCredits(int credits) {
        this.sendCredits += credits;
        if (this.writeBlocked && this.open) {
            this.writeBlocked = false;
            unsafe().flush();
        }
    }

    void peerClosed() {
        this.peerClosed = true;
        unsafe().close(unsafe().voidPromise());
    }

    // ---- AbstractChannel ----

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ChannelConfig config() {
        return this.config;
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    @Override
    public boolean isActive() {
        return this.open && this.registeredActive;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                promise.setFailure(new UnsupportedOperationException("A tunnel stream is opened by the multiplexer"));
            }
        };
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof SingleThreadEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.localAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.remoteAddress;
    }

    @Override
    protected void doRegister() {
        this.registeredActive = true;

        // Held deliveries run after the registration finishes (channelActive included), still ahead
        // of anything handed over from now on: the flip and the enqueues share the lock.
        synchronized (this.handoffLock) {
            List<Runnable> held = this.beforeRegister;
            this.beforeRegister = null;
            for (Runnable task : held) {
                eventLoop().execute(task);
            }
        }
    }

    @Override
    protected void doBind(SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() {
        doClose();
    }

    @Override
    protected void doClose() {
        if (!this.open) {
            return;
        }
        this.open = false;

        ByteBuf queued;
        while ((queued = this.inbound.poll()) != null) {
            queued.release();
        }

        if (!this.peerClosed) {
            this.multiplexer.send(TunnelFrame.close(this.streamId));
        }
        this.multiplexer.streamClosed(this.streamId);
    }

    @Override
    protected void doBeginRead() {
        this.readPending = true;
        drainInbound();
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) {
        while (true) {
            Object message = in.current();
            if (message == null) {
                return;
            }

            if (!(message instanceof ByteBuf buf)) {
                in.remove(new UnsupportedOperationException("Tunnel streams carry ByteBuf only, got " + message.getClass().getName()));
                continue;
            }

            int readable = buf.readableBytes();
            if (readable == 0) {
                in.remove();
                continue;
            }

            if (this.sendCredits <= 0) {
                this.writeBlocked = true;
                return;
            }

            int length = Math.min(readable, Math.min(this.sendCredits, TunnelProtocol.MAX_PAYLOAD_LENGTH));
            this.multiplexer.send(TunnelFrame.data(this.streamId, buf.retainedSlice(buf.readerIndex(), length)));
            this.sendCredits -= length;
            in.removeBytes(length);
        }
    }

    private void drainInbound() {
        if (!this.readPending || this.inbound.isEmpty()) {
            return;
        }
        this.readPending = false;

        int delivered = 0;
        ByteBuf buf;
        while ((buf = this.inbound.poll()) != null) {
            delivered += buf.readableBytes();
            pipeline().fireChannelRead(buf);
        }
        pipeline().fireChannelReadComplete();

        this.unreturnedCredits += delivered;
        if (this.unreturnedCredits >= this.windowBytes / 4) {
            this.multiplexer.send(TunnelFrame.window(alloc(), this.streamId, this.unreturnedCredits));
            this.unreturnedCredits = 0;
        }
    }
}
