package fr.maxlego08.jobs.rafine;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Watches clicks in the player's own inventory while a refinement menu is
 * opened. When the player clicks on a whitelisted item, it is moved to the
 * first free {@code ZJOBS_ITEM_RAFINE} slot and the refining timer starts.
 * Clicks on non-whitelisted items are refused with a red chat message.
 */
public class RafineClickListener implements Listener {

    private final JobsPlugin plugin;

    public RafineClickListener(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        var topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof InventoryEngine engine)) return;

        // Collect all RAFINE input slots in this menu, mapping each slot to
        // its bound input button so we know its max-duration tier later on.
        Map<Integer, RafineInputButton> inputButtons = new LinkedHashMap<>();
        boolean isRafineMenu = false;
        for (Button button : engine.getButtons()) {
            if (button instanceof RafineInputButton input) {
                inputButtons.put(input.getSlot(), input);
                isRafineMenu = true;
            } else if (button instanceof RafineResultButton || button instanceof RafineTimerButton) {
                isRafineMenu = true;
            }
        }
        if (!isRafineMenu) return; // not a refinement menu
        if (inputButtons.isEmpty()) return; // nowhere to deposit

        // Only handle clicks that originate from the player's own inventory
        int raw = event.getRawSlot();
        int topSize = topInv.getSize();
        if (raw < 0 || raw < topSize) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        event.setCancelled(true);

        RafineManager manager = this.plugin.getRafineManager();

        if (!manager.isAllowedInput(current)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cCet item ne fait pas partie de la liste des minerais à raffiner."));
            return;
        }

        // Extract the (XX%) from the item's display name
        int percent = RafineManager.parsePercent(current);
        if (percent < 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cCet item n'a pas de pourcentage de raffinage dans son nom (ex: &7'(30%)'&c)."));
            return;
        }

        // Find the first free RAFINE input slot
        int free = -1;
        RafineInputButton freeButton = null;
        for (Map.Entry<Integer, RafineInputButton> entry : inputButtons.entrySet()) {
            int s = entry.getKey();
            if (manager.getDeposit(player, s) == null) {
                free = s;
                freeButton = entry.getValue();
                break;
            }
        }
        if (free < 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cLe slot de raffinage est déjà occupé."));
            return;
        }

        // Transfer exactly one item from the clicked stack to the refine slot
        ItemStack deposited = current.clone();
        deposited.setAmount(1);
        long seconds = manager.setDeposited(player, free, deposited, percent, freeButton.getMaxSeconds(), freeButton.getBonusPercent());

        if (current.getAmount() > 1) {
            current.setAmount(current.getAmount() - 1);
            event.setCurrentItem(current);
        } else {
            event.setCurrentItem(null);
        }

        int bonus = freeButton.getBonusPercent();
        int effectivePercent = Math.max(0, Math.min(100, percent + bonus));
        String chanceText = bonus > 0
                ? "&e" + effectivePercent + "% &7(&e" + percent + "&7+&a" + bonus + "&7)"
                : "&e" + percent + "%";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✚ &eRaffinage démarré &7(&f" + RafineManager.formatTime(seconds) + "&7, " + chanceText + "&7)."));
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.6f, 1.0f);
        } catch (Throwable ignored) {}

        RafineInputButton.refreshRafineButtons(engine);
    }
}

