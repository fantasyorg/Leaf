package com.redefantasy.server.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called once per tick, per tracked entity, when its position/velocity is being synchronized to
 * clients ({@code ServerEntity#sendChanges}). Unlike {@link io.papermc.paper.event.entity.EntityMoveEvent}
 * — which only fires for {@link org.bukkit.entity.LivingEntity} — this fires for <em>every</em>
 * entity: projectiles, TNT, fireballs, wither skulls, items, falling blocks, minecarts, etc.
 * <p>
 * This is a root, server-level observation event. It carries no payload: consumers read whatever
 * they need from {@link #getEntity()} (e.g. {@code getEntity().getLocation()} /
 * {@code getEntity().getVelocity()}). It fires on the thread that owns the entity, so handlers may
 * safely read from it.
 * <p>
 * Fired only while the owning world is flagged as recording, so it costs nothing when nobody is
 * listening. Not cancellable.
 */
@NullMarked
public class EntityPositionUpdateEvent extends EntityEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public EntityPositionUpdateEvent(final Entity entity) {
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
