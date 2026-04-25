package fr.maxlego08.jobs.forge;

import org.bukkit.plugin.Plugin;

/**
 * Tier-4 forge input slot (yaml type {@code ZJOBS_ITEM_FORGE_4}).
 * <p>
 * Same behaviour as {@link ForgeInputButton} but raises the maximum forging
 * duration to 420 seconds and grants a +4% bonus on the chance to produce the
 * recipe's highest-rarity output (see {@link ForgeManager#computeLuckResult}).
 */
public class ForgeInputButton4 extends ForgeInputButton {

    public ForgeInputButton4(Plugin plugin) {
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
