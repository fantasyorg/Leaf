package org.dreeam.leaf.config.modules.misc;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

/**
 * O usercache.json: nome→UUID de quem já entrou. Desligado, o servidor não carrega, não anota no
 * login e não grava o arquivo; consulta por nome offline cai direto no resolvedor de perfil.
 */
public class UserCache extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.MISC.getBaseKeyName() + ".user-cache";
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        enabled = config.getBoolean(getBasePath() + ".enabled", enabled,
            config.pickStringRegionBased("""
                    Keep usercache.json (name to UUID of everyone who ever joined). Disabled, the server
                    neither loads it, records logins into it, nor writes it; offline name lookups go
                    straight to the profile repository. Vanilla rewrites the whole file on every login.""",
                """
                    是否维护 usercache.json(所有加入过的玩家的名字到 UUID). 关闭后服务器不加载, 不在登录时记录,
                    也不写入该文件; 离线名字查询直接走档案仓库. 原版在每次登录时都会重写整个文件."""));
    }
}
