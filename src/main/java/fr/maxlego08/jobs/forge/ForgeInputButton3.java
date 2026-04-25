package fr.maxlego08.jobs.forge;

import org.bukkit.plugin.Plugin;

/**
 * Tier-3 forge input slot (yaml type {@code ZJOBS_ITEM_FORGE_3}).
 * <p>
 * Same behaviour as {@link ForgeInputButton} but raises the maximum forging
 * duration to 360 seconds and grants a +3% bonus on the chance to produce the
 * recipe's highest-rarity output (see {@link ForgeManager#computeLuckResult}).
 */
public class ForgeInputButton3 extends ForgeInputButton {

    public ForgeInputButton3(Plugin plugin) {
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
