package fr.maxlego08.jobs.zmenu.permissibles;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.placeholder.Placeholder;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.requirement.Permissible;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;

import java.util.List;

public class HasPrestigePermissible extends Permissible {

    private final JobsPlugin plugin;
    private final String jobName;
    private final String prestige;

    public HasPrestigePermissible(List<Action> denyActions, List<Action> successActions, JobsPlugin plugin, String jobName, String prestige) {
        super(denyActions, successActions);
        this.plugin = plugin;
        this.jobName = jobName;
        this.prestige = prestige;
    }

    @Override
    public boolean hasPermission(Player player, Button button, InventoryEngine inventoryEngine, Placeholders placeholders) {

        var finalPrestige = Integer.parseInt(placeholders.parse(!this.prestige.contains("%") ? this.prestige : Placeholder.getPlaceholder().setPlaceholders(player, this.prestige)));
        var finalJobName = placeholders.parse(!this.jobName.contains("%") ? this.jobName : Placeholder.getPlaceholder().setPlaceholders(player, this.jobName));

        var optionalJob = this.plugin.getJobManager().getJob(finalJobName);
        if (optionalJob.isEmpty()) return false;
        var job = optionalJob.get();

        var optional = this.plugin.getJobManager().getPlayerJobs(player.getUniqueId());
        if (optional.isEmpty()) return false;

        var playerJobs = optional.get();
        var optionalPlayerJob = playerJobs.get(job);
        if (optionalPlayerJob.isPresent()) {
            var playerJob = optionalPlayerJob.get();
            return playerJob.getPrestige() >= finalPrestige;
        }
        return false;
    }

    @Override
    public boolean isValid() {
        return this.jobName != null && this.prestige != null;
    }
}
