package fr.maxlego08.jobs.hooks;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MythicMobsListener implements Listener {

    private final JobManager jobManager;

    public MythicMobsListener(JobsPlugin plugin) {
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        Entity killer = event.getKiller();
        if (!(killer instanceof Player player)) return;

        String mobType = event.getMob().getMobType();
        this.jobManager.action(player, "mm:" + mobType, JobActionType.KILL_ENTITY);
    }
}
