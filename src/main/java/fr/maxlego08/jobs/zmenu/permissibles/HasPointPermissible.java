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

public class HasPointPermissible extends Permissible {

    private final JobsPlugin plugin;
    private final String points;

    public HasPointPermissible(JobsPlugin plugin, String points, List<Action> denyActions, List<Action> successActions) {
        super(denyActions, successActions);
        this.plugin = plugin;
        this.points = points;
    }

    @Override
    public boolean hasPermission(Player player, Button button, InventoryEngine inventoryEngine, Placeholders placeholders) {

        var optional = this.plugin.getJobManager().getPlayerJobs(player.getUniqueId());
        if (optional.isEmpty()) return false;

        var playerJobs = optional.get();
        var finalPoints = Integer.parseInt(placeholders.parse(!this.points.contains("%") ? this.points : Placeholder.getPlaceholder().setPlaceholders(player, this.points)));
        return playerJobs.getPoints() >= finalPoints;
    }

    @Override
    public boolean isValid() {
        return this.points != null;
    }
}
