package org.dreeam.leaf.config.modules.network;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

public class ProxyTunnel extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.NETWORK.getBaseKeyName() + ".proxy-tunnel";
    }

    public static boolean enabled = false;
    /** 0 binds the game port + 1. */
    public static int port = 0;
    public static int flushIntervalMillis = 10;
    public static int windowBytes = 1 << 20;

    @Override
    public void onLoaded() {
        enabled = config.getBoolean(getBasePath() + ".enabled", enabled, config.pickStringRegionBased("""
                Accepts a multiplexed connection from the Velocity proxy: one TCP socket per proxy
                carries every player as a stream, and the socket is flushed on a timer instead of
                once per packet. Requires the matching tunnel support on the proxy and the Velocity
                modern forwarding secret, which authenticates the tunnel.

                Requires server restart to take effect.""",
            """
                接受来自 Velocity 代理的多路复用连接: 每个代理一个 TCP 套接字, 承载所有玩家的流,
                套接字按定时器刷新而不是每个数据包刷新一次. 需要代理端的隧道支持和 Velocity
                现代转发密钥 (用于认证隧道).

                需要重启服务器才能生效."""));
        port = config.getInt(getBasePath() + ".port", port, config.pickStringRegionBased("""
                Port the tunnel listens on. 0 uses the game port + 1.""",
            """
                隧道监听的端口. 0 表示使用游戏端口 + 1."""));
        flushIntervalMillis = config.getInt(getBasePath() + ".flush-interval-millis", flushIntervalMillis, config.pickStringRegionBased("""
                How often the tunnel socket is flushed. Lower is less latency, higher is fewer syscalls.""",
            """
                隧道套接字的刷新间隔. 越低延迟越小, 越高系统调用越少."""));
        windowBytes = config.getInt(getBasePath() + ".window-bytes", windowBytes, config.pickStringRegionBased("""
                Bytes buffered per player stream before the sender waits for the receiver.""",
            """
                每个玩家流在发送方等待接收方之前缓冲的字节数."""));
    }
}
