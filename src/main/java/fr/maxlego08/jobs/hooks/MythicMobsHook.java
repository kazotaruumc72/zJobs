package fr.maxlego08.jobs.hooks;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;

public class MythicMobsHook {

    /**
     * Check if an entity is a MythicMobs entity.
     *
     * @param entity the entity to check
     * @return true if the entity is a MythicMobs entity
     */
    public boolean isMythicMob(Entity entity) {
        return MythicBukkit.inst().getMobManager().isActiveMob(entity.getUniqueId());
    }

    /**
     * Get the MythicMobs internal name (mob type) for an entity.
     *
     * @param entity the entity to get the name for
     * @return the MythicMobs internal name, or null if the entity is not a MythicMobs entity
     */
    public String getMythicMobType(Entity entity) {
        ActiveMob activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
        return activeMob != null ? activeMob.getMobType() : null;
    }
}
