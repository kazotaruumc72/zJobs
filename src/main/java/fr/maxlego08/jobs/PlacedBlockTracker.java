package fr.maxlego08.jobs;

import org.bukkit.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary, in-memory history of blocks placed by players.
 * <p>
 * It exists to prevent the classic "place / break / place / break" exploit
 * where a player repeatedly places and breaks the same block to farm job
 * points and money. When a player places a block we remember its location for
 * a short, configurable duration. If that same block is broken while still in
 * the history, the break is not rewarded.
 * <p>
 * The history is voluntarily <b>temporary</b>: entries expire after the
 * configured duration so that blocks placed and legitimately broken a long
 * time later (or crops that need time to grow) are still rewarded, and so the
 * map never grows unbounded. A lightweight cleanup task removes expired
 * entries periodically.
 */
public class PlacedBlockTracker {

    // Key = world UID + packed coordinates, value = placement time (millis).
    private final Map<String, Long> placedBlocks = new ConcurrentHashMap<>();

    /**
     * Remembers that a player just placed this block.
     */
    public void trackPlace(Block block) {
        this.placedBlocks.put(key(block), System.currentTimeMillis());
    }

    /**
     * Returns {@code true} if the block was placed by a player within the given
     * window, meaning breaking it should not be rewarded. The record is always
     * consumed so that a subsequent (legitimate) placement starts fresh.
     *
     * @param block          the broken block
     * @param durationMillis how long a placed block stays protected
     */
    public boolean wasRecentlyPlaced(Block block, long durationMillis) {
        Long placedAt = this.placedBlocks.remove(key(block));
        if (placedAt == null) return false;
        return System.currentTimeMillis() - placedAt <= durationMillis;
    }

    /**
     * Removes entries older than the given duration to keep the map bounded.
     */
    public void cleanup(long durationMillis) {
        long now = System.currentTimeMillis();
        this.placedBlocks.values().removeIf(placedAt -> now - placedAt > durationMillis);
    }

    public void clear() {
        this.placedBlocks.clear();
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }
}