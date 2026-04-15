package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class EntityAction extends ZJobAction<EntityType> {

    private final JobActionType actionType;

    public EntityAction(EntityType target, double experience, double money, JobActionType actionType, String displayMaterial) {
        super(target, experience, money, displayMaterial);
        this.actionType = actionType;
    }

    @Override
    public JobActionType getType() {
        return actionType;
    }

    @Override
    public boolean isAction(Object target) {
        if (target instanceof EntityType entityType) return this.target == entityType;
        return target instanceof Entity entity && this.target == entity.getType();
    }
}
