package com.redefantasy.server.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called once per tick, per entity, whenever that entity's synchronized metadata (the data-watcher)
 * changed and is being sent to clients. Fired at the point the dirty metadata is packed and
 * dispatched ({@code ServerEntity#sendDirtyEntityData}, and on the async tracker its main-thread
 * commit), on the thread that owns the entity.
 * <p>
 * Carries the entity's full non-default data-watcher serialized in the vanilla entity-metadata wire
 * format (the {@code ClientboundSetEntityDataPacket} value list, without the entity id). This lets a
 * consumer reproduce <em>any</em> metadata generically (creeper charged, mob variants, potion-effect
 * colour, item-frame item, pose…) without per-type code.
 * <p>
 * Fired only while the owning world is flagged as recording, so it costs nothing when nobody is
 * listening. Not cancellable.
 */
@NullMarked
public class EntityMetadataUpdateEvent extends EntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final byte[] metadata;

    public EntityMetadataUpdateEvent(final Entity entity, final byte[] metadata) {
        super(entity);
        this.metadata = metadata;
    }

    /** The full non-default data-watcher, serialized in the vanilla entity-metadata wire format. */
    public byte[] getMetadata() {
        return this.metadata;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
