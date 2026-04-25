package fr.maxlego08.jobs.rafine;

import org.bukkit.plugin.Plugin;

/**
 * Tier-1 refine input slot (yaml type {@code ZJOBS_ITEM_RAFINE_1}).
 * <p>
 * Identical to {@link RafineInputButton} except the random refine duration
 * is bounded by {@link RafineManager#MIN_REFINE_SECONDS} and 240 seconds, and
 * the success chance of the refining roll is increased by 1 percent point.
 */
public class RafineInputButton1 extends RafineInputButton {

    public RafineInputButton1(Plugin plugin) {
        super(plugin);
    }

    @Override
    public int getMaxSeconds() {
        return 240;
    }

    @Override
    public int getBonusPercent() {
        return 1;
    }
}
