package org.dreeam.leaf.config.modules.opt;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

/**
 * Sincroniza o inventario com o cliente quando algo o tocou, em vez de comparar todos os slots
 * a cada tick. Quem escreve num container (slot, inventario do jogador, block entity, GUI de
 * plugin) marca os menus abertos sobre ele como sujos; menu limpo so passa pela varredura
 * completa no intervalo configurado, que segura o caso de item alterado por referencia sem
 * avisar o container.
 */
public class LazyContainerBroadcast extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.PERF.getBaseKeyName() + ".lazy-container-broadcast";
    }

    public static boolean enabled = false;
    public static int fullSweepIntervalTicks = 50;

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(getBasePath(), """
                Sync open menus to the client only when something wrote to them, instead of comparing
                every slot every tick. Writes through slots, the player inventory, block entities and
                plugin GUIs mark the menus that show them; a menu nobody wrote to is only fully swept
                every full-sweep-interval-ticks, which bounds how long an item edited by reference
                (without telling its container) can stay stale.""",
            """
                仅在有写入时同步已打开的菜单, 而不是每刻比较所有槽位.
                通过槽位, 玩家背包, 方块实体和插件 GUI 的写入会标记显示它们的菜单;
                没有写入的菜单只会每 full-sweep-interval-ticks 刻完整扫描一次,
                这限制了通过引用修改(未通知容器)的物品保持过期的时间.""");

        enabled = config.getBoolean(getBasePath() + ".enabled", enabled);
        fullSweepIntervalTicks = Math.max(1, config.getInt(getBasePath() + ".full-sweep-interval-ticks", fullSweepIntervalTicks,
            config.pickStringRegionBased("""
                    Ticks between full slot comparisons of a menu nobody wrote to.""",
                """
                    没有写入的菜单两次完整槽位比较之间的刻数.""")));
    }
}
