package fr.maxlego08.jobs;

import fr.maxlego08.jobs.actions.BrewAction;
import fr.maxlego08.jobs.actions.EnchantmentAction;
import fr.maxlego08.jobs.actions.EntityAction;
import fr.maxlego08.jobs.actions.MaterialAction;
import fr.maxlego08.jobs.actions.TagAction;
import fr.maxlego08.jobs.actions.ZJobAction;
import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.JobReward;
import fr.maxlego08.jobs.api.enums.JobActionType;
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

        return new ZJob(name, file.getName().replace(".yml", ""), baseExperience, maxLevels, maxPrestiges, formula, jobActions, jobRewards, canJoin, canLeave, customModelData);
    }

    private List<JobAction<?>> loadActions(YamlConfiguration configuration) {
        List<JobAction<?>> jobActions = new ArrayList<>();

        configuration.getMapList("actions").forEach(map -> {
            TypedMapAccessor accessor = new TypedMapAccessor((Map<String, Object>) map);
            double experience = accessor.getDouble("experience", 0);
            double money = accessor.getDouble("money", 0);
            try {

                JobActionType jobActionType = JobActionType.valueOf(accessor.getString("type").toUpperCase());
                String displayMaterialName = accessor.getString("display-material", null);
                Material displayMaterial = displayMaterialName == null ? null : Material.valueOf(displayMaterialName.toUpperCase());
                String displayName = accessor.getString("display-name", "Name not found");
                JobAction<?> jobAction = null;

                if (jobActionType.isMaterial()) {

                    if (accessor.contains("material")) {
                        Material material = Material.valueOf(accessor.getString("material").toUpperCase());
                        jobAction = new MaterialAction(material, experience, money, jobActionType, displayMaterial == null ? material : displayMaterial);
                    } else if (accessor.contains("tag")) {
                        Tag<Material> tag = TagRegistry.getTag(accessor.getString("tag").toUpperCase());
                        jobAction = new TagAction(tag, experience, money, jobActionType, displayMaterial == null ? Material.PAPER : displayMaterial);
                    } else {
                        this.plugin.getLogger().severe("Impossible to find the tag or material for " + jobActionType + " in file " + file.getAbsolutePath());
                    }

                } else if (jobActionType.isEntityType()) {

                    EntityType entityType = EntityType.valueOf(accessor.getString("entity").toUpperCase());
                    jobAction = new EntityAction(entityType, experience, money, jobActionType, displayMaterial == null ? EntityTypeToEggConverter.getSpawnEgg(entityType) : displayMaterial);

                } else if (jobActionType == JobActionType.ENCHANT) {

                    jobAction = loadEnchantAction(accessor, experience, money, displayMaterial);

                } else if (jobActionType == JobActionType.BREW) {

                    jobAction = loadBrewAction(accessor, experience, money, displayMaterial);
                }

                if (jobAction != null) {
                    ((ZJobAction<?>) jobAction).setDisplayName(displayName);
                    jobActions.add(jobAction);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        return jobActions;
    }

    @Override
    public void save(Job object, YamlConfiguration configuration, String path) {

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
    private JobAction<?> loadEnchantAction(TypedMapAccessor accessor, double experience, double money, Material displayMaterial) {
        Enchantments enchantments = plugin.getInventoryManager().getEnchantments();
        String enchantmentName = accessor.getString("enchantment");
        String materialName = accessor.getString("material", null);

        Material material = materialName == null ? null : Material.valueOf(materialName.toUpperCase());
        Enchantment enchantment = enchantmentName == null ? null : enchantments.getEnchantments(enchantmentName).map(MenuEnchantment::getEnchantment).orElse(null);
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
    private JobAction<?> loadBrewAction(TypedMapAccessor accessor, double experience, double money, Material displayMaterial) {
        String potionName = accessor.getString("potion-type", null);
        String potionMaterialName = accessor.getString("potion-material", "POTION");
        String ingredientName = accessor.getString("ingredient", null);

        PotionType potionType = potionName == null ? null : PotionType.valueOf(potionName.toUpperCase());
        Material material = ingredientName == null ? null : Material.valueOf(ingredientName.toUpperCase());
        Material potionMaterial = Material.valueOf(potionMaterialName.toUpperCase());

        return new BrewAction(potionType, experience, money, potionMaterial, material, displayMaterial == null ? potionMaterial : displayMaterial);
    }
}
