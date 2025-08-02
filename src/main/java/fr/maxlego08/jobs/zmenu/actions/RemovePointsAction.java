package fr.maxlego08.jobs.zmenu.actions;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.placeholder.Placeholder;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;

public class RemovePointsAction extends Action {

    private final JobsPlugin plugin;
    private final String points;

    public RemovePointsAction(JobsPlugin plugin, String points) {
        this.plugin = plugin;
        this.points = points;
    }

    @Override
    protected void execute(Player player, Button button, InventoryEngine inventoryDefault, Placeholders placeholders) {
        var finalPoints = Integer.parseInt(placeholders.parse(!this.points.contains("%") ? this.points : Placeholder.getPlaceholder().setPlaceholders(player, this.points)));
        this.plugin.getJobManager().removePoints(player.getUniqueId(), finalPoints);
    }
}
