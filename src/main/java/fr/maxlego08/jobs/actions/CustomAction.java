package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

public class CustomAction extends ZJobAction<String> {

    public CustomAction(String target, double experience, double money, String displayMaterial) {
        super(target, experience, money, displayMaterial);
    }

    @Override
    public JobActionType getType() {
        return JobActionType.CUSTOM;
    }

    @Override
    public boolean isAction(Object target) {
        return this.target.equals(target);
    }
}
