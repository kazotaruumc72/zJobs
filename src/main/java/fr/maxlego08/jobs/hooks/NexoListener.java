package fr.maxlego08.jobs.hooks;

import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class NexoListener implements Listener {

    private final JobsPlugin plugin;
    private final JobManager jobManager;

    public NexoListener(JobsPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNexoBlockBreak(NexoBlockBreakEvent event) {
        Player player = event.getPlayer();
        String nexoId = event.getMechanic().getItemID();

        if (this.plugin.getBlockHook().isTracked(event.getBlock())) return;

        this.jobManager.action(player, "nexo:" + nexoId, JobActionType.BLOCK_BREAK);
    }
}
