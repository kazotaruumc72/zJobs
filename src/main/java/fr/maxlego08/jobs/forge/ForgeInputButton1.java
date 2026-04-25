package fr.maxlego08.jobs.forge;

import org.bukkit.plugin.Plugin;

/**
 * Tier-1 forge input slot (yaml type {@code ZJOBS_ITEM_FORGE_1}).
 * <p>
 * Same behaviour as {@link ForgeInputButton} but raises the maximum forging
 * duration to 240 seconds and grants a +1% bonus on the chance to produce the
 * recipe's highest-rarity output (see {@link ForgeManager#computeLuckResult}).
 */
public class ForgeInputButton1 extends ForgeInputButton {

    public ForgeInputButton1(Plugin plugin) {
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
