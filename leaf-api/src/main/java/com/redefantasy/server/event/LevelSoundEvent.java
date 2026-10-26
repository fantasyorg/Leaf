package com.redefantasy.server.event;

import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called when the server broadcasts a positional sound ({@code ServerLevel#playSeededSound}) — the
 * path {@code world.playSound} and vanilla sounds go through. Sent as {@code ClientboundSoundPacket}
 * with no Bukkit event, so the replay recorder captures it here.
 * <p>
 * Carries the sound key (e.g. {@code minecraft:entity.generic.explode}), category, world, position,
 * volume, pitch and seed. Fired on the thread that owns the level, only while a listener is
 * registered. Not cancellable.
 */
@NullMarked
public class LevelSoundEvent extends WorldEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String sound;
    private final SoundCategory category;
    private final double x;
    private final double y;
    private final double z;
    private final float volume;
    private final float pitch;
    private final long seed;

    public LevelSoundEvent(final World world, final String sound, final SoundCategory category, final double x, final double y, final double z, final float volume, final float pitch, final long seed) {
        super(world);
        this.sound = sound;
        this.category = category;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    public String getSound() {
        return this.sound;
    }

    public SoundCategory getCategory() {
        return this.category;
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

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public long getSeed() {
        return this.seed;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
