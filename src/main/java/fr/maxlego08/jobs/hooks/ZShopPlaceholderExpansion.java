package fr.maxlego08.jobs.hooks;

import fr.maxlego08.jobs.JobsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI expansion (identifier {@code zshop}) that exposes the live
 * amount and price of the current zShop transaction so they can be used in
 * {@code experience-formula} / {@code money-formula} fields of
 * {@code ZSHOP_BUY} / {@code ZSHOP_SELL} job actions.
 *
 * <p>Available placeholders:</p>
 * <ul>
 *   <li>{@code %zshop_amount%} – number of items in the transaction</li>
 *   <li>{@code %zshop_amount_sell%} – alias of {@code amount}, intended for
 *       {@code ZSHOP_SELL} actions</li>
 *   <li>{@code %zshop_amount_buy%} – alias of {@code amount}, intended for
 *       {@code ZSHOP_BUY} actions</li>
 *   <li>{@code %zshop_material_price%} – per-unit price
 *       ({@code total_price / amount})</li>
 *   <li>{@code %zshop_total_price%} / {@code %zshop_price%} – total price of
 *       the transaction</li>
 * </ul>
 *
 * <p>All placeholders return {@code "0"} when no zShop action is currently
 * being dispatched on the calling thread.</p>
 */
public class ZShopPlaceholderExpansion extends PlaceholderExpansion {

    private final JobsPlugin plugin;

    public ZShopPlaceholderExpansion(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zshop";
    }

    @Override
    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().isEmpty()
                ? "Maxlego08"
                : this.plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        ZShopActionContext context = ZShopActionContext.get();
        if (context == null) return "0";

        return switch (params.toLowerCase()) {
            case "amount", "amount_sell", "amount_buy" -> String.valueOf(context.getAmount());
            case "material_price" -> formatDouble(context.getUnitPrice());
            case "total_price", "price" -> formatDouble(context.getTotalPrice());
            default -> null;
        };
    }

    private String formatDouble(double value) {
        // Use Locale-independent formatting; the math expression evaluator
        // expects a dot as decimal separator.
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
