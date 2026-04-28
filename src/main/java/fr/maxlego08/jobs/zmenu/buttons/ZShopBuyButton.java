package fr.maxlego08.jobs.zmenu.buttons;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.shop.api.ShopManager;
import fr.maxlego08.shop.api.buttons.ItemButton;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * zMenu button that opens the zShop "buy" inventory for a given material.
 * <p>
 * zShop is a soft dependency: this button is only registered when the zShop
 * plugin is present, and the click is silently ignored if zShop or the
 * configured item cannot be resolved at runtime.
 */
public class ZShopBuyButton extends Button {

    private final JobsPlugin plugin;
    private final String material;
    private final String inventoryName;

    public ZShopBuyButton(JobsPlugin plugin, String material, String inventoryName) {
        this.plugin = plugin;
        this.material = material;
        this.inventoryName = inventoryName;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        super.onClick(player, event, inventory, slot, placeholders);

        if (Bukkit.getPluginManager().getPlugin("zShop") == null) {
            this.plugin.getLogger().warning("ZSHOP_BUY clicked but zShop is not installed.");
            return;
        }

        RegisteredServiceProvider<ShopManager> provider = Bukkit.getServicesManager().getRegistration(ShopManager.class);
        if (provider == null) {
            this.plugin.getLogger().warning("ZSHOP_BUY clicked but zShop ShopManager service is not registered.");
            return;
        }

        ShopManager shopManager = provider.getProvider();
        Optional<ItemButton> optional = resolveItemButton(shopManager);
        if (optional.isEmpty()) {
            this.plugin.getLogger().warning("ZSHOP_BUY: no zShop item found for material '" + this.material + "'.");
            return;
        }

        ItemButton itemButton = optional.get();
        if (!itemButton.canBuy()) {
            this.plugin.getLogger().warning("ZSHOP_BUY: item '" + this.material + "' is not buyable in zShop.");
            return;
        }

        shopManager.openBuy(player, itemButton, this.inventoryName);
    }

    private Optional<ItemButton> resolveItemButton(ShopManager shopManager) {
        if (this.material == null || this.material.isEmpty()) return Optional.empty();
        Optional<ItemButton> byString = shopManager.getItemButton(this.material);
        if (byString.isPresent()) return byString;
        Material parsed = Material.matchMaterial(this.material);
        if (parsed != null) return shopManager.getItemButton(parsed);
        return Optional.empty();
    }
}
