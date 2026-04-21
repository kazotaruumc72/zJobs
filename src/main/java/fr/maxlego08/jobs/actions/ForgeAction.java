package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

/**
 * Action of type {@link JobActionType#FORGE}.
 * <p>
 * The {@code target} stored by this action is the identifier of the <b>forged</b>
 * item the player receives when the forge completes successfully (for example
 * {@code nexo:amethyst_sword_commun}). The recipe itself (ingredients and fail
 * chance) is declared in {@code items.yml} and resolved at runtime by
 * {@link fr.maxlego08.jobs.forge.ForgeManager}.
 * </p>
 */
public class ForgeAction extends ZJobAction<String> {

    public ForgeAction(String targetId, double experience, double money, String displayMaterial) {
        super(targetId, experience, money, displayMaterial);
    }

    @Override
    public JobActionType getType() {
        return JobActionType.FORGE;
    }

    @Override
    public boolean isAction(Object target) {
        return target instanceof String s && s.equalsIgnoreCase(this.target);
    }
}
