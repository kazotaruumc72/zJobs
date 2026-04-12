package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

public class NexoAction extends ZJobAction<String> {

    private final JobActionType type;

    public NexoAction(String nexoId, double experience, double money, JobActionType type, String displayMaterial) {
        super(nexoId, experience, money, displayMaterial);
        this.type = type;
    }

    @Override
    public JobActionType getType() {
        return this.type;
    }

    @Override
    public boolean isAction(Object target) {
        return target instanceof String s && s.equals(this.target);
    }
}
