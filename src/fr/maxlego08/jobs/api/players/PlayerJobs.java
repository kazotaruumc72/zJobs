package fr.maxlego08.jobs.api.players;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.actions.ActionInfo;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.boost.PlayerBoosts;
import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player with its jobs.
 *
 * @author Maxlego08
 */
public interface PlayerJobs {

    /**
     * Get the unique id of the player.
     *
     * @return the uuid of the player.
     */
    UUID getUniqueId();

    /**
     * Get the list of jobs of the player.
     *
     * @return the list of jobs of the player.
     */
    List<PlayerJob> getJobs();

    /**
     * Check if the player has a job.
     *
     * @param job the job to check.
     * @return true if the player has the job, false otherwise.
     */
    boolean hasJob(Job job);

    /**
     * Join a job.
     *
     * @param job the job to join.
     */
    void join(Job job);

    /**
     * Leave a job.
     *
     * @param job the job to leave.
     */
    void leave(Job job);

    /**
     * Get a job of the player.
     *
     * @param job the job to get.
     * @return an optional containing the job, or an empty optional if the player
     * doesn't have the job.
     */
    Optional<PlayerJob> get(Job job);

    /**
     * Get a job of the player by its id.
     *
     * @param jobId the id of the job to get.
     * @return an optional containing the job, or an empty optional if the player
     * doesn't have the job.
     */
    Optional<PlayerJob> get(String jobId);

    /**
     * Get the number of jobs of the player.
     *
     * @return the number of jobs of the player.
     */
    int size();

    /**
     * Process a job action of the player.
     *
     * @param player the player who triggered the action.
     * @param target the target of the action.
     * @param action the action to process.
     */
    void action(Player player, Object target, JobActionType action);

    /**
     * Process the player's job experience.
     *
     * @param player      the player who triggered the action.
     * @param playerJob   the job of the player.
     * @param job         the job that triggered the action.
     * @param experience  the experience to process.
     * @param initialCall true if it's the first time the experience is processed,
     *                    false otherwise.
     * @param actionInfo  the action info of the action.
     */
    void process(Player player, PlayerJob playerJob, Job job, double experience, boolean initialCall, ActionInfo<?> actionInfo);

    /**
     * Update the job economies of the player.
     */
    void updateJobEconomies();

    /**
     * Get the points of the player.
     *
     * @return the points of the player.
     */
    long getPoints();

    /**
     * Set the points of the player.
     *
     * @param points the points to set.
     */
    void setPoints(long points);

    /**
     * Add points to the player.
     *
     * @param points the points to add.
     */
    void addPoints(long points);

    /**
     * Remove points from the player.
     *
     * @param points the points to remove.
     */
    void removePoints(long points);

    /**
     * Get the rewards of the player.
     *
     * @return the rewards of the player.
     */
    Set<String> getRewards();

    /**
     * Gets the boosts of the player.
     *
     * @return the boosts of the player.
     */
    PlayerBoosts getBoosts();

    /**
     * Add a boost to the player's current boosts.
     *
     * @param boost the boost to add.
     */
    void addBoost(Boost boost);
}
