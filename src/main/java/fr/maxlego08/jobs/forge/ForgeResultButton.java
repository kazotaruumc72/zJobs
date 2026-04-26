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
                ForgeManager.LuckResult luck = manager().computeLuckResult(session);
                int bonus = session.getBonusPercent();
                if (luck.isSubstituted()) {
                    int eff = Math.min(100, luck.getLuckPercent() + bonus);
                    String tail = bonus > 0 ? " &7(&a+" + bonus + "%&7)" : "";
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Chance : &a" + eff + "%" + tail));
                } else if (session.getRecipe() != null) {
                    int fail = Math.max(0, session.getRecipe().getFailPercent() - bonus);
                    String tail = bonus > 0 ? " &7(&a-" + bonus + "%&7)" : "";
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Échec : &c" + fail + "%" + tail));
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
            ForgeManager.LuckResult luck = manager().computeLuckResult(session);
            int bonus = session.getBonusPercent();
            if (luck.isSubstituted()) {
                int eff = Math.min(100, luck.getLuckPercent() + bonus);
                String tail = bonus > 0 ? " &7(&a+" + bonus + "%&7)" : "";
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7Chance : &a" + eff + "%" + tail));
                if (luck.getDowngradedOutputId() != null) {
                    lore.add(ChatColor.translateAlternateColorCodes('&',
                            "&7&oEn cas d'échec, l'item est dégradé d'un rang."));
                } else {
                    lore.add(ChatColor.translateAlternateColorCodes('&',
                            "&7&oEn cas d'échec, l'item est tout de même livré."));
                }
            } else if (recipe != null) {
                int fail = Math.max(0, recipe.getFailPercent() - bonus);
                String tail = bonus > 0 ? " &7(&a-" + bonus + "%&7)" : "";
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Échec : &c" + fail + "%" + tail));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oEn cas d'échec, les ingrédients sont perdus."));
            }
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

        // Determine which outcome model applies. If the player substituted any
        // ingredient with a different tier, the substitution chance replaces
        // the recipe's fixed `fail` value, and on failure the item is
        // downgraded by one rank instead of losing the ingredients.
        ForgeManager.LuckResult luck = mgr.computeLuckResult(session);
        int bonus = session.getBonusPercent();

        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1..100 inclusive

        if (!luck.isSubstituted()) {
            // Legacy path: fixed fail chance, ingredients lost on failure.
            // The input slot tier bonus reduces the effective fail chance.
            int fail = Math.max(0, recipe.getFailPercent() - bonus);
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

            giveOutput(player, mgr, session, inventory, recipe, recipe.getOutputId(), false, roll, fail);
            return;
        }

        // Substitution path: roll against the computed luck percent, plus the
        // input slot tier bonus (capped at 100%).
        int luckPercent = Math.min(100, luck.getLuckPercent() + bonus);
        boolean success = roll <= luckPercent;
        String outputId;
        boolean downgraded;
        if (success) {
            outputId = recipe.getOutputId();
            downgraded = false;
        } else if (luck.getDowngradedOutputId() != null) {
            outputId = luck.getDowngradedOutputId();
            downgraded = true;
        } else {
            // Output not tierable / already at lowest tier: still deliver the
            // recipe output (no ingredients lost — that's the substitution rule).
            outputId = recipe.getOutputId();
            downgraded = false;
        }
        giveOutput(player, mgr, session, inventory, recipe, outputId, downgraded, roll, luckPercent);
    }

    /**
     * Build and deliver the chosen output to the player, fire the FORGE job
     * action and clear the session. Used by both the legacy fail path and
     * the substitution path.
     */
    private void giveOutput(Player player, ForgeManager mgr, ForgeManager.Session session,
                            InventoryEngine inventory, ForgeManager.Recipe recipe,
                            String outputId, boolean downgraded, int roll, int chancePercent) {
        ItemStack output = mgr.buildItem(outputId);
        if (output == null || output.getType() == Material.AIR) {
            // Unresolved output (e.g. missing Nexo id). Fall back to the original recipe output.
            if (downgraded) {
                output = mgr.buildItem(recipe.getOutputId());
                downgraded = false;
                outputId = recipe.getOutputId();
            }
        }
        if (output == null || output.getType() == Material.AIR) {
            // Still nothing: refund ingredients to avoid trapping the player.
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

        String itemName = getDisplayName(output);
        if (downgraded) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e⚠ &fForgeage partiellement réussi : &e" + itemName + " &7(" + roll + "/" + chancePercent + "%)"));
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            } catch (Throwable ignored) {}
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a✔ &fItem forgé récupéré : &e" + itemName + " &7(" + roll + "/" + chancePercent + "%)"));
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
            } catch (Throwable ignored) {}
        }

        ForgeInputButton.refreshForgeButtons(inventory);
    }

    private static String getDisplayName(ItemStack itemStack) {
        if (itemStack == null) return "?";
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return itemStack.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }
}
