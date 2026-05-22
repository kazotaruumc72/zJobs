package fr.maxlego08.jobs.forge;

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
 * Timer display button (yaml type {@code ZJOBS_ITEM_FORGE_TIMER}).
 * <p>
 * Follows the same RecipeBook-style rendering pattern as the other forge
 * buttons. Shows live status information and a countdown while the forge
 * timer is running.
 * <p>
 * The YAML may still include {@code %zjobs_forge_time%},
 * {@code %zjobs_forge_status%} and {@code %zjobs_forge_percent%} placeholders
 * &mdash; they are registered by {@link JobsPlugin} so they also work outside
 * of the menu (chat, other plugins, etc).
 */
public class ForgeTimerButton extends Button {

    private final JobsPlugin plugin;

    public ForgeTimerButton(Plugin plugin) {
        this.plugin = (JobsPlugin) plugin;
        setUseCache(false);
        setUpdated(true);
    }

    private ForgeManager manager() {
        return this.plugin.getForgeManager();
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
        ItemStack base = getCustomItemStack(player, false, new Placeholders());
        if (base == null) return null;

        ForgeManager.Session session = manager().getSession(player);
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        String status = manager().getStatus(player);
        String time;
        int failPercent = 0;

        if (session != null) {
            if (session.isForging()) {
                time = ForgeManager.formatTime(session.getRemainingSeconds());
            } else if (session.isReady()) {
                time = "0:00";
            } else {
                time = "--:--";
            }
            if (session.getRecipe() != null) failPercent = session.getRecipe().getFailPercent();
        } else {
            time = "--:--";
        }

        if (session != null && session.isForging()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    "&e⏳ Forgeage : &f" + time + " &7restantes"));
        } else if (session != null && session.isReady()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a✔ Forgeage terminé"));
        } else if (!meta.hasDisplayName()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e⏳ " + time));
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Statut: &f" + status));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Échec: &c" + failPercent + "%"));
        meta.setLore(lore);

        base.setItemMeta(meta);
        return base;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event, Player player, InventoryEngine inventory) {
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getRawSlot() != getSlot()) return;
        event.setCancelled(true);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        event.setCancelled(true);
    }
}
