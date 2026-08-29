package org.dreeam.leaf.async.chunk;

import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.dreeam.leaf.config.modules.opt.CacheChunkPackets;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Pacotes de chunk prontos, por mundo. O pacote de chunk nao depende do jogador (fora o Anti-Xray,
 * que passa por fora daqui), entao o mesmo objeto serve pra todo mundo que entra: o Netty serializa
 * cada envio num buffer proprio, a partir de dados que o pacote nunca altera.
 *
 * <p>Caffeine com expiracao por acesso: chunk que ninguem recebe ha um tempo solta a memoria sozinho,
 * e o teto por mundo segura o resto. Thread-safe porque o sender assincrono constroi pacotes fora da
 * main thread enquanto a main thread invalida entradas ao mudar bloco.
 */
@NullMarked
public final class ChunkPacketCache {

    private final Cache<Long, ClientboundLevelChunkWithLightPacket> packets;
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder invalidations = new LongAdder();

    private ChunkPacketCache() {
        this.packets = Caffeine.newBuilder()
            .maximumSize(CacheChunkPackets.maxChunksPerWorld)
            .expireAfterAccess(Duration.ofMinutes(CacheChunkPackets.expireAfterAccessMinutes))
            .build();
    }

    /** O cache do mundo, ou null quando a config nao o cobre. */
    public static @Nullable ChunkPacketCache of(ServerLevel level) {
        return level.leaf$chunkPacketCache;
    }

    public static @Nullable ChunkPacketCache create(ServerLevel level) {
        return CacheChunkPackets.appliesTo(level) ? new ChunkPacketCache() : null;
    }

    /** O pacote guardado, ou null se este chunk ainda nao foi construido (ou foi invalidado). */
    public @Nullable ClientboundLevelChunkWithLightPacket peek(int chunkX, int chunkZ) {
        ClientboundLevelChunkWithLightPacket cached = this.packets.getIfPresent(CoordinateUtils.getChunkKey(chunkX, chunkZ));
        if (cached != null) {
            this.hits.increment();
        }
        return cached;
    }

    /** O pacote do chunk, construindo e guardando na primeira vez. */
    public ClientboundLevelChunkWithLightPacket get(int chunkX, int chunkZ, Supplier<ClientboundLevelChunkWithLightPacket> build) {
        long key = CoordinateUtils.getChunkKey(chunkX, chunkZ);
        ClientboundLevelChunkWithLightPacket cached = this.packets.getIfPresent(key);
        if (cached != null) {
            this.hits.increment();
            return cached;
        }

        this.misses.increment();
        ClientboundLevelChunkWithLightPacket built = build.get();
        this.packets.put(key, built);
        return built;
    }

    public void invalidate(int chunkX, int chunkZ) {
        long key = CoordinateUtils.getChunkKey(chunkX, chunkZ);
        if (this.packets.getIfPresent(key) != null) {
            this.packets.invalidate(key);
            this.invalidations.increment();
        }
    }

    public void invalidateAll() {
        this.invalidations.add(this.size());
        this.packets.invalidateAll();
    }

    public long size() {
        return this.packets.estimatedSize();
    }

    public long hits() {
        return this.hits.sum();
    }

    public long misses() {
        return this.misses.sum();
    }

    public long invalidations() {
        return this.invalidations.sum();
    }

    /** Esvazia o cache de todos os mundos que tem um; devolve quantos pacotes sairam. */
    public static long clearAll() {
        long cleared = 0;
        for (ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
            ChunkPacketCache cache = of(level);
            if (cache != null) {
                cleared += cache.size();
                cache.invalidateAll();
            }
        }
        return cleared;
    }
}
