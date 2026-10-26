package com.redefantasy.server.event;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called when the server sends a particle effect to players ({@code ServerLevel#sendParticles}) —
 * e.g. TNT/creeper explosion clouds, potion swirls, dragon breath, any {@code world.spawnParticle}.
 * These go out as {@code ClientboundLevelParticlesPacket} and have no Bukkit event, so the replay
 * recorder captures them here.
 * <p>
 * The particle (type <em>and</em> data — dust colour, block state, item…) is carried as an opaque
 * blob serialized with the vanilla particle stream codec, so consumers keep the full effect. Fired
 * on the thread that owns the level, only while a listener is registered. Not cancellable.
 */
@NullMarked
public class LevelParticleEvent extends WorldEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final byte[] particle;
    private final double x;
    private final double y;
    private final double z;
    private final int count;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double speed;

    public LevelParticleEvent(final World world, final byte[] particle, final double x, final double y, final double z, final int count, final double offsetX, final double offsetY, final double offsetZ, final double speed) {
        super(world);
        this.particle = particle;
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
    }

    /** The particle type + data, serialized with the vanilla particle stream codec. */
    public byte[] getParticle() {
        return this.particle;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public int getCount() {
        return this.count;
    }

    public double getOffsetX() {
        return this.offsetX;
    }

    public double getOffsetY() {
        return this.offsetY;
    }

    public double getOffsetZ() {
        return this.offsetZ;
    }

    public double getSpeed() {
        return this.speed;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
