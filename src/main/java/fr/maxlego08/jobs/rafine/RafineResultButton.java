package fr.maxlego08.jobs.rafine;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result slot button (yaml type {@code ZJOBS_ITEM_RAFINE_RESULT}).
 * <p>
 * Rendering follows the same RecipeBook-style pattern as
 * {@link RafineInputButton}: {@link #hasSpecialRender()} is {@code true} and
 * {@link #onRender(Player, InventoryEngine)} pushes the current item into the
 * Bukkit inventory via {@link InventoryEngine#addItem(int, ItemStack)}.
 * <p>
 * Three states are displayed :
 * <ul>
 *     <li><b>No deposit</b> : the yaml placeholder (typically a barrier).</li>
 *     <li><b>REFINING</b> : a furnace-like placeholder with the remaining time.</li>
 *     <li><b>READY</b> : a preview of the refined output — clicking
 *     immediately gives the refined item to the player.</li>
 * </ul>
 */
public class RafineResultButton extends Button {

    private final JobsPlugin plugin;

    public RafineResultButton(Plugin plugin) {
        this.plugin = (JobsPlugin) plugin;
        setUseCache(false);
        setUpdated(true);
    }

    private RafineManager manager() {
        return this.plugin.getRafineManager();
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public void onRender(Player player, InventoryEngine inventory) {
        ItemStack stack = computeStack(player);
        if (stack != null) inventory.addItem(getSlot(), stack);
    }

    private ItemStack computeStack(Player player) {
        RafineManager.Deposit deposit = findDeposit(player);
        if (deposit == null) {
            // No active deposit - fall back to yaml placeholder (barrier)
            return getCustomItemStack(player);
        }

        if (deposit.isRefining()) {
            ItemStack inProgress = new ItemStack(Material.FURNACE);
            ItemMeta meta = inProgress.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e⌛ Raffinage en cours..."));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Temps restant : &f" + RafineManager.formatTime(deposit.getRemainingSeconds())));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Chance de réussite : &e" + deposit.getPercent() + "%"));
                meta.setLore(lore);
                inProgress.setItemMeta(meta);
            }
            return inProgress;
        }

        // READY - preview the refined output so the player sees what he will get.
        ItemStack output = manager().buildResult(deposit);
        ItemStack preview = (output != null && output.getType() != Material.AIR)
                ? output.clone()
                : deposit.getItemStack().clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a✔ Raffinage terminé !"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eCliquez pour récupérer l'item."));
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        event.setCancelled(true);

        Map<Integer, RafineManager.Deposit> deposits = manager().getDeposits(player);
        Integer readySlot = null;
        RafineManager.Deposit ready = null;
        for (Map.Entry<Integer, RafineManager.Deposit> entry : deposits.entrySet()) {
            if (entry.getValue().isReady()) {
                readySlot = entry.getKey();
                ready = entry.getValue();
                break;
            }
        }

        if (ready == null) return;

        ItemStack output = manager().buildResult(ready);
        if (output == null || output.getType() == Material.AIR) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cAucune recette configurée pour ce pourcentage. Contactez un administrateur."));
            RafineInputButton.refreshRafineButtons(inventory);
            return;
        }

        manager().clearSlot(player, readySlot);

        var leftover = player.getInventory().addItem(output);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));

        String outputId = manager().getItemId(output);
        if (outputId != null) {
            this.plugin.getJobManager().action(player, outputId, JobActionType.RAFINE);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✔ Item raffiné récupéré !"));

        RafineInputButton.refreshRafineButtons(inventory);
    }

    private RafineManager.Deposit findDeposit(Player player) {
        RafineManager.Deposit refining = null;
        for (RafineManager.Deposit deposit : manager().getDeposits(player).values()) {
            if (deposit.isReady()) return deposit;
            if (refining == null) refining = deposit;
        }
        return refining;
    }
}
