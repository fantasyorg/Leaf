package com.redefantasy.server.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Called when a player in creative mode duplicates a stack with the middle mouse button, at the
 * exact point the server copies it ({@code AbstractContainerMenu#doClick}, {@code ClickType.CLONE}).
 * <p>
 * This exists because the duplication is otherwise invisible: the clone is a byte-for-byte copy,
 * so anything the original carried in NBT — ownership, identity, tracking — silently exists twice.
 * Observing it from the API side means reacting to {@code InventoryClickEvent} and reading the
 * cursor a tick later, which guesses at timing and races anything else touching that cursor. Here
 * there is no gap: the clone has been built and has not been handed to the player yet.
 * <p>
 * {@link #setClone(ItemStack)} replaces what the player receives, which is the point — a listener
 * can re-stamp the copy so it is not mistaken for the original. Cancelling stops the duplication
 * and leaves the cursor empty.
 */
@NullMarked
public class CreativeItemCloneEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack source;
    private ItemStack clone;
    private boolean cancelled;

    public CreativeItemCloneEvent(final Player player, final ItemStack source, final ItemStack clone) {
        super(player);
        this.source = source;
        this.clone = clone;
    }

    /**
     * The stack being copied, as it sits in the clicked slot. Modifying it does not affect the
     * clone — use {@link #setClone(ItemStack)} for that.
     *
     * @return the original {@link ItemStack}
     */
    public ItemStack getSource() {
        return this.source;
    }

    /**
     * The copy the player is about to receive on the cursor.
     *
     * @return the cloned {@link ItemStack}
     */
    public ItemStack getClone() {
        return this.clone;
    }

    /**
     * Replaces the copy the player receives.
     *
     * @param clone the {@link ItemStack} to hand over instead
     */
    public void setClone(final ItemStack clone) {
        this.clone = clone;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
