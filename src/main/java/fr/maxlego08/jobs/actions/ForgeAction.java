package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

/**
 * Action of type {@link JobActionType#FORGE} (or one of its tier variants
 * {@link JobActionType#FORGE_1}, {@link JobActionType#FORGE_2},
 * {@link JobActionType#FORGE_3}, {@link JobActionType#FORGE_4}).
 * <p>
 * The {@code target} stored by this action is the identifier of the <b>forged</b>
 * item the player receives when the forge completes successfully (for example
 * {@code nexo:amethyst_sword_commun}). The recipe itself (ingredients and fail
 * chance) is declared in {@code items.yml} and resolved at runtime by
 * {@link fr.maxlego08.jobs.forge.ForgeManager}.
 * </p>
 * <p>
 * The concrete {@link JobActionType} is preserved so jobs can grant different
 * XP/money rewards depending on the tier of the forge inventory the player
 * used. Tier 0 ({@link JobActionType#FORGE}) corresponds to the base
 * {@code ZJOBS_ITEM_FORGE} input slot; tiers 1..4 correspond to
 * {@code ZJOBS_ITEM_FORGE_1..4}.
 * </p>
 */
public class ForgeAction extends ZJobAction<String> {

    private final JobActionType type;

    public ForgeAction(String targetId, double experience, double money, String displayMaterial) {
        this(targetId, experience, money, displayMaterial, JobActionType.FORGE);
    }

    public ForgeAction(String targetId, double experience, double money, String displayMaterial, JobActionType type) {
        super(targetId, experience, money, displayMaterial);
        this.type = type == null ? JobActionType.FORGE : type;
    }

    @Override
    public JobActionType getType() {
        return this.type;
    }

    @Override
    public boolean isAction(Object target) {
        return target instanceof String s && s.equalsIgnoreCase(this.target);
    }
}
