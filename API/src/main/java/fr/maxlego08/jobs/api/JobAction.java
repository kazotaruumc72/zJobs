package fr.maxlego08.jobs.api;

import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.utils.ValueInformation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public interface JobAction<T> {

    /**
     * Get the type of action.
     *
     * @return the type of action
     */
    JobActionType getType();

    /**
     * Get the target of the action.
     *
     * @return the target of the action
     */
    T getTarget();

    /**
     * Get the experience given for this action.
     *
     * @return the experience given
     */
    double getExperience();

    /**
     * Get the money given for this action.
     *
     * @return the money given
     */
    double getMoney();

    /**
     * Checks if the given object is the target of this action.
     *
     * @param target the object to check
     * @return true if the object is the target of this action, false otherwise
     */
    boolean isAction(Object target);

    /**
     * Apply the action to the given item stack.
     *
     * @param itemStack the item stack to apply the action to
     */
    void applyItemStack(ItemStack itemStack);

    /**
     * Get the material that should be used to represent this action in the gui.
     *
     * @return the material to use for the gui
     */
    String getDisplayMaterial();

    /**
     * Gets the display name for the action in the gui.
     *
     * @return the display name for the action
     */
    String getDisplayName();

    ValueInformation toValueInformation();
}
