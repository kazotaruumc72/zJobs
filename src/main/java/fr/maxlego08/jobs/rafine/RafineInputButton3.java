package fr.maxlego08.jobs.rafine;

import org.bukkit.plugin.Plugin;

/**
 * Tier-3 refine input slot (yaml type {@code ZJOBS_ITEM_RAFINE_3}).
 * <p>
 * Identical to {@link RafineInputButton} except the random refine duration
 * is bounded by {@link RafineManager#MIN_REFINE_SECONDS} and 360 seconds, and
 * the success chance of the refining roll is increased by 3 percent points.
 */
public class RafineInputButton3 extends RafineInputButton {

    public RafineInputButton3(Plugin plugin) {
        super(plugin);
    }

    @Override
    public int getMaxSeconds() {
        return 360;
    }

    @Override
    public int getBonusPercent() {
        return 3;
    }
}
