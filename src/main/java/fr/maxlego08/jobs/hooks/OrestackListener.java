package fr.maxlego08.jobs.hooks;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import io.github.pigaut.orestack.api.event.GeneratorMineEvent;
import io.github.pigaut.orestack.api.event.GeneratorPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class OrestackListener implements Listener {

    private final JobsPlugin plugin;
    private final JobManager jobManager;

    public OrestackListener(JobsPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGeneratorMine(GeneratorMineEvent event) {
        Player player = event.getPlayer();
        String generatorId = event.getGenerator();

        if (this.plugin.getBlockHook().isTracked(event.getBlockMined())) return;

        String target = "orestack:" + generatorId.toLowerCase();
        this.jobManager.action(player, target, JobActionType.BLOCK_BREAK);
        this.jobManager.action(player, target, JobActionType.FARMING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGeneratorPlace(GeneratorPlaceEvent event) {
        Player player = event.getPlayer();
        String generatorId = event.getGenerator();

        String target = "orestack:" + generatorId.toLowerCase();
        this.jobManager.action(player, target, JobActionType.BLOCK_PLACE);
    }
}