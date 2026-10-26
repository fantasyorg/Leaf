package org.dreeam.leaf.command.subcommands;

import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.dreeam.leaf.async.chunk.ChunkPacketCache;
import org.dreeam.leaf.command.LeafCommand;
import org.dreeam.leaf.command.PermissionedLeafSubcommand;
import org.dreeam.leaf.config.modules.opt.CacheChunkPackets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

/**
 * {@code /leaf chunks cache} mostra o estado do cache de pacotes de chunk por mundo;
 * {@code /leaf chunks clearcache [mundo]} esvazia (o proximo envio de cada chunk reconstroi).
 */
public final class ChunksCommand extends PermissionedLeafSubcommand {

    public static final String LITERAL_ARGUMENT = "chunks";
    public static final String PERM = LeafCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    private static final List<String> ACTIONS = List.of("cache", "clearcache");

    public ChunksCommand() {
        super(PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "cache";

        if (!CacheChunkPackets.enabled) {
            sender.sendMessage(Component.text("Chunk packet cache is disabled (performance.chunk-packet-cache.enabled).", RED));
            return true;
        }

        switch (action) {
            case "cache" -> {
                sender.sendMessage(Component.text("Chunk packet cache", YELLOW));
                for (ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
                    ChunkPacketCache cache = ChunkPacketCache.of(level);
                    String name = level.getWorld().getName();
                    if (cache == null) {
                        sender.sendMessage(Component.text("  " + name + ": not cached", GRAY));
                        continue;
                    }
                    long hits = cache.hits();
                    long misses = cache.misses();
                    long total = hits + misses;
                    String rate = total == 0 ? "-" : (100 * hits / total) + "%";
                    sender.sendMessage(Component.text("  " + name + ": ", GRAY)
                        .append(Component.text(cache.size() + " chunks", GREEN))
                        .append(Component.text(", hits " + hits + " / misses " + misses + " (" + rate + "), invalidated " + cache.invalidations(), GRAY)));
                }
            }
            case "clearcache" -> {
                if (args.length > 1) {
                    ServerLevel level = findLevel(args[1]);
                    ChunkPacketCache cache = level == null ? null : ChunkPacketCache.of(level);
                    if (cache == null) {
                        sender.sendMessage(Component.text("No cached world named '" + args[1] + "'.", RED));
                        return true;
                    }
                    long size = cache.size();
                    cache.invalidateAll();
                    sender.sendMessage(Component.text("Dropped " + size + " cached chunk packets from " + args[1] + ".", GREEN));
                    return true;
                }
                long cleared = ChunkPacketCache.clearAll();
                sender.sendMessage(Component.text("Dropped " + cleared + " cached chunk packets across all worlds.", GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /leaf chunks <cache|clearcache [world]>", RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        if (args.length == 1) {
            return ACTIONS.stream().filter(a -> a.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clearcache")) {
            List<String> names = new ArrayList<>();
            for (ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
                String name = level.getWorld().getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) names.add(name);
            }
            return names;
        }
        return List.of();
    }

    private static ServerLevel findLevel(String name) {
        for (ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
            if (level.getWorld().getName().equalsIgnoreCase(name)) return level;
        }
        return null;
    }
}
