package fr.maxlego08.jobs.rafine;

import org.bukkit.plugin.Plugin;

/**
 * Tier-2 refine input slot (yaml type {@code ZJOBS_ITEM_RAFINE_2}).
 * <p>
 * Identical to {@link RafineInputButton} except the random refine duration
 * is bounded by {@link RafineManager#MIN_REFINE_SECONDS} and 300 seconds, and
 * the success chance of the refining roll is increased by 2 percent points.
 */
public class RafineInputButton2 extends RafineInputButton {

    public RafineInputButton2(Plugin plugin) {
        super(plugin);
    }

    @Override
    public int getMaxSeconds() {
        return 300;
    }

    @Override
    public int getBonusPercent() {
        return 2;
    }
}
