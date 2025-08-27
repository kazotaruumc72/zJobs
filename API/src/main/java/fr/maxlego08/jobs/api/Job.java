package fr.maxlego08.jobs.api;

import fr.maxlego08.jobs.api.enums.JobActionType;

import java.util.Collection;
import java.util.Optional;

public interface Job {

    /**
     * Get the name of this job.
     *
     * @return The name of this job.
     */
    String getName();

    /**
     * Gets the name of the file associated with this job.
     *
     * @return The name of the file associated with this job.
     */
    String getFileName();

    /**
     * Get the base experience needed to level up in this job.
     *
     * @return The base experience needed to level up in this job.
     */
    double getBaseExperience();

    /**
     * Get the maximum amount of levels a player can reach in this job.
     *
     * @return The maximum amount of levels a player can reach in this job.
     */
    int getMaxLevels();

    /**
     * Get the maximum amount of prestige a player can reach in this job.
     *
     * @return The maximum amount of prestige a player can reach in this job.
     */
    int getMaxPrestiges();

    /**
     * Get the formula used to calculate the experience required to level up in this job.
     * This formula should be a valid {@link java.util.function.Function} that takes two parameters: the current level and the current prestige.
     *
     * @return The formula used to calculate the experience required to level up in this job.
     */
    String getFormula();

    /**
     * Get all the actions registered for this job.
     *
     * @return A {@link Collection} containing all the actions registered for this job.
     */
    Collection<JobAction<?>> getActions();

    /**
     * Gets all the rewards that players can receive when leveling up in this job.
     *
     * @return A {@link Collection} containing all the rewards that players can receive when leveling up in this job.
     */
    Collection<JobReward> getRewards();

    /**
     * Gets the experience matrix of this job.
     * The matrix is a two-dimensional array of doubles, where the first index represents the level and the second index represents the prestige.
     * The value at a given index is the experience required to level up in this job at that level and prestige.
     *
     * @return The experience matrix of this job.
     */
    double[][] getMatrix();

    /**
     * Gets the experience required for a player to level up in this job, given his current level and prestige.
     *
     * @param level    The current level of the player.
     * @param prestige The current prestige of the player.
     * @return The experience required for a player to level up in this job, given his current level and prestige.
     */
    double getExperienceForNextLevel(int level, int prestige);

    /**
     * Gets the experience a player has at a given level and prestige in this job.
     *
     * @param level    The level of the player.
     * @param prestige The prestige of the player.
     * @return The experience a player has at a given level and prestige in this job.
     */
    double getExperience(int level, int prestige);

    /**
     * Gets a job action for a given type and target.
     *
     * @param action The type of the action.
     * @param target The target of the action.
     * @return An {@link Optional} containing the job action if found, or an empty optional if not.
     */
    Optional<JobAction<?>> getAction(JobActionType action, Object target);

    /**
     * Whether or not players are allowed to leave this job.
     *
     * @return {@code true} if players are allowed to leave this job, {@code false} otherwise.
     */
    boolean canLeave();

    /**
     * Whether or not players are allowed to join this job.
     *
     * @return {@code true} if players are allowed to join this job, {@code false} otherwise.
     */
    boolean canJoin();

    /**
     * Gets the custom model data for this job. This is used to display custom item models representing jobs.
     *
     * @return The custom model data for this job.
     */
    int getCustomModelData();
}
