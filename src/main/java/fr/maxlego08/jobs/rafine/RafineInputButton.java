package fr.maxlego08.jobs.rafine;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Input slot button (yaml type {@code ZJOBS_ITEM_RAFINE}).
 * <p>
 * Uses the RecipeBook-style rendering approach: {@link #hasSpecialRender()}
 * returns {@code true} and {@link #onRender(Player, InventoryEngine)} writes
 * the current-state item directly into the Bukkit inventory via
 * {@link InventoryEngine#addItem(int, ItemStack)}. This is called every
 * {@code updateInterval} tick of the parent menu, guaranteeing a live swap
 * between states without any cache stall.
 * <p>
 * States :
 * <ul>
 *     <li><b>No deposit</b> : renders the yaml-defined placeholder (gray glass).</li>
 *     <li><b>REFINING</b> : renders the raw deposited item (with its
 *     {@code (XX%)} name) and a lore showing the remaining time.</li>
 *     <li><b>READY</b> : the slot reverts to the yaml placeholder because
 *     the item has visually "moved" to the result slot.</li>
 * </ul>
 * Clicking on an item being refined cancels the refining and gives the raw
 * item back to the player.
 */
public class RafineInputButton extends Button {

    private final JobsPlugin plugin;

    public RafineInputButton(Plugin plugin) {
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

    /**
     * Compute the item to display right now. Falls back to the yaml
     * placeholder via {@link #getCustomItemStack(Player)} when idle / ready.
     */
    private ItemStack computeStack(Player player) {
        RafineManager.Deposit deposit = manager().getDeposit(player, getSlot());
        if (deposit == null || deposit.isReady()) {
            return getCustomItemStack(player);
        }

        ItemStack itemStack = deposit.getItemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e⌛ Raffinage en cours..."));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Temps restant : &f" + RafineManager.formatTime(deposit.getRemainingSeconds())));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Chance de réussite : &e" + deposit.getPercent() + "%"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&cCliquez pour annuler le raffinage."));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        event.setCancelled(true);

        RafineManager.Deposit deposit = manager().getDeposit(player, slot);
        if (deposit == null || deposit.isReady()) return;

        // Cancel refining: give the item back
        ItemStack stack = deposit.getItemStack().clone();
        var leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        manager().clearSlot(player, slot);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&eRaffinage annulé, minerai rendu."));

        refreshRafineButtons(inventory);
    }

    /**
     * Redraw every refinement-related button of the given engine. The
     * engine will invoke {@link #onRender} back, which writes the fresh
     * stack into the Bukkit inventory.
     */
    static void refreshRafineButtons(InventoryEngine engine) {
        for (Button button : engine.getButtons()) {
            if (button instanceof RafineInputButton
                    || button instanceof RafineResultButton
                    || button instanceof RafineTimerButton) {
                engine.displayButton(button);
            }
        }
    }
}
