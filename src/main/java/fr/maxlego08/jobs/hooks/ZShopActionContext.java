package fr.maxlego08.jobs.hooks;

/**
 * Per-thread context holding the live amount and price information of a zShop
 * transaction while a {@code ZSHOP_BUY} / {@code ZSHOP_SELL} job action is
 * being processed.
 *
 * <p>{@link ZShopListener} populates this context immediately before
 * dispatching the action to the {@link fr.maxlego08.jobs.api.JobManager} and
 * clears it in a {@code finally} block. The {@link ZShopPlaceholderExpansion}
 * reads from it to resolve the {@code %zshop_amount_sell%},
 * {@code %zshop_amount_buy%} and {@code %zshop_material_price%} placeholders
 * used in {@code experience-formula} / {@code money-formula} fields.</p>
 *
 * <p>The context is stored in a {@link ThreadLocal} because the action
 * dispatch — including formula resolution — happens synchronously on the
 * thread firing the zShop event.</p>
 */
public final class ZShopActionContext {

    private static final ThreadLocal<ZShopActionContext> CONTEXT = new ThreadLocal<>();

    private final int amount;
    private final double unitPrice;
    private final double totalPrice;

    private ZShopActionContext(int amount, double unitPrice, double totalPrice) {
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    /**
     * Bind a new context to the current thread.
     *
     * @param amount     number of items involved in the transaction
     * @param totalPrice total price paid / received for the transaction
     */
    public static void set(int amount, double totalPrice) {
        double unitPrice = amount > 0 ? totalPrice / amount : totalPrice;
        CONTEXT.set(new ZShopActionContext(amount, unitPrice, totalPrice));
    }

    /**
     * Remove any context previously bound to the current thread.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * @return the context bound to the current thread, or {@code null} when no
     * zShop action is currently being dispatched.
     */
    public static ZShopActionContext get() {
        return CONTEXT.get();
    }

    public int getAmount() {
        return this.amount;
    }

    public double getUnitPrice() {
        return this.unitPrice;
    }

    public double getTotalPrice() {
        return this.totalPrice;
    }
}
