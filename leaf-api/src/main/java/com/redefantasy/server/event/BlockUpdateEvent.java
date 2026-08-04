package com.redefantasy.server.event;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called whenever a block in a loaded world actually changes state — from any source
 * (vanilla, plugin {@code setType}/{@code setBlockData}, physics, piston, explosion).
 * <p>
 * This is a root, server-level observation event fired at the single choke point every
 * loaded-world block write funnels through ({@code LevelChunk#setBlockState}), AFTER the
 * old-equals-new short-circuit, so {@link #getOldState()} and {@link #getNewState()} are
 * always different. World generation is NOT covered (it writes through a different code
 * path), which is intentional.
 * <p>
 * The event is fired only while the owning world is flagged as recording, so it costs
 * nothing when nobody is listening. It is not cancellable: it observes committed writes,
 * it does not gate them. It carries no cause/source — attribution (who broke/placed) is
 * tracked separately by gameplay events.
 */
@NullMarked
public class BlockUpdateEvent extends BlockEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BlockData oldState;
    private final BlockData newState;

    public BlockUpdateEvent(final Block block, final BlockData oldState, final BlockData newState) {
        super(block);
        this.oldState = oldState;
        this.newState = newState;
    }

    /**
     * The block data before the change.
     *
     * @return the previous {@link BlockData}
     */
    public BlockData getOldState() {
        return this.oldState;
    }

    /**
     * The block data after the change.
     *
     * @return the new {@link BlockData}
     */
    public BlockData getNewState() {
        return this.newState;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
