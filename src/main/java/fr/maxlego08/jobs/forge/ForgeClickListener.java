package fr.maxlego08.jobs.forge;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Watches clicks in the player's own inventory while a forge menu is opened.
 * <p>
 * When the player clicks on an item that is part of any registered recipe,
 * exactly one unit is moved to the first free {@code ZJOBS_ITEM_FORGE} slot
 * (or stacked on an existing slot if the ingredient is identical). Clicks on
 * non-whitelisted items are refused with a red chat message.
 */
public class ForgeClickListener implements Listener {

    private final JobsPlugin plugin;

    public ForgeClickListener(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        var topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof InventoryEngine engine)) return;

        // Collect every FORGE input slot in this menu (and flag that this is a forge menu).
        List<Integer> inputSlots = new ArrayList<>();
        boolean isForgeMenu = false;
        for (Button button : engine.getButtons()) {
            if (button instanceof ForgeInputButton) {
                inputSlots.add(button.getSlot());
                isForgeMenu = true;
            } else if (button instanceof ForgeResultButton || button instanceof ForgeTimerButton) {
                isForgeMenu = true;
            }
        }
        if (!isForgeMenu) return;
        if (inputSlots.isEmpty()) return;

        // Only handle clicks that originate from the player's own inventory
        int raw = event.getRawSlot();
        int topSize = topInv.getSize();
        if (raw < 0 || raw < topSize) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        event.setCancelled(true);

        ForgeManager manager = this.plugin.getForgeManager();

        ForgeManager.Session session = manager.getSession(player);
        if (session != null && session.hasTimer()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cUn forgeage est déjà en cours."));
            return;
        }

        if (!manager.isAllowedIngredient(current)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cCet item n'est utilisé dans aucune recette de forge."));
            return;
        }

        // Try to add to an existing matching slot first (stack same ingredient);
        // otherwise pick the first free slot.
        int targetSlot = -1;
        ForgeManager.Session existing = manager.getOrCreateSession(player);
        for (int s : inputSlots) {
            ItemStack depositedStack = existing.getDeposits().get(s);
            if (depositedStack != null) {
                String depositedId = manager.getItemId(depositedStack);
                String currentId = manager.getItemId(current);
                if (depositedId != null && depositedId.equalsIgnoreCase(currentId)
                        && depositedStack.getAmount() < depositedStack.getMaxStackSize()) {
                    targetSlot = s;
                    break;
                }
            }
        }
        if (targetSlot < 0) {
            for (int s : inputSlots) {
                if (existing.getDeposits().get(s) == null) {
                    targetSlot = s;
                    break;
                }
            }
        }

        if (targetSlot < 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cTous les slots de dépôt sont occupés."));
            return;
        }

        boolean deposited = manager.deposit(player, targetSlot, current);
        if (!deposited) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cImpossible de déposer cet item."));
            return;
        }

        if (current.getAmount() > 1) {
            current.setAmount(current.getAmount() - 1);
            event.setCurrentItem(current);
        } else {
            event.setCurrentItem(null);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✚ &eIngrédient déposé&7. &fCliquez sur le slot pour démarrer le forgeage."));
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.0f);
        } catch (Throwable ignored) {}

        ForgeInputButton.refreshForgeButtons(engine);
    }
}
