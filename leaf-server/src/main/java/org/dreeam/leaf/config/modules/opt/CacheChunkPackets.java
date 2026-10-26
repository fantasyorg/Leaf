package org.dreeam.leaf.config.modules.opt;

import net.minecraft.server.level.ServerLevel;
import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusa o pacote de chunk entre jogadores em mundos que nao mudam (lobby, auth, spawn): o primeiro
 * envio serializa o chunk, os seguintes mandam o mesmo objeto. Qualquer mudanca de bloco, luz ou
 * block entity no chunk descarta a entrada; o chunk descarregar tambem.
 */
public class CacheChunkPackets extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.PERF.getBaseKeyName() + ".chunk-packet-cache";
    }

    public static boolean enabled = false;
    /** Vazio = todos os mundos. */
    public static List<String> worlds = new ArrayList<>();
    public static int maxChunksPerWorld = 4096;
    public static int expireAfterAccessMinutes = 10;

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(getBasePath(), """
                Reuse chunk packets between players in worlds that do not change (lobby, auth, spawn).
                The first send serialises the chunk; every later send of the same chunk is a copy of that
                packet. Any block, light or block entity change in a chunk drops its entry, and so does
                unloading it. Disabled automatically for chunks Anti-Xray needs to obfuscate.""",
            """
                在不变的世界(大厅, 登录, 出生点)中在玩家之间复用区块数据包.
                首次发送序列化区块, 之后同一区块的每次发送都是该数据包的副本.
                区块内任何方块, 光照或方块实体变化都会丢弃缓存, 卸载区块亦然.
                反矿透需要混淆的区块会自动禁用.""");

        enabled = config.getBoolean(getBasePath() + ".enabled", enabled);
        worlds = config.getList(getBasePath() + ".worlds", worlds,
            config.pickStringRegionBased("""
                    Worlds the cache applies to. Empty = every world.""",
                """
                    应用缓存的世界. 留空 = 所有世界."""));
        maxChunksPerWorld = config.getInt(getBasePath() + ".max-chunks-per-world", maxChunksPerWorld,
            config.pickStringRegionBased("""
                    Cap of cached chunks per world; the least recently sent ones leave first.""",
                """
                    每个世界缓存区块的上限; 最久未发送的先被淘汰."""));
        expireAfterAccessMinutes = config.getInt(getBasePath() + ".expire-after-access-minutes", expireAfterAccessMinutes,
            config.pickStringRegionBased("""
                    A cached chunk nobody was sent for this long is dropped to free memory.""",
                """
                    这么长时间内没有发送给任何人的缓存区块将被丢弃以释放内存."""));
    }

    public static boolean appliesTo(ServerLevel level) {
        return enabled && (worlds.isEmpty() || worlds.contains(level.getWorld().getName()));
    }
}
