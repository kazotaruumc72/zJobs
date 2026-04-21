package fr.maxlego08.jobs.forge;

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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Result slot button (yaml type {@code ZJOBS_ITEM_FORGE_RESULT}).
 * <p>
 * Three states are displayed :
 * <ul>
 *     <li><b>No active session</b> : the yaml placeholder (typically a barrier).</li>
 *     <li><b>FORGING</b> : an anvil-like placeholder with the remaining time.</li>
 *     <li><b>READY</b> : a preview of the forged output &mdash; clicking rolls
 *     the fail chance; on success the item is given to the player and the FORGE
 *     job action is fired, on failure the ingredients are lost and the session reset.</li>
 * </ul>
 */
public class ForgeResultButton extends Button {

    private final JobsPlugin plugin;

    public ForgeResultButton(Plugin plugin) {
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
        if (session == null || !session.hasTimer()) {
            return getCustomItemStack(player);
        }

        if (session.isForging()) {
            ItemStack inProgress = new ItemStack(Material.ANVIL);
            ItemMeta meta = inProgress.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e⌛ Forgeage en cours..."));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Temps restant : &f" + ForgeManager.formatTime(session.getRemainingSeconds())));
                if (session.getRecipe() != null) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Échec : &c" + session.getRecipe().getFailPercent() + "%"));
                }
                meta.setLore(lore);
                inProgress.setItemMeta(meta);
            }
            return inProgress;
        }

        // READY: preview the forged output.
        ForgeManager.Recipe recipe = session.getRecipe();
        ItemStack output = recipe == null ? null : manager().buildItem(recipe.getOutputId());
        ItemStack preview = (output != null && output.getType() != Material.AIR)
                ? output.clone()
                : new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a✔ Forgeage terminé !"));
            if (recipe != null) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Échec : &c" + recipe.getFailPercent() + "%"));
            }
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oEn cas d'échec, les ingrédients sont perdus."));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eCliquez pour tenter de récupérer l'item."));
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event, Player player, InventoryEngine inventory) {
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
        if (session == null || !session.isReady()) return;

        ForgeManager.Recipe recipe = session.getRecipe();
        if (recipe == null) {
            mgr.clearSession(player);
            ForgeInputButton.refreshForgeButtons(inventory);
            return;
        }

        // Roll the dice on the fail chance. On failure, ingredients are lost.
        int fail = recipe.getFailPercent();
        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1..100 inclusive
        boolean success = roll > fail;

        if (!success) {
            mgr.clearSession(player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c✘ Le forgeage a échoué ! Les ingrédients sont perdus &7(" + roll + "/" + fail + "%)&c."));
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            ForgeInputButton.refreshForgeButtons(inventory);
            return;
        }

        ItemStack output = mgr.buildItem(recipe.getOutputId());
        if (output == null || output.getType() == Material.AIR) {
            // Unresolved output (e.g. missing Nexo id). Refund ingredients so the
            // player isn't stuck nor robbed.
            for (ItemStack ingredient : session.getDeposits().values()) {
                ItemStack refund = ingredient.clone();
                var leftover = player.getInventory().addItem(refund);
                leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
            mgr.clearSession(player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cAucun item forgé n'a pu être résolu. Ingrédients rendus, contactez un administrateur."));
            ForgeInputButton.refreshForgeButtons(inventory);
            return;
        }

        mgr.clearSession(player);
        var leftover = player.getInventory().addItem(output);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));

        // Fire the FORGE job action so the player earns xp/money declared in the job yaml.
        this.plugin.getJobManager().action(player, recipe.getOutputId(), JobActionType.FORGE);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✔ Item forgé récupéré ! &7(" + roll + "/" + fail + "%)"));
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        } catch (Throwable ignored) {}

        ForgeInputButton.refreshForgeButtons(inventory);
    }
}
