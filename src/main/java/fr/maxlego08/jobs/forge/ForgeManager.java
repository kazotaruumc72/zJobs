package fr.maxlego08.jobs.forge;

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
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centralised registry for the FORGE feature.
 * <p>
 * Unlike {@link fr.maxlego08.jobs.rafine.RafineManager} which tiers a single
 * input by its display-name percentage, the forge matches a <b>set of
 * deposited ingredients</b> against a catalog of recipes loaded from
 * {@code items.yml} and produces the associated output with a configurable
 * fail chance.
 * <p>
 * Expected {@code items.yml} format :
 * <pre>
 * items:
 *   nexo:amethyst_sword_commun:
 *     recipe:
 *       - nexo:amethyst_gem_commune:2
 *       - STICK:1
 *     fail: 20
 * </pre>
 */
public class ForgeManager {

    /** Minimum forge time in seconds. */
    public static final int MIN_FORGE_SECONDS = 60;
    /** Maximum forge time in seconds. */
    public static final int MAX_FORGE_SECONDS = 180;

    /** Default fail chance applied when a recipe does not declare one. */
    public static final int DEFAULT_FAIL_PERCENT = 20;

    // ---------------------------------------------------------------------
    // Tier detection (for substitution chance)
    // ---------------------------------------------------------------------

    /** Prefix used by Nexo-backed item ids in the codebase. */
    private static final String NEXO_PREFIX = "nexo:";

    /**
     * Known item tiers, ordered from lowest quality to highest. The order is
     * used for substitution comparisons (e.g. depositing a higher tier than
     * required is considered a perfect match).
     */
    public enum Tier {
        COMMUN, PEU_COMMUNE, RARE, EPIQUE, LEGENDAIRE
    }

    /**
     * Mapping of accepted Nexo id suffixes to a tier. Loaded from
     * {@code forge_tiers.yml} at {@link #load()}; populated with the
     * historical defaults via {@link #applyDefaultTierTables()} so the
     * feature still works when the file is missing or malformed.
     * <p>
     * The list is searched in <i>longest-suffix-first</i> order so that
     * {@code _peu_commune} is matched before {@code _commune}.
     */
    private static final List<String[]> TIER_SUFFIXES = new ArrayList<>();

    /** Subset of {@link #TIER_SUFFIXES} that denote a feminine grammatical form. */
    private static final Set<String> FEMININE_SUFFIXES = new HashSet<>();

    /**
     * Per-tier suffix used to rebuild a downgraded id. Index 0 is the
     * masculine form, index 1 is the feminine form (falling back to index 0
     * when no feminine suffix is declared).
     */
    private static final Map<Tier, String[]> DOWNGRADE_SUFFIXES = new EnumMap<>(Tier.class);

    static {
        applyDefaultTierTables();
    }

    /**
     * Reset {@link #TIER_SUFFIXES}, {@link #FEMININE_SUFFIXES} and
     * {@link #DOWNGRADE_SUFFIXES} to the historical defaults shipped with
     * PR #17. Used both as the initial state (so the manager works without a
     * config file) and as a clean slate before loading {@code forge_tiers.yml}.
     */
    private static void applyDefaultTierTables() {
        TIER_SUFFIXES.clear();
        FEMININE_SUFFIXES.clear();
        DOWNGRADE_SUFFIXES.clear();

        registerTierSuffix(Tier.LEGENDAIRE, "_legendaire", false);
        registerTierSuffix(Tier.EPIQUE, "_epique", false);
        registerTierSuffix(Tier.RARE, "_rare", false);
        registerTierSuffix(Tier.PEU_COMMUNE, "_peu_commune", true);
        registerTierSuffix(Tier.PEU_COMMUNE, "_peu_commun", false);
        registerTierSuffix(Tier.COMMUN, "_commune", true);
        registerTierSuffix(Tier.COMMUN, "_commun", false);
        sortTierSuffixesLongestFirst();
    }

    /**
     * Register a single suffix → tier mapping into the static tables. The
     * first masculine suffix declared for a tier is treated as its
     * canonical downgrade form; the first feminine suffix is the canonical
     * feminine form.
     */
    private static void registerTierSuffix(Tier tier, String suffix, boolean feminine) {
        if (suffix == null || suffix.isEmpty()) return;
        TIER_SUFFIXES.add(new String[]{suffix, tier.name()});
        if (feminine) FEMININE_SUFFIXES.add(suffix);

        String[] downgrade = DOWNGRADE_SUFFIXES.computeIfAbsent(tier, t -> new String[]{null, null});
        int index = feminine ? 1 : 0;
        if (downgrade[index] == null) downgrade[index] = suffix;
    }

