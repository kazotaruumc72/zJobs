package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

public class MythicMobAction extends ZJobAction<String> {

    private final JobActionType actionType;

    public MythicMobAction(String mythicMobId, double experience, double money, JobActionType actionType, String displayMaterial) {
              super(mythicMobId, experience, money, displayMaterial);
              this.actionType = actionType;
    }

    @Override
      public JobActionType getType() {
                return actionType;
      }

    @Override
      public boolean isAction(Object target) {
                if (target instanceof String s) {
                              return s.equalsIgnoreCase(this.target);
                }
                return false;
      }
}
