package com.redefantasy.server.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called once per tick, per entity, whenever that entity's synchronized metadata (the
 * data-watcher) changed and is being sent to clients — e.g. TNT fuse, entity pose, creeper
 * charging/ignited, item-frame item, player sneaking/gliding/hand-active, etc.
 * <p>
 * This is a root, server-level observation event fired at the point the dirty metadata is
 * packed and dispatched ({@code ServerEntity#sendDirtyEntityData}, and — on the async
 * tracker — its main-thread commit). It fires on the thread that owns the entity, so
 * handlers may safely read from the entity.
 * <p>
 * The event deliberately carries no raw metadata payload: the changed values are internal
 * NMS types. Consumers should read whatever they need from {@link #getEntity()} (e.g.
 * {@code ((TNTPrimed) getEntity()).getFuseTicks()}). It exists to signal <em>when</em>
 * visual state changed — the trigger that scattered gameplay events do not provide.
 * <p>
 * Fired only while the owning world is flagged as recording, so it costs nothing when
 * nobody is listening. Not cancellable.
 */
@NullMarked
public class EntityMetadataUpdateEvent extends EntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public EntityMetadataUpdateEvent(final Entity entity) {
        super(entity);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
