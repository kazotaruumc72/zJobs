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
import java.util.concurrent.ThreadLocalRandom;

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
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Chance de réussite : " + RafineManager.formatChance(deposit)));
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
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Chance de réussite : " + RafineManager.formatChance(deposit)));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oSi le raffinage échoue, le minerai sera perdu."));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eCliquez pour tenter de récupérer l'item."));
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    @Override
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event, Player player, InventoryEngine inventory) {
        // zMenu only wires Button#onClick via ItemButton#setClick inside
        // displayFinalButton, which is skipped for hasSpecialRender()
        // buttons. For those buttons, zMenu still dispatches
        // onInventoryClick to every button on every click, so we handle
        // the click here and delegate to onClick when it targets our slot.
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getRawSlot() != getSlot()) return;
        onClick(player, event, inventory, getSlot(), new Placeholders());
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

        // Roll the dice : the percentage written on the ore is its chance of
        // success, plus the input slot's tier bonus (capped at 100%). On
        // failure, the ore is consumed (already removed from the input slot
        // when the deposit was created) and the player gets a red chat
        // message + a breaking sound. On success, the refined output is built
        // and given to the player.
        int effectivePercent = ready.getEffectivePercent();
        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1..100 inclusive
        boolean success = effectivePercent > 0 && roll <= effectivePercent;

        if (!success) {
            manager().clearSlot(player, readySlot);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c✘ Le raffinage a échoué ! Le minerai s'est brisé &7(" + roll + "/" + effectivePercent + "%)&c."));
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } catch (Throwable ignored) {
                // older sound names - silently ignore
            }
            RafineInputButton.refreshRafineButtons(inventory);
            return;
        }

        ItemStack output = manager().buildResult(ready);
        if (output == null || output.getType() == Material.AIR) {
            // No recipe / output could be resolved for this percentage (e.g. the
            // configured Nexo item id does not exist anymore). Still free the
            // slot and refund the raw deposited ore so the player isn't stuck
            // nor robbed of his item.
            manager().clearSlot(player, readySlot);
            ItemStack refund = ready.getItemStack().clone();
            var refundLeftover = player.getInventory().addItem(refund);
            refundLeftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cAucune recette configurée pour ce pourcentage. Minerai rendu, contactez un administrateur."));
            RafineInputButton.refreshRafineButtons(inventory);
            return;
        }

        manager().clearSlot(player, readySlot);

        var leftover = player.getInventory().addItem(output);
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));

        String outputId = manager().getItemId(output);
        if (outputId != null) {
            // Dispatch the RAFINE action with the just-completed deposit as the
            // current context, so placeholders like %zjobs_rafine_percent% and
            // %zjobs_rafine_percent_inverse% used in experience/money formulas
            // reflect THIS deposit and not the player's remaining deposits
            // (the slot above has just been cleared).
            RafineManager.Deposit context = ready;
            RafineManager.withActionContext(context, () ->
                    this.plugin.getJobManager().action(player, outputId, JobActionType.RAFINE));
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✔ &fItem raffiné récupéré : &e" + getDisplayName(output) + " &7(" + roll + "/" + effectivePercent + "%)"));
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        } catch (Throwable ignored) {
            // older sound names - silently ignore
        }

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

    private static String getDisplayName(ItemStack itemStack) {
        if (itemStack == null) return "?";
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return itemStack.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }
}
