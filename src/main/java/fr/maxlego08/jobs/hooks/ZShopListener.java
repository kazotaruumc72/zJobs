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
 *     experience-formula: "%zshop_amount_buy% * %zshop_material_price%"
 *     money-formula: "%zshop_amount_buy% * %zshop_material_price%"
 * </pre>
 *
 * The target dispatched to {@link JobManager#action} is either the Bukkit
 * {@link org.bukkit.Material} of the bought/sold item, or, when the item is a
 * Nexo custom item, its {@code "nexo:<id>"} string id. Existing
 * {@link fr.maxlego08.jobs.actions.MaterialAction} / {@link fr.maxlego08.jobs.actions.NexoAction}
 * matching logic in the loader then handles both forms transparently.
 *
 * <p>While dispatching, this listener binds a {@link ZShopActionContext} to
 * the current thread so the {@code %zshop_amount_*%}, {@code %zshop_material_price%}
 * and {@code %zshop_total_price%} placeholders (registered by
 * {@link ZShopPlaceholderExpansion}) resolve to the live amount / price of
 * the current transaction.</p>
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
        dispatchPerItem(event.getPlayer(), event.getItemButton(), JobActionType.ZSHOP_BUY,
                event.getAmount(), event.getPrice());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZShopSell(ZShopSellEvent event) {
        dispatchPerItem(event.getPlayer(), event.getItemButton(), JobActionType.ZSHOP_SELL,
                event.getAmount(), event.getPrice());
    }

    /**
     * Forwards each item sold through zShop's "sell all" feature (triggered
     * by a middle-click in the zShop menu) to the {@link JobManager}. zShop
     * fires a single {@link ZShopSellAllEvent} carrying every
     * {@link ShopAction} performed during the bulk sell. Each {@code ShopAction}
     * represents a stack of items of the same type, with its
     * {@link ItemStack#getAmount() amount} being the number of items sold from
     * that stack. Each individual item is dispatched as its own action via
     * {@link #dispatchPerItem(Player, ItemButton, JobActionType, int, double)}.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZShopSellAll(ZShopSellAllEvent event) {
        Player player = event.getPlayer();
        for (ShopAction shopAction : event.getShopActions()) {
            int totalAmount = shopAction.getItemStack().getAmount();
            dispatchPerItem(player, shopAction.getItemButton(), JobActionType.ZSHOP_SELL,
                    totalAmount, shopAction.getPrice());
        }
    }

    /**
     * Dispatches one {@link JobActionType} action <b>per individual item</b>
     * involved in the given zShop transaction.
     *
     * <p>zShop buy / sell events carry a stack-level amount {@code N} and a
     * total price {@code P} (the price for the whole stack). To make sure
     * every individual item bought or sold by the player is counted as its
     * own action — and not a single action for the whole stack — we forward
     * the action {@code N} times with {@code amount = 1} and
     * {@code unitPrice = P / N}. This keeps formula evaluation
     * (e.g. {@code %zshop_material_price% * %zshop_amount_buy%}) consistent
     * regardless of how many items were grouped in the transaction (single
     * click, shift-click, middle-click sell-all, etc.).</p>
     */
    private void dispatchPerItem(Player player, ItemButton itemButton, JobActionType actionType,
                                 int totalAmount, double totalPrice) {
        if (itemButton == null || totalAmount <= 0) return;

        double unitPrice = totalPrice / totalAmount;
        for (int i = 0; i < totalAmount; i++) {
            dispatch(player, itemButton, actionType, 1, unitPrice);
        }
    }

    /**
     * Resolve the target (Material or {@code "nexo:<id>"}) from the shop button
     * and forward the action to the {@link JobManager}. Resolution prefers the
     * Nexo item id when available so jobs can match Nexo items declared with
     * {@code material: "nexo:<id>"}; otherwise the Bukkit material name is used.
     *
     * <p>The {@code amount} and {@code totalPrice} of the transaction are bound
     * to a thread-local {@link ZShopActionContext} for the duration of the
     * dispatch so that {@code %zshop_amount_sell%}, {@code %zshop_amount_buy%},
     * {@code %zshop_material_price%} and {@code %zshop_total_price%} resolve to
     * the exact values of the transaction during formula evaluation.</p>
     */
    private void dispatch(Player player, ItemButton itemButton, JobActionType actionType,
                          int amount, double totalPrice) {
        if (itemButton == null) return;

        ItemStack itemStack;
        try {
            itemStack = itemButton.getCustomItemStack(player, false, new fr.maxlego08.menu.api.utils.Placeholders());
        } catch (Throwable throwable) {
            // Defensive: never let a broken item rendering kill the buy / sell flow.
            this.plugin.getLogger().warning("Unable to resolve item for zShop " + actionType + " action: " + throwable.getMessage());
            return;
        }
        if (itemStack == null) return;

        ZShopActionContext.set(amount, totalPrice);
        try {
            if (this.plugin.getNexoHook() != null) {
                String nexoId = this.plugin.getNexoHook().getNexoItemId(itemStack);
                if (nexoId != null) {
                    this.jobManager.action(player, "nexo:" + nexoId, actionType);
                    return;
                }
            }

            this.jobManager.action(player, itemStack.getType(), actionType);
        } finally {
            ZShopActionContext.clear();
        }
    }
}
