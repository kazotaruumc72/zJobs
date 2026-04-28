package fr.maxlego08.jobs.hooks;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.shop.api.buttons.ItemButton;
import fr.maxlego08.shop.api.event.ShopAction;
import fr.maxlego08.shop.api.event.events.ZShopBuyEvent;
import fr.maxlego08.shop.api.event.events.ZShopSellAllEvent;
import fr.maxlego08.shop.api.event.events.ZShopSellEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Bridges zShop buy / sell events to the {@link JobManager#action} pipeline,
 * so that jobs can declare:
 *
 * <pre>
 * actions:
 *   - type: ZSHOP_BUY
 *     material: STICK            # or "nexo:item_id"
 *     experience: "%zshop_amount_buy% * %zshop_material_price%"
 *     money: "%zshop_amount_buy% * %zshop_material_price%"
 * </pre>
 *
 * The target dispatched to {@link JobManager#action} is either the Bukkit
 * {@link org.bukkit.Material} of the bought/sold item, or, when the item is a
 * Nexo custom item, its {@code "nexo:<id>"} string id. Existing
 * {@link fr.maxlego08.jobs.actions.MaterialAction} / {@link fr.maxlego08.jobs.actions.NexoAction}
 * matching logic in the loader then handles both forms transparently.
 */
public class ZShopListener implements Listener {

    private final JobsPlugin plugin;
    private final JobManager jobManager;

    public ZShopListener(JobsPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZShopBuy(ZShopBuyEvent event) {
        dispatch(event.getPlayer(), event.getItemButton(), JobActionType.ZSHOP_BUY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZShopSell(ZShopSellEvent event) {
        dispatch(event.getPlayer(), event.getItemButton(), JobActionType.ZSHOP_SELL);
    }

    /**
     * Forwards each item sold through zShop's "sell all" feature to the
     * {@link JobManager}. zShop fires a single {@link ZShopSellAllEvent}
     * carrying every {@link ShopAction} performed during the bulk sell, so we
     * iterate and dispatch a {@link JobActionType#ZSHOP_SELL} action per item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZShopSellAll(ZShopSellAllEvent event) {
        Player player = event.getPlayer();
        for (ShopAction shopAction : event.getShopActions()) {
            dispatch(player, shopAction.getItemButton(), JobActionType.ZSHOP_SELL);
        }
    }

    /**
     * Resolve the target (Material or {@code "nexo:<id>"}) from the shop button
     * and forward the action to the {@link JobManager}. Resolution prefers the
     * Nexo item id when available so jobs can match Nexo items declared with
     * {@code material: "nexo:<id>"}; otherwise the Bukkit material name is used.
     */
    private void dispatch(Player player, ItemButton itemButton, JobActionType actionType) {
        if (itemButton == null) return;

        ItemStack itemStack;
        try {
            itemStack = itemButton.getCustomItemStack(player);
        } catch (Throwable throwable) {
            // Defensive: never let a broken item rendering kill the buy / sell flow.
            this.plugin.getLogger().warning("Unable to resolve item for zShop " + actionType + " action: " + throwable.getMessage());
            return;
        }
        if (itemStack == null) return;

        if (this.plugin.getNexoHook() != null) {
            String nexoId = this.plugin.getNexoHook().getNexoItemId(itemStack);
            if (nexoId != null) {
                this.jobManager.action(player, "nexo:" + nexoId, actionType);
                return;
            }
        }

        this.jobManager.action(player, itemStack.getType(), actionType);
    }
}
