package fr.maxlego08.jobs.rafine;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.hooks.NexoHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralised registry for the RAFINE feature.
 * <p>
 * The feature works as follows :
 * <ol>
 *     <li>Each item the player may deposit has a percentage in its display
 *     name (e.g. {@code &6Amethyste &8(30%)}). That percentage is both the
 *     refining <b>success chance</b> <i>and</i> the tier used to pick the
 *     refined output from {@code result.yml}.</li>
 *     <li>When the player clicks on the item in his inventory, one copy is
 *     removed and deposited in the first free RAFINE input slot. The refine
 *     timer (random duration between {@link #MIN_REFINE_SECONDS} and
 *     {@link #MAX_REFINE_SECONDS}) starts immediately.</li>
 *     <li>When the timer reaches 0, the deposited item visually &quot;moves&quot;
 *     to the result slot. Clicking on it rolls the dice :
 *     <ul>
 *         <li>{@code random(1..100) <= percent} : success. The player
 *         receives the refined item picked from the range that contains the
 *         percentage (see {@link RecipeRange}).</li>
 *         <li>Otherwise : the ore breaks, the player receives a red chat
 *         message and nothing else.</li>
 *     </ul>
 *     </li>
 * </ol>
 */
public class RafineManager {

    /** Minimum refine time in seconds. */
    public static final int MIN_REFINE_SECONDS = 60;
    /** Maximum refine time in seconds. */
    public static final int MAX_REFINE_SECONDS = 180;

    private static final Pattern PERCENT_PATTERN = Pattern.compile("\\((\\d{1,3})%\\)");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(\\d{1,3})%?\\s*-\\s*(\\d{1,3})%?$");

    /**
     * Inclusive range of percentages mapped to a refined output.
     */
    public static final class RecipeRange {
        private final int min;
        private final int max;
        private final String output;

        public RecipeRange(int min, int max, String output) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
            this.output = output;
        }

        public boolean contains(int percent) {
            return percent >= min && percent <= max;
        }

        public int getMin() { return min; }
        public int getMax() { return max; }
        public String getOutput() { return output; }
    }

    /**
     * Deposit state for a single slot. Timer starts immediately.
     */
    public static final class Deposit {
        private final ItemStack itemStack;
        private final int percent;
        private final int bonusPercent;
        private final long finishTime;
        private boolean completionNotified;

        public Deposit(ItemStack itemStack, int percent, int bonusPercent, long finishTime) {
            this.itemStack = itemStack;
            this.percent = percent;
            this.bonusPercent = Math.max(0, bonusPercent);
            this.finishTime = finishTime;
        }

        public ItemStack getItemStack() { return itemStack; }
        public int getPercent() { return percent; }

        /**
         * @return additive bonus (in percent points) granted by the input
         * slot tier on top of {@link #getPercent()}. Always {@code >= 0}.
         */
        public int getBonusPercent() { return bonusPercent; }

        /**
         * @return the success chance of the refining roll, clamped to
         * {@code [0, 100]}. Equal to {@code percent + bonusPercent} bounded.
         */
        public int getEffectivePercent() {
            return Math.max(0, Math.min(100, percent + bonusPercent));
        }

        public long getFinishTime() { return finishTime; }

        public boolean isRefining() { return System.currentTimeMillis() < finishTime; }
        public boolean isReady() { return System.currentTimeMillis() >= finishTime; }

        public boolean isCompletionNotified() { return completionNotified; }
        public void markCompletionNotified() { this.completionNotified = true; }

        public long getRemainingMillis() {
            return Math.max(0L, finishTime - System.currentTimeMillis());
        }

        public long getRemainingSeconds() {
            return (getRemainingMillis() + 999L) / 1000L;
        }
    }

    private final JobsPlugin plugin;

    private final Set<String> allowedInputs = new HashSet<>();
    /** input id -&gt; list of ranges. */
    private final Map<String, List<RecipeRange>> recipes = new LinkedHashMap<>();
    private final Map<UUID, Map<Integer, Deposit>> deposited = new HashMap<>();

    /**
     * Thread-local pointer to the deposit currently driving a RAFINE action
     * (e.g. while {@code RafineResultButton} is collecting a refined item and
     * the {@code JobManager.action(...)} call evaluates experience/money
     * formulas). When set, placeholders such as
     * {@code %zjobs_rafine_percent%} and {@code %zjobs_rafine_percent_inverse%}
     * must reflect this deposit instead of the player's other live deposits,
     * because the deposit is removed from the player's slot before the action
     * is dispatched.
     */
    private static final ThreadLocal<Deposit> ACTION_CONTEXT = new ThreadLocal<>();

    /**
     * @return the deposit currently driving a RAFINE action on this thread,
     * or {@code null} when no such action is being processed.
     */
    public static Deposit getActionContext() {
        return ACTION_CONTEXT.get();
    }

    /**
     * Run {@code runnable} with the given deposit installed as the current
     * RAFINE action context, restoring the previous value (typically
     * {@code null}) when it returns. Used so placeholders evaluated during
     * action processing reflect the deposit that triggered the action.
     */
    public static void withActionContext(Deposit deposit, Runnable runnable) {
        Deposit previous = ACTION_CONTEXT.get();
        ACTION_CONTEXT.set(deposit);
        try {
            runnable.run();
        } finally {
            if (previous == null) {
                ACTION_CONTEXT.remove();
            } else {
                ACTION_CONTEXT.set(previous);
            }
        }
    }

    private BukkitTask completionTask;

    public RafineManager(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start a ticking task that watches every player deposit and fires a nice
     * notification (chat + title + sound) the very tick a refinement turns
     * ready. Safe to call multiple times.
     */
    public void startCompletionWatcher() {
        if (this.completionTask != null) return;
        this.completionTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickCompletion, 20L, 20L);
    }

    public void stopCompletionWatcher() {
        if (this.completionTask != null) {
            this.completionTask.cancel();
            this.completionTask = null;
        }
    }

    private void tickCompletion() {
        for (Map.Entry<UUID, Map<Integer, Deposit>> entry : this.deposited.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            for (Deposit deposit : entry.getValue().values()) {
                if (deposit.isReady() && !deposit.isCompletionNotified()) {
                    deposit.markCompletionNotified();
                    notifyCompletion(player, deposit);
                    forceRefineRedraw(player);
                }
            }
        }
    }

    /**
     * If the player currently has a refinement menu open, force an immediate
     * redraw of the refine-related buttons so the slot visually swaps
     * (input → placeholder, result → deposited item) without waiting for the
     * next {@code updateInterval} tick of the menu.
     */
    private void forceRefineRedraw(Player player) {
        var top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof fr.maxlego08.menu.api.engine.InventoryEngine engine)) return;
        RafineInputButton.refreshRafineButtons(engine);
    }

    private void notifyCompletion(Player player, Deposit deposit) {
        String displayName = getDisplayName(deposit.getItemStack());

        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "   &a&l✔ &eRaffinage terminé &7!"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "   &7Minerai : &f" + displayName));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "   &7Chance : &e" + deposit.getPercent() + "%"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "   &7➥ &fCliquez sur le slot résultat pour récupérer."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------"));
        player.sendMessage("");

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', "&a&l✔ Raffinage terminé"),
                ChatColor.translateAlternateColorCodes('&', "&7Récupérez votre minerai raffiné"),
                10, 40, 10);

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
        } catch (Throwable ignored) {
            // older sound names - silently ignore
        }
    }

    private String getDisplayName(ItemStack itemStack) {
        if (itemStack == null) return "?";
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return itemStack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    // ---------------------------------------------------------------------
    // File handling
    // ---------------------------------------------------------------------

    public void load() {
        this.allowedInputs.clear();
        this.recipes.clear();

        loadRafineFile();
        loadResultFile();

        int ranges = this.recipes.values().stream().mapToInt(List::size).sum();
        this.plugin.getLogger().info("Loaded " + this.allowedInputs.size() + " refine inputs and " + ranges + " refine recipe ranges.");
    }

    private void loadRafineFile() {
        File file = new File(this.plugin.getDataFolder(), "rafine.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            YamlConfiguration def = new YamlConfiguration();
            def.options().setHeader(List.of(
                    "List of items that players are allowed to place in a ZJOBS_ITEM_RAFINE slot.",
                    "Supports vanilla materials (e.g. DIAMOND) and Nexo items (prefix with \"nexo:\")."
            ));
            def.set("items", List.of("nexo:moonstone"));
            try {
                def.save(file);
            } catch (IOException e) {
                this.plugin.getLogger().severe("Could not save default rafine.yml: " + e.getMessage());
            }
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (String raw : configuration.getStringList("items")) {
            if (raw == null || raw.isBlank()) continue;
            this.allowedInputs.add(normalizeId(raw));
        }
    }

    private void loadResultFile() {
        File file = new File(this.plugin.getDataFolder(), "result.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            YamlConfiguration def = new YamlConfiguration();
            def.options().setHeader(List.of(
                    "Refinement recipes used by ZJOBS_ITEM_RAFINE_RESULT.",
                    "",
                    "Each input item can define percentage ranges. The percentage used is the",
                    "one written in the item's display name, e.g. \"Amethyste (30%)\".",
                    "",
                    "Format :",
                    "  result:",
                    "    \"<input-id>\":",
                    "      \"100-70\": \"<output-id>\"   # percent between 70 and 100",
                    "      \"70-40\":  \"<output-id>\"",
                    "      \"40-15\":  \"<output-id>\"",
                    "      \"15-5\":   \"<output-id>\"",
                    "      \"5-1\":    \"<output-id>\"",
                    "",
                    "Supports vanilla materials and Nexo items (prefix with \"nexo:\")."
            ));
            Map<String, String> ranges = new LinkedHashMap<>();
            ranges.put("100-70", "nexo:amethyst");
            ranges.put("70-40", "nexo:amethyst");
            ranges.put("40-15", "nexo:amethyst");
            ranges.put("15-5", "nexo:amethyst");
            ranges.put("5-1", "nexo:amethyst");
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("nexo:moonstone", ranges);
            def.createSection("result", sample);
            try {
                def.save(file);
            } catch (IOException e) {
                this.plugin.getLogger().severe("Could not save default result.yml: " + e.getMessage());
            }
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        // --- New format : result.<input-id>.<range> = <output-id> ---
        ConfigurationSection section = configuration.getConfigurationSection("result");
        if (section != null) {
            for (String inputKey : section.getKeys(false)) {
                String normInput = normalizeId(inputKey);
                ConfigurationSection subSection = section.getConfigurationSection(inputKey);
                List<RecipeRange> list = this.recipes.computeIfAbsent(normInput, k -> new ArrayList<>());

                if (subSection != null) {
                    for (String rangeKey : subSection.getKeys(false)) {
                        String outputRaw = subSection.getString(rangeKey);
                        if (outputRaw == null) continue;
                        int[] bounds = parseRangeKey(rangeKey);
                        if (bounds == null) {
                            this.plugin.getLogger().warning("Invalid refine range '" + rangeKey + "' for input '" + inputKey + "' in result.yml");
                            continue;
                        }
                        list.add(new RecipeRange(bounds[0], bounds[1], normalizeId(outputRaw)));
                    }
                } else {
                    // Legacy inline form :   result: <key>: <value>
                    String outputRaw = section.getString(inputKey);
                    if (outputRaw != null) {
                        list.add(new RecipeRange(1, 100, normalizeId(outputRaw)));
                    }
                }
            }
        }

        // --- Legacy list format : result: list of "a = b" ---
        for (String raw : configuration.getStringList("result")) {
            if (raw == null || raw.isBlank()) continue;
            int eq = raw.indexOf('=');
            if (eq <= 0) continue;
            String input = normalizeId(raw.substring(0, eq).trim());
            String output = normalizeId(raw.substring(eq + 1).trim());
            if (input.isEmpty() || output.isEmpty()) continue;
            this.recipes.computeIfAbsent(input, k -> new ArrayList<>()).add(new RecipeRange(1, 100, output));
        }
    }

    /**
     * Parse a YAML range key such as {@code "100-70"} or {@code "100%-70%"}.
     */
    private int[] parseRangeKey(String key) {
        if (key == null) return null;
        Matcher m = RANGE_PATTERN.matcher(key.replace("%", "").trim());
        if (!m.matches()) return null;
        try {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            int min = Math.max(0, Math.min(a, b));
            int max = Math.min(100, Math.max(a, b));
            return new int[]{min, max};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Item identification
    // ---------------------------------------------------------------------

    private String normalizeId(String id) {
        String trimmed = id.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.toLowerCase(Locale.ROOT).startsWith("nexo:")
                ? trimmed.toLowerCase(Locale.ROOT)
                : trimmed.toUpperCase(Locale.ROOT);
    }

    public String getItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return null;
        NexoHook nexoHook = this.plugin.getNexoHook();
        if (nexoHook != null) {
            String nexoId = nexoHook.getNexoItemId(itemStack);
            if (nexoId != null) return ("nexo:" + nexoId).toLowerCase(Locale.ROOT);
        }
        return itemStack.getType().name();
    }

    public boolean isAllowedInput(ItemStack itemStack) {
        String id = getItemId(itemStack);
        return id != null && this.allowedInputs.contains(normalizeId(id));
    }

    /**
     * Parse the refinement percentage from the item's display name.
     *
     * @return the percentage between 0 and 100, or {@code -1} when the name
     * does not contain a valid percentage.
     */
    public static int parsePercent(ItemStack itemStack) {
        if (itemStack == null) return -1;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return -1;
        String clean = ChatColor.stripColor(meta.getDisplayName());
        Matcher m = PERCENT_PATTERN.matcher(clean);
        if (m.find()) {
            try {
                int p = Integer.parseInt(m.group(1));
                if (p >= 0 && p <= 100) return p;
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    /**
     * Pick the refined output for the given deposit based on its input id and
     * percentage tier.
     *
     * @return the corresponding {@link ItemStack}, or {@code null} when no
     * matching recipe / range exists.
     */
    public ItemStack buildResult(Deposit deposit) {
        if (deposit == null) return null;
        String inputId = getItemId(deposit.getItemStack());
        if (inputId == null) return null;
        List<RecipeRange> ranges = this.recipes.get(normalizeId(inputId));
        if (ranges == null || ranges.isEmpty()) return null;
        for (RecipeRange range : ranges) {
            if (range.contains(deposit.getPercent())) {
                return buildItem(range.getOutput());
            }
        }
        return null;
    }

    public ItemStack buildItem(String id) {
        if (id == null) return null;
        String norm = normalizeId(id);
        if (norm.startsWith("nexo:")) {
            NexoHook hook = this.plugin.getNexoHook();
            if (hook == null) return null;
            return hook.getItemStack(norm.substring(5));
        }
        Material material = Material.matchMaterial(norm);
        return material == null ? null : new ItemStack(material);
    }

    public Set<String> getAllowedInputs() { return Collections.unmodifiableSet(this.allowedInputs); }
    public Map<String, List<RecipeRange>> getRecipes() { return Collections.unmodifiableMap(this.recipes); }

    // ---------------------------------------------------------------------
    // Per-player deposit state
    // ---------------------------------------------------------------------

    public Map<Integer, Deposit> getDeposits(Player player) {
        return this.deposited.computeIfAbsent(player.getUniqueId(), uuid -> new LinkedHashMap<>());
    }

    public Deposit getDeposit(Player player, int slot) {
        Map<Integer, Deposit> map = this.deposited.get(player.getUniqueId());
        return map == null ? null : map.get(slot);
    }

    /**
     * Put a new deposit in the given slot. The refine timer starts immediately.
     *
     * @param maxSeconds   the inclusive upper bound of the random refine
     *                     duration (in seconds). Must be greater than or equal
     *                     to {@link #MIN_REFINE_SECONDS}.
     * @param bonusPercent additive bonus to the success chance (in percent
     *                     points) granted by the input slot tier. Negative
     *                     values are clamped to {@code 0}.
     * @return the random refine duration in seconds
     */
    public long setDeposited(Player player, int slot, ItemStack itemStack, int percent, int maxSeconds, int bonusPercent) {
        int upper = Math.max(MIN_REFINE_SECONDS, maxSeconds);
        long seconds = ThreadLocalRandom.current().nextInt(MIN_REFINE_SECONDS, upper + 1);
        Deposit deposit = new Deposit(itemStack.clone(), percent, bonusPercent, System.currentTimeMillis() + seconds * 1000L);
        getDeposits(player).put(slot, deposit);
        return seconds;
    }

    public void clearSlot(Player player, int slot) {
        Map<Integer, Deposit> map = this.deposited.get(player.getUniqueId());
        if (map != null) {
            map.remove(slot);
            if (map.isEmpty()) this.deposited.remove(player.getUniqueId());
        }
    }

    public void clearAll(Player player) {
        this.deposited.remove(player.getUniqueId());
    }

    public static String formatTime(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        return String.format("%d:%02d", m, s);
    }
}

