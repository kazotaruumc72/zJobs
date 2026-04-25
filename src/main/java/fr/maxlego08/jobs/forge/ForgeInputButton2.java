package fr.maxlego08.jobs.forge;

import org.bukkit.plugin.Plugin;

/**
 * Tier-2 forge input slot (yaml type {@code ZJOBS_ITEM_FORGE_2}).
 * <p>
 * Same behaviour as {@link ForgeInputButton} but raises the maximum forging
 * duration to 300 seconds and grants a +2% bonus on the chance to produce the
 * recipe's highest-rarity output (see {@link ForgeManager#computeLuckResult}).
 */
public class ForgeInputButton2 extends ForgeInputButton {

    public ForgeInputButton2(Plugin plugin) {
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
