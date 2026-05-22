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
 * Input slot button (yaml type {@code ZJOBS_ITEM_FORGE}).
 * <p>
 * Uses the RecipeBook-style rendering approach: {@link #hasSpecialRender()}
 * returns {@code true} and {@link #onRender(Player, InventoryEngine)} writes
 * the current-state item directly into the Bukkit inventory via
 * {@link InventoryEngine#addItem(int, ItemStack)}.
 * <p>
 * States :
 * <ul>
 *     <li><b>Empty</b> : renders the yaml-defined placeholder (light gray glass).</li>
 *     <li><b>Filled</b> : renders the deposited ingredient stack.</li>
 *     <li><b>Filled + timer running</b> : same as above with a lore showing the remaining time.</li>
 * </ul>
 * Clicking on a deposited ingredient while the timer is not running :
 * <ul>
 *     <li>First tries to {@link ForgeManager#tryStartForging(Player, int, int)}: if the current deposits
 *     match a known recipe, the forge timer starts immediately.</li>
 *     <li>Otherwise, the deposited ingredient is refunded to the player.</li>
 * </ul>
 * While the timer runs, the slot is read-only.
 */
public class ForgeInputButton extends Button {

    private final JobsPlugin plugin;

    public ForgeInputButton(Plugin plugin) {
        this.plugin = (JobsPlugin) plugin;
        setUseCache(false);
        setUpdated(true);
    }

    /**
     * Maximum forge duration (in seconds) contributed by this slot when the
     * forging starts. The actual session timer uses the maximum value across
     * every {@link ForgeInputButton} present in the open menu, so a menu can
     * mix tiers and the longest one wins. Subclasses (e.g.
     * {@link ForgeInputButton1}) override this to provide longer ranges. The
     * minimum is always {@link ForgeManager#MIN_FORGE_SECONDS}.
     */
    public int getMaxSeconds() {
        return ForgeManager.MAX_FORGE_SECONDS;
    }

    /**
     * Bonus (in percent points) that this slot contributes to the chance of
     * producing the recipe's highest-rarity output. The session uses the
     * maximum bonus across every {@link ForgeInputButton} present in the open
     * menu. Subclasses (e.g. {@link ForgeInputButton1}) override this to
     * provide values between 1 and 4.
     */
    public int getBonusPercent() {
        return 0;
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
        if (session == null) return getCustomItemStack(player, false, new Placeholders());

        ItemStack deposited = session.getDeposits().get(getSlot());
        if (deposited == null) return getCustomItemStack(player, false, new Placeholders());

        ItemStack itemStack = deposited.clone();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (session.isForging()) {
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&e⌛ Forgeage en cours..."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Temps restant : &f" + ForgeManager.formatTime(session.getRemainingSeconds())));
        } else if (!session.isReady()) {
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Cliquez pour démarrer le forgeage"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ou pour récupérer l'ingrédient."));
        }
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
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
        if (session == null) return;

        if (session.isForging() || session.isReady()) return; // locked while forging

        ItemStack deposited = session.getDeposits().get(slot);
        if (deposited == null) return;

        // Attempt to start forging first. If the current deposit matches a recipe,
        // the timer begins and the slot becomes read-only. The forging duration
        // and rarity bonus are taken from the highest tier among input buttons
        // currently present in the open menu.
        int[] tier = ForgeManager.aggregateInputTier(inventory);
        ForgeManager.Recipe recipe = mgr.tryStartForging(player, tier[0], tier[1]);
        if (recipe != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a✚ &eForgeage démarré &7(" + ForgeManager.formatTime(session.getRemainingSeconds()) + "&7)."));
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.6f, 1.0f);
            } catch (Throwable ignored) {}
            refreshForgeButtons(inventory);
            return;
        }

        // No recipe matches yet: refund the clicked ingredient stack and free the slot.
        ItemStack refund = deposited.clone();
        var leftover = player.getInventory().addItem(refund);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        session.getDeposits().remove(slot);
        if (session.getDeposits().isEmpty()) mgr.clearSession(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&eIngrédient retiré du slot."));
        refreshForgeButtons(inventory);
    }

    /**
     * Redraw every forge-related button of the given engine so the slots
     * visually swap without waiting for the next {@code updateInterval} tick.
     */
    static void refreshForgeButtons(InventoryEngine engine) {
        for (Button button : engine.getButtons()) {
            if (button instanceof ForgeInputButton
                    || button instanceof ForgeResultButton
                    || button instanceof ForgeTimerButton
                    || button instanceof ForgeValidateButton) {
                engine.displayButton(button);
            }
        }
    }
}
