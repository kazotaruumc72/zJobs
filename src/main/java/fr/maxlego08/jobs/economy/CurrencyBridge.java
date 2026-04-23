package fr.maxlego08.jobs.economy;

import fr.maxlego08.menu.hooks.currencies.CurrencyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Small compatibility bridge around {@link CurrencyProvider#deposit}.
 * <p>
 * zMenu shades {@code fr.traqueur.currencies.CurrencyProvider} into
 * {@code fr.maxlego08.menu.hooks.currencies.CurrencyProvider}. Across zMenu
 * releases the {@code deposit(...)} signature changed:
 * <ul>
 *     <li>zMenu {@code 1.1.0.4} (CurrenciesAPI 1.0.10) — {@code deposit(OfflinePlayer, BigDecimal, String)}</li>
 *     <li>zMenu {@code 1.1.1.3+} (CurrenciesAPI main)   — {@code deposit(UUID, BigDecimal, String)}</li>
 * </ul>
 * Because the deposit call runs from the async storage task, a signature
 * mismatch used to surface as a silent {@code NoSuchMethodError}, leaving
 * {@code updateMoney} stuck at 0 (no money paid out for any job action,
 * including RAFINE/FORGE).
 * <p>
 * This bridge resolves the correct method once via reflection and invokes it
 * directly thereafter, so zJobs stays compatible with both old and new zMenu
 * builds regardless of the compile-time API coordinate.
 */
public final class CurrencyBridge {

    private CurrencyBridge() {
    }

    /**
     * Cached {@code deposit} {@link Method} reference together with the flag
     * that tells whether its first parameter is a {@link UUID} (newer zMenu)
     * or an {@link OfflinePlayer} (legacy zMenu).
     */
    private record Deposit(Method method, boolean takesUuid) {
    }

    private static final AtomicReference<Deposit> CACHE = new AtomicReference<>();

    /**
     * Deposit the given amount to the player identified by {@code playerId}.
     *
     * @param provider the currency provider (may not be {@code null})
     * @param playerId the target player's unique id
     * @param amount   the amount to deposit
     * @param reason   deposit reason (passed to the provider)
     * @throws ReflectiveOperationException if no supported {@code deposit}
     *                                      overload can be found or the
     *                                      invocation fails
     */
    public static void deposit(CurrencyProvider provider, UUID playerId, BigDecimal amount, String reason)
            throws ReflectiveOperationException {
        Deposit deposit = CACHE.get();
        if (deposit == null) {
            deposit = resolve(provider.getClass());
            CACHE.set(deposit);
        }
        Object target = deposit.takesUuid() ? playerId : Bukkit.getOfflinePlayer(playerId);
        try {
            deposit.method().invoke(provider, target, amount, reason);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw e;
        }
    }

    private static Deposit resolve(Class<?> providerClass) throws NoSuchMethodException {
        // Prefer the newer UUID-based overload so we keep working on the
        // latest zMenu releases; fall back to OfflinePlayer for 1.1.0.4.
        for (Method method : providerClass.getMethods()) {
            if (!"deposit".equals(method.getName())) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 3) continue;
            if (params[1] != BigDecimal.class || params[2] != String.class) continue;
            if (params[0] == UUID.class) {
                return new Deposit(method, true);
            }
        }
        for (Method method : providerClass.getMethods()) {
            if (!"deposit".equals(method.getName())) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 3) continue;
            if (params[1] != BigDecimal.class || params[2] != String.class) continue;
            if (OfflinePlayer.class.isAssignableFrom(params[0])) {
                return new Deposit(method, false);
            }
        }
        throw new NoSuchMethodException("No compatible CurrencyProvider.deposit(UUID|OfflinePlayer, BigDecimal, String) on " + providerClass.getName());
    }
}
