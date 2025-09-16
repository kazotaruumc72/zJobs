package fr.maxlego08.jobs.zmenu.buttons;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.utils.ValueInformation;
import fr.maxlego08.jobs.zcore.utils.FormatUtils;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;

public class JobValueButton extends PaginateButton {

    private final JobsPlugin plugin;

    public JobValueButton(Plugin plugin) {
        this.plugin = (JobsPlugin) plugin;
    }

    @Override
    public void onInventoryOpen(Player player, InventoryEngine inventory, Placeholders placeholders) {
        super.onInventoryOpen(player, inventory, placeholders);
        var targetJob = this.plugin.getJobManager().getTargetJob(player);
        if (targetJob == null) return;

        placeholders.register("job-name", targetJob.getName());
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public void onRender(Player player, InventoryEngine inventory) {
        var targetJobs = plugin.getJobManager().getTargetJob(player);
        if (targetJobs == null) return;

        var jobActions = targetJobs.getValues().stream().sorted(Comparator.comparingDouble(ValueInformation::experience).reversed()).toList();
        paginate(jobActions, inventory, (slot, value) -> {

            Placeholders placeholders = new Placeholders();
            placeholders.register("experience", FormatUtils.format(value.experience()));
            placeholders.register("money", FormatUtils.format(value.money()));
            placeholders.register("material", value.material());
            placeholders.register("name", value.name());

            ItemStack itemStack = getItemStack().build(player, false, placeholders);
            if (value.applyItemStack() != null) {
                value.applyItemStack().accept(itemStack);
            }

            inventory.addItem(slot, itemStack);
        });
    }

    @Override
    public int getPaginationSize(Player player) {
        var targetJobs = this.plugin.getJobManager().getTargetJob(player);
        return targetJobs == null ? 0 : targetJobs.getValues().size();
    }

    @Override
    public boolean isPermanent() {
        return true;
    }
}
