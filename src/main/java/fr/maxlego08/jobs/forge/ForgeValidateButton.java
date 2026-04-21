package fr.maxlego08.jobs.forge;

import fr.maxlego08.jobs.JobsPlugin;
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

/**
 * Validate slot button (yaml type {@code ZJOBS_ITEM_FORGE_VALIDATE}).
 * <p>
 * Renders a clickable confirmation button that the player presses to launch
 * the forge once every required ingredient is in the input slots. While the
 * timer runs the button reflects the current state (in-progress, done) so
 * the player knows that no further action on this slot is needed.
 * <p>
 * If the deposited ingredients do not match any registered recipe the click
 * is rejected with a chat message; otherwise the recipe is started exactly
 * like a click on a deposited ingredient slot.
 */
public class ForgeValidateButton extends Button {

    private final JobsPlugin plugin;

    public ForgeValidateButton(Plugin plugin) {
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
        ForgeManager.Session session = manager().getSession(player);

        if (session != null && session.isForging()) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e⌛ Forgeage en cours..."));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Temps restant : &f" + ForgeManager.formatTime(session.getRemainingSeconds())));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        if (session != null && session.isReady()) {
            ItemStack item = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a✔ Forgeage terminé"));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Récupérez l'item dans le slot résultat."));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        boolean hasDeposits = session != null && !session.getDeposits().isEmpty();
        ItemStack item = new ItemStack(hasDeposits ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (hasDeposits) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a✔ Valider"));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Cliquez pour lancer le forgeage"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7avec les ingrédients déposés."));
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Valider"));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Déposez d'abord des ingrédients"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7dans les slots de dépôt."));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event, Player player, InventoryEngine inventory) {
        // zMenu does not wire Button#onClick for hasSpecialRender() buttons, so we route the click here.
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getRawSlot() != getSlot()) return;
        onClick(player, event, inventory, getSlot(), new Placeholders());
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        event.setCancelled(true);

        ForgeManager mgr = manager();
        ForgeManager.Session session = mgr.getSession(player);

        // Already running or finished: nothing to do here.
        if (session != null && session.hasTimer()) return;

        if (session == null || session.getDeposits().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cDéposez d'abord des ingrédients avant de valider."));
            return;
        }

        ForgeManager.Recipe recipe = mgr.tryStartForging(player);
        if (recipe == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cAucune recette ne correspond aux ingrédients déposés."));
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            } catch (Throwable ignored) {}
            return;
        }

        ForgeManager.Session updated = mgr.getSession(player);
        long remaining = updated == null ? 0L : updated.getRemainingSeconds();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✚ &eForgeage démarré &7(" + ForgeManager.formatTime(remaining) + "&7)."));
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.6f, 1.0f);
        } catch (Throwable ignored) {}

        ForgeInputButton.refreshForgeButtons(inventory);
    }
}
