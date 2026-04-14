package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.entity.Entity;

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
                if (target instanceof Entity entity) {
                              try {
                                                var activeMob = io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
                                                if (activeMob.isPresent()) {
                                                                      String mobType = activeMob.get().getMobType();
                                                                      return this.target.equalsIgnoreCase("mm:" + mobType);
                                                }
                              } catch (Exception ignored) {
                              }
                }
                return false;
      }
}