    /** Re-sort {@link #TIER_SUFFIXES} so the longest suffix is matched first. */
    private static void sortTierSuffixesLongestFirst() {
        TIER_SUFFIXES.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));
    }

    /**
     * Result of parsing a normalized id into a tier component. Stores the
     * family root (id stripped of its tier suffix), the matched tier and the
     * exact suffix that was matched (so the masculine/feminine gender can be
     * preserved when reconstructing a downgraded id).
     */
    public static final class TieredId {
        private final String family;
        private final Tier tier;
        private final String matchedSuffix;

        TieredId(String family, Tier tier, String matchedSuffix) {
            this.family = family;
            this.tier = tier;
            this.matchedSuffix = matchedSuffix;
        }

        public String getFamily() { return family; }
        public Tier getTier() { return tier; }
        public String getMatchedSuffix() { return matchedSuffix; }

        /** Whether the matched suffix is a feminine form ({@code _commune}, {@code _peu_commune}). */
        public boolean isFeminine() {
            return FEMININE_SUFFIXES.contains(matchedSuffix);
        }
    }

    /**
     * Parse a normalized id into a tiered representation. Vanilla materials
     * and Nexo ids without a recognised tier suffix return {@code null}.
     */
    public static TieredId parseTieredId(String normalizedId) {
        if (normalizedId == null) return null;
        if (!normalizedId.startsWith(NEXO_PREFIX)) return null;
        for (String[] entry : TIER_SUFFIXES) {
            String suffix = entry[0];
            if (normalizedId.endsWith(suffix) && normalizedId.length() > NEXO_PREFIX.length() + suffix.length()) {
                String family = normalizedId.substring(0, normalizedId.length() - suffix.length());
                return new TieredId(family, Tier.valueOf(entry[1]), suffix);
            }
        }
        return null;
    }

    /**
     * Build a downgraded id one tier below the given one, preserving the
     * gender of the original suffix when possible. Returns {@code null} when
     * the id is not tierable or already at the lowest tier.
     */
    public static String downgradeId(String normalizedId) {
        TieredId t = parseTieredId(normalizedId);
        if (t == null || t.tier == Tier.COMMUN) return null;
        Tier lower = Tier.values()[t.tier.ordinal() - 1];

        String[] forms = DOWNGRADE_SUFFIXES.get(lower);
        if (forms == null) return null;

        String masculine = forms[0];
        String feminine = forms[1];
        String suffix = t.isFeminine() && feminine != null ? feminine
                : (masculine != null ? masculine : feminine);
        if (suffix == null) return null;
        return t.getFamily() + suffix;
    }

    /**
     * Outcome of evaluating a session's deposits against its bound recipe to
     * compute the substitution chance. When {@link #isSubstituted()} is
     * {@code false} no tier mismatch was detected and the recipe's regular
     * {@code fail} mechanic should apply unchanged.
     */
    public static final class LuckResult {
        private final boolean substituted;
        private final int luckPercent;
        private final String downgradedOutputId;

        LuckResult(boolean substituted, int luckPercent, String downgradedOutputId) {
            this.substituted = substituted;
            this.luckPercent = Math.max(0, Math.min(100, luckPercent));
            this.downgradedOutputId = downgradedOutputId;
        }

        public boolean isSubstituted() { return substituted; }
        public int getLuckPercent() { return luckPercent; }
        public String getDowngradedOutputId() { return downgradedOutputId; }
    }

    /**
     * Single ingredient of a forge recipe.
     */
    public static final class Ingredient {
        private final String id;
        private final int amount;

        public Ingredient(String id, int amount) {
            this.id = id;
            this.amount = Math.max(1, amount);
        }

        public String getId() { return id; }
        public int getAmount() { return amount; }
    }

    /**
     * A forge recipe: an ordered list of required ingredients, the output id
     * and the fail chance (0..100) applied when the player collects the result.
     */
    public static final class Recipe {
        private final String outputId;
        private final List<Ingredient> ingredients;
        private final int failPercent;

        public Recipe(String outputId, List<Ingredient> ingredients, int failPercent) {
            this.outputId = outputId;
            this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
            this.failPercent = Math.max(0, Math.min(100, failPercent));
        }

        public String getOutputId() { return outputId; }
        public List<Ingredient> getIngredients() { return ingredients; }
        public int getFailPercent() { return failPercent; }
    }

    /**
     * A running forge session bound to a single player. Stores the deposited
     * ingredients (one stack per input slot), the target recipe (once the
     * deposit fully matches one), and the completion time.
     */
    public static final class Session {
        private final Map<Integer, ItemStack> deposits = new LinkedHashMap<>();
        private Recipe recipe;
        private long finishTime;
        private boolean completionNotified;
        private int bonusPercent;

        public Map<Integer, ItemStack> getDeposits() { return deposits; }

        public Recipe getRecipe() { return recipe; }
        public void setRecipe(Recipe recipe) { this.recipe = recipe; }

        public long getFinishTime() { return finishTime; }
        public void setFinishTime(long finishTime) { this.finishTime = finishTime; }

        /**
         * @return additive bonus (in percent points) granted by the input
         * slot tier on top of the computed substitution-luck percentage. Set
         * by {@link #tryStartForging(Player, int, int)} when the forge starts
         * and read back by {@link #computeLuckResult(Session)}.
         */
        public int getBonusPercent() { return bonusPercent; }
        public void setBonusPercent(int bonusPercent) { this.bonusPercent = Math.max(0, bonusPercent); }

        public boolean hasTimer() { return this.finishTime > 0L; }
        public boolean isForging() { return hasTimer() && System.currentTimeMillis() < finishTime; }
        public boolean isReady() { return hasTimer() && System.currentTimeMillis() >= finishTime; }

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

    /** output-id -&gt; recipe. */
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();
    /**
     * Explicit whitelist of ingredient ids (normalised) that the player is
     * allowed to deposit in a {@code ZJOBS_ITEM_FORGE} slot. When
     * {@link #whitelistEnabled} is {@code false} the whitelist is ignored and
     * any ingredient used by a registered recipe is allowed.
     */
    private final Set<String> whitelist = new HashSet<>();
    private boolean whitelistEnabled = false;
    private final Map<UUID, Session> sessions = new HashMap<>();

    private BukkitTask completionTask;

    public ForgeManager(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------------
    // Completion watcher
    // ---------------------------------------------------------------------

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
        for (Map.Entry<UUID, Session> entry : this.sessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            Session session = entry.getValue();
            if (session.isReady() && !session.isCompletionNotified()) {
                session.markCompletionNotified();
                notifyCompletion(player, session);
                forceForgeRedraw(player);
            }
        }
    }

    private void forceForgeRedraw(Player player) {
        var top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof fr.maxlego08.menu.api.engine.InventoryEngine engine)) return;
        ForgeInputButton.refreshForgeButtons(engine);
    }

    private void notifyCompletion(Player player, Session session) {
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "   &a&l✔ &eForgeage terminé &7!"));
        if (session.getRecipe() != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "   &7Recette : &f" + session.getRecipe().getOutputId()));
            ForgeManager.LuckResult luck = computeLuckResult(session);
            int bonus = session.getBonusPercent();
            if (luck.isSubstituted()) {
                int eff = Math.min(100, luck.getLuckPercent() + bonus);
                String tail = bonus > 0 ? " &7(&a+" + bonus + "%&7)" : "";
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "   &7Chance : &a" + eff + "%" + tail));
            } else {
                int fail = Math.max(0, session.getRecipe().getFailPercent() - bonus);
                String tail = bonus > 0 ? " &7(&a-" + bonus + "%&7)" : "";
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "   &7Échec : &c" + fail + "%" + tail));
            }
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "   &7➥ &fCliquez sur le slot résultat pour récupérer."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------"));
        player.sendMessage("");

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', "&a&l✔ Forgeage terminé"),
                ChatColor.translateAlternateColorCodes('&', "&7Récupérez votre item forgé"),
                10, 40, 10);

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------------
    // File handling
    // ---------------------------------------------------------------------

    public void load() {
        this.recipes.clear();
        this.whitelist.clear();
        this.whitelistEnabled = false;

        loadTiersFile();

        File file = new File(this.plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            writeDefaultItemsFile(file);
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection("items");
        if (section != null) {
            for (String outputKey : section.getKeys(false)) {
                ConfigurationSection recipeSection = section.getConfigurationSection(outputKey);
                List<String> recipeList;
                int failPercent = DEFAULT_FAIL_PERCENT;

                if (recipeSection != null) {
                    recipeList = recipeSection.getStringList("recipe");
                    failPercent = recipeSection.getInt("fail", DEFAULT_FAIL_PERCENT);
                } else {
                    recipeList = section.getStringList(outputKey + ".recipe");
                    failPercent = section.getInt(outputKey + ".fail", DEFAULT_FAIL_PERCENT);
                }

                if (recipeList == null || recipeList.isEmpty()) {
                    this.plugin.getLogger().warning("FORGE recipe '" + outputKey + "' has no ingredients in items.yml; skipping.");
                    continue;
                }

                List<Ingredient> ingredients = new ArrayList<>();
                for (String raw : recipeList) {
                    Ingredient ingredient = parseIngredient(raw);
                    if (ingredient != null) ingredients.add(ingredient);
                    else this.plugin.getLogger().warning("Invalid FORGE ingredient '" + raw + "' for '" + outputKey + "'.");
                }

                if (ingredients.isEmpty()) continue;

                String outputId = normalizeId(outputKey);
                this.recipes.put(outputId, new Recipe(outputId, ingredients, failPercent));
            }
        }

        loadWhitelistFile();

        this.plugin.getLogger().info("Loaded " + this.recipes.size() + " forge recipes"
                + (this.whitelistEnabled ? " and " + this.whitelist.size() + " whitelisted ingredients." : "."));
    }

    /**
     * Load {@code forge_items_whitelist.yml}. When the file is missing a
     * default one is written, pre-populated with the ingredients used by the
     * sample recipe so the feature works out of the box.
     * <p>
     * The whitelist is considered enabled as soon as the {@code whitelist}
     * key is present (even when the list is empty); this lets server owners
     * explicitly disable forging by writing {@code whitelist: []}. When the
     * {@code whitelist} key is entirely absent (legacy behaviour / deleted
     * file) the listener falls back to the recipe-derived allowance.
     */
    private void loadWhitelistFile() {
        File whitelistFile = new File(this.plugin.getDataFolder(), "forge_items_whitelist.yml");
        if (!whitelistFile.exists()) {
            writeDefaultWhitelistFile(whitelistFile);
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(whitelistFile);
        if (!configuration.contains("whitelist")) return;

        this.whitelistEnabled = true;
        for (String raw : configuration.getStringList("whitelist")) {
            if (raw == null || raw.isBlank()) continue;
            this.whitelist.add(normalizeId(raw));
        }
    }

    private void writeDefaultWhitelistFile(File file) {
        file.getParentFile().mkdirs();
        YamlConfiguration def = new YamlConfiguration();
        def.options().setHeader(List.of(
                "Ingredients the player is allowed to deposit in a ZJOBS_ITEM_FORGE slot.",
                "",
                "Supports vanilla materials (e.g. STICK) and Nexo items (prefix with \"nexo:\").",
                "Removing the `whitelist` key entirely disables this gate and falls back to",
                "allowing every ingredient used by at least one recipe in items.yml."
        ));
        def.set("whitelist", List.of("nexo:amethyst_gem_commune", "STICK"));
        try {
            def.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save default forge_items_whitelist.yml: " + e.getMessage());
        }
    }

    /**
     * Load {@code forge_tiers.yml}, which classifies forge ingredients into
     * rarity tiers via id-suffix matching. When the file is missing the
     * bundled default is copied from the plugin jar; when the file exists
     * but is malformed, the historical defaults shipped with PR #17 are
     * used so the feature keeps working.
     */
    private void loadTiersFile() {
        File tiersFile = new File(this.plugin.getDataFolder(), "forge_tiers.yml");
        if (!tiersFile.exists()) {
            tiersFile.getParentFile().mkdirs();
            try {
                this.plugin.saveResource("forge_tiers.yml", false);
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().severe("Bundled forge_tiers.yml is missing from the jar: " + e.getMessage());
                applyDefaultTierTables();
                return;
            }
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tiersFile);
        ConfigurationSection tiersSection = configuration.getConfigurationSection("tiers");
        if (tiersSection == null) {
            this.plugin.getLogger().warning("forge_tiers.yml is missing the 'tiers' section; falling back to defaults.");
            applyDefaultTierTables();
            return;
        }

        TIER_SUFFIXES.clear();
        FEMININE_SUFFIXES.clear();
        DOWNGRADE_SUFFIXES.clear();

        int loaded = 0;
        for (String tierKey : tiersSection.getKeys(false)) {
            Tier tier;
            try {
                tier = Tier.valueOf(tierKey.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().warning("Unknown forge tier '" + tierKey + "' in forge_tiers.yml; skipping.");
                continue;
            }

            List<Map<?, ?>> entries = tiersSection.getMapList(tierKey + ".suffixes");
            if (entries.isEmpty()) {
                this.plugin.getLogger().warning("Forge tier '" + tierKey + "' has no suffixes in forge_tiers.yml; skipping.");
                continue;
            }
            for (Map<?, ?> entry : entries) {
                Object rawValue = entry.get("value");
                if (rawValue == null) continue;
                String suffix = String.valueOf(rawValue);
                if (suffix.isEmpty()) continue;
                Object rawFeminine = entry.get("feminine");
                boolean feminine = rawFeminine instanceof Boolean ? (Boolean) rawFeminine
                        : Boolean.parseBoolean(String.valueOf(rawFeminine));
                registerTierSuffix(tier, suffix, feminine);
                loaded++;
            }
        }

        if (loaded == 0) {
            this.plugin.getLogger().warning("forge_tiers.yml declared no usable suffix; falling back to defaults.");
            applyDefaultTierTables();
            return;
        }

        sortTierSuffixesLongestFirst();
    }

    private void writeDefaultItemsFile(File file) {
        file.getParentFile().mkdirs();
        YamlConfiguration def = new YamlConfiguration();
        def.options().setHeader(List.of(
                "Forge recipes used by ZJOBS_ITEM_FORGE / ZJOBS_ITEM_FORGE_RESULT buttons.",
                "",
                "Format :",
                "  items:",
                "    \"<output-id>\":",
                "      recipe:",
                "        - \"<input-id>:<amount>\"",
                "        - \"<input-id>:<amount>\"",
                "      fail: <0..100>   # percent chance the forge fails on result click",
                "",
                "Supports vanilla materials and Nexo items (prefix with \"nexo:\")."
        ));
        Map<String, Object> swordRecipe = new LinkedHashMap<>();
        swordRecipe.put("recipe", List.of("nexo:amethyst_gem_commune:2", "STICK:1"));
        swordRecipe.put("fail", 20);
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("nexo:amethyst_sword_commun", swordRecipe);
        def.createSection("items", items);
        try {
            def.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save default items.yml: " + e.getMessage());
        }
    }

    /**
     * Parse an ingredient descriptor such as {@code "nexo:amethyst_gem_commune:2"}
     * or {@code "STICK:1"}. The amount suffix is optional (defaults to 1).
     *
     * @return the parsed ingredient, or {@code null} if {@code raw} is malformed.
     */
    private Ingredient parseIngredient(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        int colonIndex = trimmed.lastIndexOf(':');
        String idPart = trimmed;
        int amount = 1;

        // Only treat the trailing ":<number>" as the amount (avoids breaking
        // "nexo:xxx" ids which also contain a colon).
        if (colonIndex > 0) {
            String maybeAmount = trimmed.substring(colonIndex + 1).trim();
            try {
                amount = Integer.parseInt(maybeAmount);
                idPart = trimmed.substring(0, colonIndex).trim();
            } catch (NumberFormatException ignored) {
                // No trailing amount; keep full string as id, default amount of 1.
            }
        }

        if (idPart.isEmpty()) return null;
        return new Ingredient(normalizeId(idPart), amount);
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

    /**
     * Whether the given item may be deposited in a {@code ZJOBS_ITEM_FORGE} slot.
     * <p>
     * When {@code forge_items_whitelist.yml} defines a {@code whitelist} key,
     * that explicit list is the sole authority. Otherwise the method falls
     * back to allowing every ingredient used by at least one registered recipe.
     */
    public boolean isAllowedIngredient(ItemStack itemStack) {
        String id = getItemId(itemStack);
        if (id == null) return false;
        String norm = normalizeId(id);
        TieredId depTier = parseTieredId(norm);
        if (this.whitelistEnabled) {
            if (this.whitelist.contains(norm)) return true;
            // Same-family substitution: a deposit of any tier in the family of
            // a whitelisted ingredient is accepted (substitution up or down).
            if (depTier != null) {
                for (String allowed : this.whitelist) {
                    TieredId allowedTier = parseTieredId(allowed);
                    if (allowedTier != null && allowedTier.getFamily().equals(depTier.getFamily())) return true;
                }
            }
            return false;
        }
        for (Recipe recipe : this.recipes.values()) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getId().equals(norm)) return true;
                if (depTier != null) {
                    TieredId reqTier = parseTieredId(ingredient.getId());
                    if (reqTier != null && reqTier.getFamily().equals(depTier.getFamily())) return true;
                }
            }
        }
        return false;
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

    public Map<String, Recipe> getRecipes() { return Collections.unmodifiableMap(this.recipes); }

    // ---------------------------------------------------------------------
    // Per-player session state
    // ---------------------------------------------------------------------

    public Session getOrCreateSession(Player player) {
        return this.sessions.computeIfAbsent(player.getUniqueId(), uuid -> new Session());
    }

    public Session getSession(Player player) {
        return this.sessions.get(player.getUniqueId());
    }

    public void clearSession(Player player) {
        this.sessions.remove(player.getUniqueId());
    }

    /**
     * Add one unit of the given ingredient to the session in the given input slot.
     * If no existing deposit is in that slot the slot is initialised with a
     * single-item copy; otherwise the stack amount is incremented (clamped to
     * the stack's max size).
     *
     * @return {@code true} if the ingredient was deposited, {@code false} if
     * the slot was already used by a <i>different</i> item.
     */
    public boolean deposit(Player player, int slot, ItemStack ingredient) {
        Session session = getOrCreateSession(player);
        if (session.hasTimer()) return false; // don't allow deposits while forging

        ItemStack existing = session.getDeposits().get(slot);
        if (existing == null) {
            ItemStack single = ingredient.clone();
            single.setAmount(1);
            session.getDeposits().put(slot, single);
            return true;
        }
        if (!isSameIngredient(existing, ingredient)) return false;
        existing.setAmount(Math.min(existing.getMaxStackSize(), existing.getAmount() + 1));
        return true;
    }

    private boolean isSameIngredient(ItemStack a, ItemStack b) {
        String idA = getItemId(a);
        String idB = getItemId(b);
        if (idA == null || idB == null) return false;
        return normalizeId(idA).equals(normalizeId(idB));
    }

    /**
     * Try to find a registered recipe fully satisfied by the current session
     * deposits. If found, the recipe is recorded on the session and the
     * timer starts immediately.
     *
     * @param maxSeconds   the inclusive upper bound of the random forge
     *                     duration (in seconds). Bounded by
     *                     {@link #MIN_FORGE_SECONDS} below.
     * @param bonusPercent additive bonus to the success chance of producing
     *                     the recipe's highest-rarity output (in percent
     *                     points). Stored on the session and read back by
     *                     {@link #computeLuckResult(Session)}. Negative
     *                     values are clamped to {@code 0}.
     * @return the matched recipe, or {@code null} if no recipe matches the
     * current deposits.
     */
    public Recipe tryStartForging(Player player, int maxSeconds, int bonusPercent) {
        Session session = getOrCreateSession(player);
        if (session.hasTimer()) return session.getRecipe();

        // Group deposits by recipe key (family for tiered ids, exact id otherwise).
        Map<String, Integer> depositedByKey = new HashMap<>();
        for (ItemStack stack : session.getDeposits().values()) {
            String id = getItemId(stack);
            if (id == null) continue;
            String norm = normalizeId(id);
            TieredId tiered = parseTieredId(norm);
            String key = tiered == null ? norm : tiered.getFamily();
            depositedByKey.merge(key, stack.getAmount(), Integer::sum);
        }
        if (depositedByKey.isEmpty()) return null;

        int upper = Math.max(MIN_FORGE_SECONDS, maxSeconds);
        for (Recipe recipe : this.recipes.values()) {
            if (matches(recipe, depositedByKey)) {
                long seconds = ThreadLocalRandom.current().nextInt(MIN_FORGE_SECONDS, upper + 1);
                session.setRecipe(recipe);
                session.setFinishTime(System.currentTimeMillis() + seconds * 1000L);
                session.setBonusPercent(bonusPercent);
                return recipe;
            }
        }
        return null;
    }

    /**
     * Match a recipe against a deposit map keyed by family (tiered ids) or
     * exact id (non-tiered). For each required key the deposited total must
     * be at least the required total, and no extra unrelated keys may have
     * been deposited.
     */
    private boolean matches(Recipe recipe, Map<String, Integer> depositedByKey) {
        Map<String, Integer> required = new HashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            TieredId tiered = parseTieredId(ingredient.getId());
            String key = tiered == null ? ingredient.getId() : tiered.getFamily();
            required.merge(key, ingredient.getAmount(), Integer::sum);
        }
        if (depositedByKey.size() != required.size()) return false;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            Integer have = depositedByKey.get(entry.getKey());
            if (have == null || have < entry.getValue()) return false;
        }
        return true;
    }

    /**
     * Compute the substitution-luck result of the given session against its
     * bound recipe. The returned object indicates whether any tier mismatch
     * was detected; when none is, callers should keep using the recipe's
     * regular {@code fail} mechanic. When a substitution was detected, the
     * computed {@code luckPercent} is the chance (0..100) that the produced
     * item is the recipe output; on the complementary chance the produced
     * item should be the {@code downgradedOutputId} (which falls back to the
     * original output id when the output is not tierable or already at the
     * lowest tier).
     *
     * <p>Per family, the chance is {@code matching / total} where
     * {@code matching} is the count of deposited units whose tier is greater
     * than or equal to the required tier, and {@code total} is the recipe's
     * required quantity for that family. Multiple tierable families are
     * combined multiplicatively.
     */
    public LuckResult computeLuckResult(Session session) {
        if (session == null) return new LuckResult(false, 100, null);
        Recipe recipe = session.getRecipe();
        if (recipe == null) return new LuckResult(false, 100, null);

        // Tally deposits per family with per-tier counts.
        Map<String, Map<Tier, Integer>> depositedByFamily = new HashMap<>();
        for (ItemStack stack : session.getDeposits().values()) {
            String id = getItemId(stack);
            if (id == null) continue;
            String norm = normalizeId(id);
            TieredId tiered = parseTieredId(norm);
            if (tiered == null) continue;
            depositedByFamily
                    .computeIfAbsent(tiered.getFamily(), k -> new HashMap<>())
                    .merge(tiered.getTier(), stack.getAmount(), Integer::sum);
        }

        // Aggregate recipe requirements per family.
        Map<String, int[]> requiredByFamily = new LinkedHashMap<>(); // family -> [requiredAmount, requiredTierOrdinal]
        for (Ingredient ingredient : recipe.getIngredients()) {
            TieredId tiered = parseTieredId(ingredient.getId());
            if (tiered == null) continue;
            int[] cur = requiredByFamily.get(tiered.getFamily());
            if (cur == null) {
                requiredByFamily.put(tiered.getFamily(), new int[]{ingredient.getAmount(), tiered.getTier().ordinal()});
            } else {
                cur[0] += ingredient.getAmount();
                // If the recipe lists multiple tiers in the same family, use the highest as required.
                cur[1] = Math.max(cur[1], tiered.getTier().ordinal());
            }
        }

        boolean substituted = false;
        double chance = 1.0;
        for (Map.Entry<String, int[]> entry : requiredByFamily.entrySet()) {
            String family = entry.getKey();
            int requiredAmount = entry.getValue()[0];
            int requiredTierOrdinal = entry.getValue()[1];
            Map<Tier, Integer> deposits = depositedByFamily.getOrDefault(family, Collections.emptyMap());

            int matching = 0;
            for (Map.Entry<Tier, Integer> dep : deposits.entrySet()) {
                if (dep.getKey().ordinal() != requiredTierOrdinal) substituted = true;
                if (dep.getKey().ordinal() >= requiredTierOrdinal) matching += dep.getValue();
            }
            // Cap matching to required to avoid >100% from over-deposit.
            int effectiveMatching = Math.min(matching, requiredAmount);
            chance *= (double) effectiveMatching / (double) requiredAmount;
        }

        int luckPercent = (int) Math.round(chance * 100.0);
        String downgraded = downgradeId(normalizeId(recipe.getOutputId()));
        return new LuckResult(substituted, luckPercent, downgraded);
    }

    public static String formatTime(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        return String.format("%d:%02d", m, s);
    }

    /**
     * Aggregate the highest forge tier across the input buttons of an open
     * inventory. Used by {@link ForgeInputButton#onClick} and
     * {@link ForgeValidateButton#onClick} to feed the same
     * {@code (maxSeconds, bonusPercent)} pair into
     * {@link #tryStartForging(Player, int, int)}.
     *
     * @param engine the open inventory engine, may be {@code null}
     * @return the {@code int[2]} pair {@code {maxSeconds, bonusPercent}}
     * defaulting to {@code {MAX_FORGE_SECONDS, 0}} when no input button is
     * present.
     */
    public static int[] aggregateInputTier(fr.maxlego08.menu.api.engine.InventoryEngine engine) {
        int maxSeconds = MAX_FORGE_SECONDS;
        int bonusPercent = 0;
        if (engine != null) {
            for (fr.maxlego08.menu.api.button.Button btn : engine.getButtons()) {
                if (btn instanceof ForgeInputButton input) {
                    if (input.getMaxSeconds() > maxSeconds) maxSeconds = input.getMaxSeconds();
                    if (input.getBonusPercent() > bonusPercent) bonusPercent = input.getBonusPercent();
                }
            }
        }
        return new int[]{maxSeconds, bonusPercent};
    }

    /**
     * Resolve the {@link fr.maxlego08.jobs.api.enums.JobActionType} that should
     * be fired when a forge completes inside the given inventory.
     * <p>
     * The forge tier is inferred from the highest-numbered
     * {@link ForgeInputButton} subclass present in the engine
     * ({@link ForgeInputButton1}..{@link ForgeInputButton4} map to
     * {@link fr.maxlego08.jobs.api.enums.JobActionType#FORGE_1}..{@code FORGE_4};
     * the base {@link ForgeInputButton} maps to
     * {@link fr.maxlego08.jobs.api.enums.JobActionType#FORGE}). When the engine
     * is {@code null} or contains no forge input button, {@code FORGE} is
     * returned so legacy menus keep firing the base action type.
     *
     * @param engine the open inventory engine, may be {@code null}
     * @return the forge action type matching the inventory's tier
     */
    public static fr.maxlego08.jobs.api.enums.JobActionType aggregateInputForgeType(fr.maxlego08.menu.api.engine.InventoryEngine engine) {
        fr.maxlego08.jobs.api.enums.JobActionType type = fr.maxlego08.jobs.api.enums.JobActionType.FORGE;
        int bestTier = 0;
        if (engine != null) {
            for (fr.maxlego08.menu.api.button.Button btn : engine.getButtons()) {
                int tier;
                // Order matters: ForgeInputButton1..4 all extend ForgeInputButton,
                // so we must check the most specific subclass first to recover the
                // tier number — otherwise every tier would match the base class.
                if (btn instanceof ForgeInputButton4) tier = 4;
                else if (btn instanceof ForgeInputButton3) tier = 3;
                else if (btn instanceof ForgeInputButton2) tier = 2;
                else if (btn instanceof ForgeInputButton1) tier = 1;
                else if (btn instanceof ForgeInputButton) tier = 0;
                else continue;
                if (tier > bestTier) {
                    bestTier = tier;
                    type = switch (tier) {
                        case 1 -> fr.maxlego08.jobs.api.enums.JobActionType.FORGE_1;
                        case 2 -> fr.maxlego08.jobs.api.enums.JobActionType.FORGE_2;
                        case 3 -> fr.maxlego08.jobs.api.enums.JobActionType.FORGE_3;
                        case 4 -> fr.maxlego08.jobs.api.enums.JobActionType.FORGE_4;
                        default -> fr.maxlego08.jobs.api.enums.JobActionType.FORGE;
                    };
                }
            }
        }
        return type;
    }

    /**
     * Human-readable status string used by the {@code %zjobs_forge_status%} placeholder.
     */
    public String getStatus(Player player) {
        Session session = getSession(player);
        if (session == null || session.getDeposits().isEmpty()) return "En attente";
        if (session.isReady()) return "Terminé";
        if (session.isForging()) return "Forgeage";
        return "Prêt à forger";
    }
}
