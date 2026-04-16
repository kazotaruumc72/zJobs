package fr.maxlego08.jobs.hooks;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MythicMobsListener implements Listener {

    private final JobsPlugin plugin;
    private final JobManager jobManager;

    public MythicMobsListener(JobsPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        // Get the killer
        if (!(event.getKiller() instanceof Player player)) {
            return;
        }

        // Get the MythicMobs mob type
        String mobType = event.getMobType().getInternalName();

        // Dispatch the action with the mm: prefix
        this.jobManager.action(player, "mm:" + mobType, JobActionType.KILL_ENTITY);
    }
}
