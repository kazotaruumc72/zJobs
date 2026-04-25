package fr.maxlego08.jobs.rafine;

import org.bukkit.plugin.Plugin;

/**
 * Tier-4 refine input slot (yaml type {@code ZJOBS_ITEM_RAFINE_4}).
 * <p>
 * Identical to {@link RafineInputButton} except the random refine duration
 * is bounded by {@link RafineManager#MIN_REFINE_SECONDS} and 420 seconds, and
 * the success chance of the refining roll is increased by 4 percent points.
 */
public class RafineInputButton4 extends RafineInputButton {

    public RafineInputButton4(Plugin plugin) {
        super(plugin);
    }

    @Override
    public int getMaxSeconds() {
        return 420;
    }

    @Override
    public int getBonusPercent() {
        return 4;
    }
}
