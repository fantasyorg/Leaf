package org.dreeam.leaf.config.modules.opt;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

/**
 * Dois cortes pra mundo decorativo (spawn, lobby): nao varrer os chunks de spawn de mob quando nenhuma
 * categoria pode nascer, e nao recalcular a lista de quem ve uma entidade parada a cada tick.
 */
public class IdleWorldTicking extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.PERF.getBaseKeyName() + ".idle-world";
    }

    public static boolean skipSpawnScanWithoutSpawning = false;
    public static int staticEntityTrackerInterval = 1;

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(getBasePath(), """
                Savings for worlds that mostly sit still (spawn, lobby).""",
            """
                针对基本静止的世界(出生点, 大厅)的节省.""");

        skipSpawnScanWithoutSpawning = config.getBoolean(getBasePath() + ".skip-spawn-scan-without-spawning", skipSpawnScanWithoutSpawning,
            config.pickStringRegionBased("""
                    Skip the per-tick scan of chunks around players when no mob category can spawn
                    (spawn limits at 0, mob spawning off) and it is not thundering. Chunks then stop
                    accumulating inhabited time, which only feeds local difficulty.""",
                """
                    当没有生物类别可以生成(生成上限为 0, 生成关闭)且没有雷暴时, 跳过每刻对玩家周围区块的扫描.
                    区块将不再累积居住时间, 这只影响局部难度."""));
        staticEntityTrackerInterval = Math.max(1, config.getInt(getBasePath() + ".static-entity-tracker-interval", staticEntityTrackerInterval,
            config.pickStringRegionBased("""
                    How many ticks between viewer recalculations for a non-player entity that did not
                    move or turn since last tick. 1 = every tick (vanilla). Metadata and movement
                    packets still go out every tick; only who starts or stops seeing the entity can
                    lag behind by up to this many ticks.""",
                """
                    未移动或转向的非玩家实体两次观察者重算之间的刻数. 1 = 每刻(原版).
                    元数据和移动数据包仍每刻发送; 只有开始或停止看到该实体的判定最多延迟这么多刻.""")));
    }
}
