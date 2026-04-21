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
 * Timer display button (yaml type {@code ZJOBS_ITEM_RAFINE_TIMER}).
 * <p>
 * Uses the RecipeBook-style rendering pattern (see {@link RafineInputButton}).
 * Shows in real time the shortest remaining refinement time across every
 * input slot of the currently-opened menu.
 */
public class RafineTimerButton extends Button {

    private final JobsPlugin plugin;

    public RafineTimerButton(Plugin plugin) {
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
        ItemStack base = getCustomItemStack(player);
        if (base == null) return null;

        long shortest = Long.MAX_VALUE;
        boolean anyReady = false;
        int refining = 0;

        for (RafineManager.Deposit deposit : manager().getDeposits(player).values()) {
            if (deposit.isReady()) {
                anyReady = true;
            } else if (deposit.isRefining()) {
                refining++;
                if (deposit.getRemainingSeconds() < shortest) {
                    shortest = deposit.getRemainingSeconds();
                }
            }
        }

        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : "";

        if (refining > 0) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    "&e⏳ Raffinage : &f" + RafineManager.formatTime(shortest) + " &7restantes"));
        } else if (anyReady) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    "&a✔ Raffinage terminé"));
        } else if (!displayName.isEmpty()) {
            meta.setDisplayName(displayName);
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (refining > 1) {
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + refining + " items en cours de raffinage."));
        }
        meta.setLore(lore);

        base.setItemMeta(meta);
        return base;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        event.setCancelled(true);
    }
}
