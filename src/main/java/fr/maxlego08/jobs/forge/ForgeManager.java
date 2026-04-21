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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        public Map<Integer, ItemStack> getDeposits() { return deposits; }

        public Recipe getRecipe() { return recipe; }
        public void setRecipe(Recipe recipe) { this.recipe = recipe; }

        public long getFinishTime() { return finishTime; }
        public void setFinishTime(long finishTime) { this.finishTime = finishTime; }

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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "   &7Échec : &c" + session.getRecipe().getFailPercent() + "%"));
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

        this.plugin.getLogger().info("Loaded " + this.recipes.size() + " forge recipes.");
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
     * @return {@code true} if the given item is part of at least one registered
     * forge recipe (i.e. can be deposited in an input slot).
     */
    public boolean isAllowedIngredient(ItemStack itemStack) {
        String id = getItemId(itemStack);
        if (id == null) return false;
        String norm = normalizeId(id);
        for (Recipe recipe : this.recipes.values()) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getId().equals(norm)) return true;
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
     * @return the matched recipe, or {@code null} if no recipe matches the
     * current deposits.
     */
    public Recipe tryStartForging(Player player) {
        Session session = getOrCreateSession(player);
        if (session.hasTimer()) return session.getRecipe();

        Map<String, Integer> deposited = new HashMap<>();
        for (ItemStack stack : session.getDeposits().values()) {
            String id = getItemId(stack);
            if (id == null) continue;
            deposited.merge(normalizeId(id), stack.getAmount(), Integer::sum);
        }
        if (deposited.isEmpty()) return null;

        for (Recipe recipe : this.recipes.values()) {
            if (matches(recipe, deposited)) {
                long seconds = ThreadLocalRandom.current().nextInt(MIN_FORGE_SECONDS, MAX_FORGE_SECONDS + 1);
                session.setRecipe(recipe);
                session.setFinishTime(System.currentTimeMillis() + seconds * 1000L);
                return recipe;
            }
        }
        return null;
    }

    private boolean matches(Recipe recipe, Map<String, Integer> deposited) {
        Map<String, Integer> required = new HashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            required.merge(ingredient.getId(), ingredient.getAmount(), Integer::sum);
        }
        if (deposited.size() != required.size()) return false;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            Integer have = deposited.get(entry.getKey());
            if (have == null || have < entry.getValue()) return false;
        }
        return true;
    }

    public static String formatTime(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        return String.format("%d:%02d", m, s);
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
