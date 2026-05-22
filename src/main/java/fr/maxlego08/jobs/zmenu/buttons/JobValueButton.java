package fr.maxlego08.jobs.zmenu.buttons;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zcore.utils.FormatUtils;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

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

        // Display values in the order declared by the config — the user is
        // expected to order them from "least rewarding" (top) to "most
        // rewarding" (bottom), so we do not re-sort here.
        var jobActions = targetJobs.getValues();
        paginate(jobActions, inventory, (slot, value) -> {

            String materialStr = value.material();
            String lowerMaterial = materialStr == null ? "" : materialStr.toLowerCase();
            boolean isNexo = lowerMaterial.startsWith("nexo:");
            boolean isOrestack = lowerMaterial.startsWith("orestack:");

            Placeholders placeholders = new Placeholders();
            placeholders.register("experience", FormatUtils.format(value.experience()));
            placeholders.register("money", FormatUtils.format(value.money()));
            placeholders.register("material", (isNexo || isOrestack) ? "PAPER" : materialStr);
            placeholders.register("name", value.name());

            ItemStack itemStack = getItemStack().build(player, false, placeholders);

            if (isNexo && plugin.getNexoHook() != null) {
                String nexoId = materialStr.substring(5);
                ItemStack nexoItem = plugin.getNexoHook().getItemStack(nexoId);
                if (nexoItem != null) {
                    // Start from the actual Nexo item so we keep every visual
                    // component (type, custom model data, item-model, dyed
                    // color, trim, ...), then overlay the template's
                    // display-name and lore so the placeholders configured in
                    // job_info.yml still show through.
                    ItemStack display = nexoItem.clone();
                    ItemMeta templateMeta = itemStack.getItemMeta();
                    ItemMeta nexoMeta = display.getItemMeta();
                    if (templateMeta != null && nexoMeta != null) {
                        if (templateMeta.hasDisplayName()) {
                            nexoMeta.setDisplayName(templateMeta.getDisplayName());
                        }
                        if (templateMeta.hasLore()) {
                            nexoMeta.setLore(templateMeta.getLore());
                        }
                        display.setItemMeta(nexoMeta);
                    }
                    itemStack = display;
                }
            }

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
