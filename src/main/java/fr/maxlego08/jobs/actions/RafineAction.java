package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;

/**
 * Action of type {@link JobActionType#RAFINE}.
 * <p>
 * The {@code target} stored by this action is the identifier of the <b>refined</b>
 * item the player receives when refinement is successful (for example
 * {@code nexo:amethyst} or {@code DIAMOND}).
 * </p>
 * <p>
 * The {@code sourceId} is the identifier of the block/item that the player mines
 * and that will later be refined inside the configured inventory (for example
 * {@code nexo:moonstone}).
 * </p>
 */
public class RafineAction extends ZJobAction<String> {

    private final String sourceId;
    private final String pluginName;
    private final String inventoryName;
    private final int minChance;
    private final int maxChance;

    public RafineAction(String targetId, double experience, double money, String displayMaterial,
                        String sourceId, String pluginName, String inventoryName,
                        int minChance, int maxChance) {
        super(targetId, experience, money, displayMaterial);
        this.sourceId = sourceId;
        this.pluginName = pluginName;
        this.inventoryName = inventoryName;
        this.minChance = minChance;
        this.maxChance = maxChance;
    }

    @Override
    public JobActionType getType() {
        return JobActionType.RAFINE;
    }

    @Override
    public boolean isAction(Object target) {
        return target instanceof String s && s.equalsIgnoreCase(this.target);
    }

    /**
     * @return the identifier of the raw source block (e.g. {@code nexo:moonstone}).
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @return the name of the external plugin handling the refinement inventory
     * (purely informative, e.g. {@code RecipeBook}).
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * @return the name of the inventory in which the player will refine the item.
     */
    public String getInventoryName() {
        return inventoryName;
    }

    public int getMinChance() {
        return minChance;
    }

    public int getMaxChance() {
        return maxChance;
    }
}

