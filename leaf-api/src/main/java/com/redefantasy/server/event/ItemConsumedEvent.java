package com.redefantasy.server.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Called when units of a stack are used up by gameplay, at the single point the server spends them
 * ({@code ItemStack#consume}). Placing a block, eating, drinking, throwing an ender pearl and
 * roughly fifty other vanilla uses all funnel through it.
 * <p>
 * This is a root, server-level observation event. It exists because none of those losses can be
 * observed honestly from the API: the matching Bukkit events fire <i>before</i> the fact and can
 * still be cancelled by someone else, or never report how much was actually spent. Here the amount
 * is exact and the decision is already made.
 * <p>
 * Fired BEFORE the stack shrinks, so {@link #getStack()} still describes what is being spent.
 * Creative mode never reaches this point, since the server skips the spend entirely.
 * <p>
 * ⚠️ {@link #getStack()} is a live view of the real stack, not a copy — this fires on every block
 * placed by every player, and copying the NBT each time is not worth it. Read it, do not modify it,
 * and do not hold on to it: once the spend goes through it reflects the shrunk stack. Take a
 * {@code clone()} if you need to keep it.
 * <p>
 * The event is fired only while something is listening, so it costs nothing otherwise. It is not
 * cancellable: it observes a spend that has already been decided, it does not gate it.
 */
@NullMarked
public class ItemConsumedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable LivingEntity entity;
    private final ItemStack stack;
    private final int amount;

    public ItemConsumedEvent(final @Nullable LivingEntity entity, final ItemStack stack, final int amount) {
        this.entity = entity;
        this.stack = stack;
        this.amount = amount;
    }

    /**
     * Who spent it, when the server knows. Null for spends with no entity behind them, such as a
     * dispenser firing.
     *
     * @return the {@link LivingEntity} that consumed the units, or null
     */
    public @Nullable LivingEntity getEntity() {
        return this.entity;
    }

    /**
     * A copy of the stack as it was before the spend.
     *
     * @return the {@link ItemStack} being spent
     */
    public ItemStack getStack() {
        return this.stack;
    }

    /**
     * How many units are being spent.
     *
     * @return the consumed amount
     */
    public int getAmount() {
        return this.amount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
