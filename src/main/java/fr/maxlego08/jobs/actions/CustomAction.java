package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

public class CustomAction extends ZJobAction<String> {

    private final JobActionType actionType;

    public CustomAction(String target, double experience, double money, String displayMaterial) {
        this(target, experience, money, displayMaterial, JobActionType.CUSTOM);
    }

    public CustomAction(String target, double experience, double money, String displayMaterial, JobActionType actionType) {
        super(target, experience, money, displayMaterial);
        this.actionType = actionType;
    }

    @Override
    public JobActionType getType() {
        return this.actionType;
    }

    @Override
    public boolean isAction(Object target) {
        return this.target.equals(target);
    }
}
