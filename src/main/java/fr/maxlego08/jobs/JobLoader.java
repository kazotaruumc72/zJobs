package fr.maxlego08.jobs;

import fr.maxlego08.jobs.actions.BrewAction;
import fr.maxlego08.jobs.actions.CustomAction;
import fr.maxlego08.jobs.actions.EnchantmentAction;
import fr.maxlego08.jobs.actions.EntityAction;
import fr.maxlego08.jobs.actions.ForgeAction;
import fr.maxlego08.jobs.actions.MaterialAction;
import fr.maxlego08.jobs.actions.NexoAction;
import fr.maxlego08.jobs.actions.OrestackAction;
import fr.maxlego08.jobs.actions.RafineAction;
import fr.maxlego08.jobs.actions.TagAction;
import fr.maxlego08.jobs.actions.ZJobAction;
import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.JobReward;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.utils.ValueInformation;
import fr.maxlego08.jobs.zcore.utils.EntityTypeToEggConverter;
import fr.maxlego08.jobs.zcore.utils.TagRegistry;
import fr.maxlego08.jobs.zcore.utils.loader.Loader;
import fr.maxlego08.menu.api.enchantment.Enchantments;
import fr.maxlego08.menu.api.enchantment.MenuEnchantment;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JobLoader implements Loader<Job> {

    private final JobsPlugin plugin;
    private final File file;

    public JobLoader(JobsPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    @Override
    public Job load(YamlConfiguration configuration, String path) {

        double baseExperience = configuration.getDouble("base-experience", 100);
        int maxLevels = configuration.getInt("max-levels", 100);
        int maxPrestiges = configuration.getInt("max-prestiges", 100);
        boolean canJoin = configuration.getBoolean("can-join", true);
        boolean canLeave = configuration.getBoolean("can-leave", true);
        int customModelData = configuration.getInt("custom-model-data", 0);
        String name = configuration.getString("name");
        String formula = configuration.getString("formula", "baseExperience * (1 + 0.05 * level + 0.005 * level^2) * (1 + 0.3 * (prestige / maxPrestiges))");
        List<JobAction<?>> jobActions = loadActions(configuration);

        List<JobReward> jobRewards = new ArrayList<>();
        configuration.getMapList("rewards").forEach(map -> {
            TypedMapAccessor accessor = new TypedMapAccessor((Map<String, Object>) map);
            int level = accessor.getInt("level");
            int prestige = accessor.getInt("prestige");
            List<Action> actions = plugin.getButtonManager().loadActions(map.containsKey("actions") ? (List<Map<String, Object>>) map.get("actions") : new ArrayList<>(), path, file);
            jobRewards.add(new ZJobReward(level, prestige, actions));
        });

        List<ValueInformation> valueInformations = loadValueInformations(configuration);

        return new ZJob(name, file.getName().replace(".yml", ""), baseExperience, maxLevels, maxPrestiges, formula, jobActions, jobRewards, canJoin, canLeave, customModelData, valueInformations);
    }

    private List<ValueInformation> loadValueInformations(YamlConfiguration configuration) {
        List<ValueInformation> valueInformations = new ArrayList<>();
        for (Map<?, ?> map : configuration.getMapList("additional-value-informations")) {
            var accessor = new TypedMapAccessor((Map<String, Object>) map);
            var material = accessor.getString("material");
            var name = accessor.getString("name");
            var experience = accessor.getDouble("experience");
            var money = accessor.getDouble("money");
            valueInformations.add(new ValueInformation(material, name, experience, money, null));
        }
        return valueInformations;
    }

    private List<JobAction<?>> loadActions(YamlConfiguration configuration) {
        List<JobAction<?>> jobActions = new ArrayList<>();

        List<Map<?, ?>> actionMaps = configuration.getMapList("actions");
        for (int actionIndex = 0; actionIndex < actionMaps.size(); actionIndex++) {
            Map<?, ?> map = actionMaps.get(actionIndex);
            int actionNumber = actionIndex + 1;
            TypedMapAccessor accessor = new TypedMapAccessor((Map<String, Object>) map);
            double experience = accessor.getDouble("experience", 0);
            double money = accessor.getDouble("money", 0);

            // Read raw values for PlaceholderAPI placeholder support (e.g. "%math_1+1%")
            // and arithmetic expression support (e.g. "%zjobs_rafine_percent_inverse% * 5").
            // Two equivalent ways are supported:
            //   - explicit dedicated key: experience-formula / money-formula (preferred, documented)
            //   - placeholder/expression put directly inside experience / money as a string
            // A string value is treated as a formula whenever it can't be parsed as a
            // plain number (i.e. it contains a placeholder or any operator/space).
            Object rawExperienceFormula = map.get("experience-formula");
            Object rawMoneyFormula = map.get("money-formula");
            Object rawExperience = map.get("experience");
            Object rawMoney = map.get("money");
            String experienceFormula = rawExperienceFormula instanceof String ef && isFormulaString(ef) ? ef
                    : (rawExperience instanceof String s && isFormulaString(s) ? s : null);
            String moneyFormula = rawMoneyFormula instanceof String mf && isFormulaString(mf) ? mf
                    : (rawMoney instanceof String s && isFormulaString(s) ? s : null);

            try {

                String typeName = accessor.getString("type");
                if (typeName == null) {
                    plugin.getLogger().severe("Missing 'type' for action #" + actionNumber + " in file " + file.getAbsolutePath());
                    continue;
                }
                JobActionType jobActionType;
                try {
                    jobActionType = JobActionType.valueOf(typeName.toUpperCase());
                } catch (IllegalArgumentException unknownType) {
                    plugin.getLogger().severe("Unknown action type '" + typeName + "' for action #" + actionNumber
                            + " in file " + file.getAbsolutePath()
                            + "; skipping this action. Valid types: " + Arrays.toString(JobActionType.values()));
                    continue;
                }
                String displayMaterialName = accessor.getString("display-material", null);
                String displayMaterial = displayMaterialName == null ? null :
                        (isCustomDisplayMaterial(displayMaterialName) ? displayMaterialName.toLowerCase() : displayMaterialName.toUpperCase());
                String displayName = accessor.getString("display-name", "Name not found");
                JobAction<?> jobAction = null;

                if (jobActionType.isMaterial()) {

                    if (accessor.contains("material")) {
                        String materialName = accessor.getString("material");
                        if (materialName.toLowerCase().startsWith("nexo:")) {
                            String nexoId = "nexo:" + materialName.substring(5);
                            jobAction = new NexoAction(nexoId, experience, money, jobActionType, displayMaterial == null ? "PAPER" : displayMaterial);
                        } else if (materialName.toLowerCase().startsWith("orestack:")) {
                            String orestackId = ("orestack:" + materialName.substring(9)).toLowerCase();
                            jobAction = new OrestackAction(orestackId, experience, money, jobActionType, displayMaterial == null ? "PAPER" : displayMaterial);
                        } else {
                            Material material = Material.valueOf(materialName.toUpperCase());
                            jobAction = new MaterialAction(material, experience, money, jobActionType, displayMaterial == null ? material.name() : displayMaterial);
                        }
                    } else if (accessor.contains("tag")) {
                        Tag<Material> tag = TagRegistry.getTag(accessor.getString("tag").toUpperCase());
                        jobAction = new TagAction(tag, experience, money, jobActionType, displayMaterial == null ? "PAPER" : displayMaterial);
                    } else {
                        this.plugin.getLogger().severe("Impossible to find the tag or material for " + jobActionType + " in file " + file.getAbsolutePath());
                    }

                } else if (jobActionType.isEntityType()) {
                    String entityName = accessor.getString("entity");
                    if (entityName.toLowerCase().startsWith("mm:")) {
                        // MythicMobs entity
                        String mythicMobId = "mm:" + entityName.substring(3);
                        jobAction = new CustomAction(mythicMobId, experience, money, displayMaterial == null ? "PAPER" : displayMaterial, jobActionType);
                    } else {
                        // Vanilla entity
                        EntityType entityType = EntityType.valueOf(entityName.toUpperCase());
                        jobAction = new EntityAction(entityType, experience, money, jobActionType, displayMaterial == null ? EntityTypeToEggConverter.getSpawnEgg(entityType).name() : displayMaterial);
                    }

                } else if (jobActionType == JobActionType.ENCHANT) {

                    jobAction = loadEnchantAction(accessor, experience, money, displayMaterial);

                } else if (jobActionType == JobActionType.BREW) {

                    jobAction = loadBrewAction(accessor, experience, money, displayMaterial);

                } else if (jobActionType == JobActionType.RAFINE) {

                    jobAction = loadRafineAction(accessor, experience, money, displayMaterial);

                } else if (jobActionType == JobActionType.FORGE
                        || jobActionType == JobActionType.FORGE_1
                        || jobActionType == JobActionType.FORGE_2
                        || jobActionType == JobActionType.FORGE_3
                        || jobActionType == JobActionType.FORGE_4) {

                    jobAction = loadForgeAction(accessor, experience, money, displayMaterial, jobActionType);

                } else if (jobActionType == JobActionType.CUSTOM) {

                    String data = accessor.getString("data", null);
                    if (data == null) {
                        plugin.getLogger().severe("Custom data was not found for " + jobActionType + " in file " + file.getAbsolutePath());
                    } else jobAction = new CustomAction(data, experience, money, displayMaterial);
                }

                if (jobAction != null) {
                    ((ZJobAction<?>) jobAction).setDisplayName(displayName);
                    if (experienceFormula != null) ((ZJobAction<?>) jobAction).setExperienceFormula(experienceFormula);
                    if (moneyFormula != null) ((ZJobAction<?>) jobAction).setMoneyFormula(moneyFormula);
                    jobActions.add(jobAction);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return jobActions;
    }

    @Override
    public void save(Job object, YamlConfiguration configuration, String path) {

    }

    /**
     * Checks if a string contains a valid PlaceholderAPI placeholder pattern.
     * A valid placeholder requires at least two '%' characters (e.g. "%math_1+1%").
     *
     * @param value the string to check
     * @return true if the string contains a placeholder pattern
     */
    private boolean isPlaceholder(String value) {
        int firstIndex = value.indexOf('%');
        return firstIndex >= 0 && value.indexOf('%', firstIndex + 1) > firstIndex;
    }

    /**
     * Returns {@code true} when the given string should be treated as a
     * dynamic formula (placeholder substitution and/or arithmetic expression)
     * rather than a plain numeric literal. A value qualifies as a formula as
     * soon as it cannot be parsed as a plain {@code double}, which covers:
     * <ul>
     *     <li>strings containing a PlaceholderAPI pattern (e.g. {@code %xxx%}),</li>
     *     <li>strings containing arithmetic operators or spaces
     *     (e.g. {@code "100 - 25"} or {@code "%zjobs_rafine_percent_inverse% * 5"}).</li>
     * </ul>
     */
    /**
     * Returns {@code true} when the given display-material is a custom
     * identifier (e.g. {@code nexo:xxx} or {@code orestack:xxx}) rather than a
     * vanilla {@link Material} name. Custom identifiers keep their original
     * case so the matching hook can resolve them as-is.
     */
    private boolean isCustomDisplayMaterial(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase();
        return lower.startsWith("nexo:") || lower.startsWith("orestack:");
    }

    private boolean isFormulaString(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return false;
        try {
            Double.parseDouble(trimmed);
            return false;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }


    /**
     * Load an enchantment job action from configuration.
     *
     * @param accessor        accessor for the configuration
     * @param experience      the experience given when the job action is done
     * @param money           the money given when the job action is done
     * @param displayMaterial the material to display in the GUI
     * @return the job action
     */
    private JobAction<?> loadEnchantAction(TypedMapAccessor accessor, double experience, double money, String displayMaterial) {
        Enchantments enchantments = plugin.getInventoryManager().getEnchantments();
        String enchantmentName = accessor.getString("enchantment");
        String materialName = accessor.getString("material", null);

        Material material = materialName == null ? null : Material.valueOf(materialName.toUpperCase());
        Enchantment enchantment = enchantmentName == null ? null : enchantments.getEnchantments(enchantmentName).map(MenuEnchantment::enchantment).orElse(null);
        int minimumLevel = accessor.getInt("minimum-level", 0);
        int minimumCost = accessor.getInt("minimum-cost", 0);

        return new EnchantmentAction(material, experience, money, enchantment, minimumLevel, minimumCost, displayMaterial);
    }

    /**
     * Load a {@link JobAction} of type {@link JobActionType#BREW} from the given configuration accessor.
     *
     * @param accessor        the configuration accessor
     * @param experience      the base experience given when brewing the potion
     * @param money           the base money given when brewing the potion
     * @param displayMaterial the display material of the item, if not set, the material of the potion is used
     * @return a new {@link JobAction} of type {@link JobActionType#BREW}
     */
    private JobAction<?> loadBrewAction(TypedMapAccessor accessor, double experience, double money, String displayMaterial) {
        String potionName = accessor.getString("potion-type", null);
        String potionMaterialName = accessor.getString("potion-material", "POTION");
        String ingredientName = accessor.getString("ingredient", null);

        PotionType potionType = potionName == null ? null : PotionType.valueOf(potionName.toUpperCase());
        Material material = ingredientName == null ? null : Material.valueOf(ingredientName.toUpperCase());
        Material potionMaterial = Material.valueOf(potionMaterialName.toUpperCase());

        return new BrewAction(potionType, experience, money, potionMaterial, material, displayMaterial == null ? potionMaterial.name() : displayMaterial);
    }

    /**
     * Load a {@link JobAction} of type {@link JobActionType#RAFINE} from the given configuration accessor.
     * <p>
     * Expected keys :
     * <ul>
     *     <li>{@code material}         : the refined output identifier (vanilla material or {@code nexo:xxx}).</li>
     *     <li>{@code display-material} : the raw source identifier (the block the player mines).</li>
     *     <li>{@code plugin}           : the name of the external plugin providing the refinement inventory.</li>
     *     <li>{@code inventory-name}   : the name/title of the refinement inventory.</li>
     *     <li>{@code display-name}     : the display name that will be used on the raw dropped item.</li>
     *     <li>{@code min-chance}       : minimum refine chance rolled on drop (default 10).</li>
     *     <li>{@code max-chance}       : maximum refine chance rolled on drop (default 100).</li>
     * </ul>
     *
     * @param accessor        the configuration accessor
     * @param experience      experience reward on successful refinement
     * @param money           money reward on successful refinement
     * @param displayMaterial the display material used for the GUI (usually the source id)
     * @return the new RAFINE {@link JobAction}
     */
    private JobAction<?> loadRafineAction(TypedMapAccessor accessor, double experience, double money, String displayMaterial) {

        String materialName = accessor.getString("material");
        String sourceName = accessor.getString("display-material");
        String pluginName = accessor.getString("plugin", "");
        String inventoryName = accessor.getString("inventory-name", "");
        int minChance = accessor.getInt("min-chance", 10);
        int maxChance = accessor.getInt("max-chance", 100);

        if (materialName == null || sourceName == null) {
            plugin.getLogger().severe("RAFINE action requires 'material' and 'display-material' in file " + file.getAbsolutePath());
            return null;
        }

        String targetId = materialName.toLowerCase().startsWith("nexo:")
                ? materialName.toLowerCase()
                : materialName.toUpperCase();
        String sourceId = sourceName.toLowerCase().startsWith("nexo:")
                ? sourceName.toLowerCase()
                : sourceName.toUpperCase();

        // When no explicit display-material was passed to the main loader we fall back to the source id
        String finalDisplayMaterial = displayMaterial == null ? sourceId : displayMaterial;

        return new RafineAction(targetId, experience, money, finalDisplayMaterial, sourceId, pluginName, inventoryName, minChance, maxChance);
    }

    /**
     * Load a {@link JobAction} of type {@link JobActionType#FORGE} (or one of
     * its tier variants {@link JobActionType#FORGE_1}..{@link JobActionType#FORGE_4})
     * from the given configuration accessor.
     * <p>
     * Expected keys :
     * <ul>
     *     <li>{@code material}         : the forged output identifier (vanilla material or {@code nexo:xxx}).</li>
     *     <li>{@code display-material} : the item shown in the jobs info GUI (defaults to {@code material}).</li>
     *     <li>{@code display-name}     : the human-readable name of the action shown in the jobs info GUI.</li>
     * </ul>
     * The recipe (ingredients and fail chance) is declared separately in {@code items.yml} and
     * resolved at runtime by {@link fr.maxlego08.jobs.forge.ForgeManager}.
     * <p>
     * The {@code type} parameter is the concrete action type declared in the
     * job yaml so jobs can grant different rewards per forge tier (the inventory
     * tier is detected at runtime from the {@code ZJOBS_ITEM_FORGE_N} input
     * buttons present in the open menu).
     *
     * @param accessor        the configuration accessor
     * @param experience      experience reward on successful forging
     * @param money           money reward on successful forging
     * @param displayMaterial the display material used for the GUI
     * @param type            the concrete forge action type (FORGE, FORGE_1..FORGE_4)
     * @return the new FORGE {@link JobAction}, or {@code null} if required fields are missing
     */
    private JobAction<?> loadForgeAction(TypedMapAccessor accessor, double experience, double money, String displayMaterial, JobActionType type) {

        String materialName = accessor.getString("material");
        if (materialName == null) {
            plugin.getLogger().severe("FORGE action requires 'material' in file " + file.getAbsolutePath());
            return null;
        }

        String targetId = materialName.toLowerCase().startsWith("nexo:")
                ? materialName.toLowerCase()
                : materialName.toUpperCase();

        String finalDisplayMaterial = displayMaterial == null ? targetId : displayMaterial;

        return new ForgeAction(targetId, experience, money, finalDisplayMaterial, type);
    }
}
