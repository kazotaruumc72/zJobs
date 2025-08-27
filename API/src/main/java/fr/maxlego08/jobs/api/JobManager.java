package fr.maxlego08.jobs.api;

import fr.maxlego08.jobs.api.enums.AdminAction;
import fr.maxlego08.jobs.api.enums.AttributeType;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface JobManager {

    /**
     * Load all jobs from the jobs folder. This method will load all jobs files,
     * create a new {@link Job} instance and register it in the internal map.
     *
     * @see Job
     */
    void loadJobs();

    /**
     * Load a job from the given file and return the job instance.
     *
     * @param file the file to load the job from
     * @return the job instance or null if the job could not be loaded
     */
    Job loadJob(File file);

    /**
     * Load the jobs of the given player from the database.
     * This method will load the jobs of the player from the database and
     * set the player jobs in the internal map.
     * If the player is not found in the database, a new player job will be
     * created.
     * This method should be called when a player join the server.
     * The given player must be a valid player.
     *
     * @param player the player to load the jobs for
     */
    void loadPlayerJobs(Player player);

    /**
     * Returns the job with the given id.
     * If the job is not found, an empty optional is returned.
     *
     * @param jobId the id of the job
     * @return the job instance or an empty optional if the job is not found
     */
    Optional<Job> getJob(String jobId);

    /**
     * Save the jobs of the given player and remove him from the internal map.
     * This method should be called when a player quit the server.
     * The given player must be a valid player.
     *
     * @param player the player to save the jobs for
     */
    void playerQuit(Player player);

    /**
     * Execute a job action.
     * This method will execute the given job action on the given target.
     * The given player must be a valid player.
     * The given target must be a valid target.
     * The given job action must be a valid action.
     *
     * @param player        the player to execute the action for
     * @param target        the target to execute the action on
     * @param jobActionType the job action to execute
     */
    void action(Player player, Object target, JobActionType jobActionType);

    /**
     * Returns the player jobs for the given uuid.
     * If the player jobs are not found, an empty optional is returned.
     *
     * @param uuid the uuid of the player
     * @return the player jobs instance or an empty optional if the player jobs are not found
     */
    Optional<PlayerJobs> getPlayerJobs(UUID uuid);

    /**
     * Returns the player jobs for the given uuid or create a new one if it does not exist.
     * This method is thread safe.
     *
     * @param uuid the uuid of the player
     * @return the player jobs instance
     */
    PlayerJobs getOrCreatePlayerJobs(UUID uuid);

    /**
     * Returns a list of all the job names available.
     *
     * @return a list of all the job names available
     */
    List<String> getJobsName();

    /**
     * Returns a list of all the job names available to the given sender.
     * If the sender is a player, the list will only contain the jobs that the player can join.
     * If the sender is a command block or the console, the list will contain all the jobs.
     *
     * @param sender the sender to get the job names for
     * @return a list of all the job names available to the given sender
     */
    List<String> getJobsName(CommandSender sender);

    /**
     * Join the given player to the job with the given name.
     * This method will check if the player can join the job and if the job is available.
     * If the player can join the job, his player jobs instance will be updated and the job will be added to his joined jobs.
     * If the player cannot join the job, nothing will happen.
     *
     * @param player the player to join to the job
     * @param name   the name of the job to join
     */
    void join(Player player, String name);

    /**
     * Leave the given player from the job with the given name.
     * This method will check if the player can leave the job and if the job is available.
     * If the player can leave the job, his player jobs instance will be updated and the job will be removed from his joined jobs.
     * If the player cannot leave the job, nothing will happen.
     * If the given confirm is true, the player will be asked to confirm before leaving the job.
     *
     * @param player  the player to leave from the job
     * @param name    the name of the job to leave
     * @param confirm if true, the player will be asked to confirm before leaving the job
     */
    void leave(Player player, String name, boolean confirm);

    /**
     * Update the given job attribute for the given offline player.
     * This method will check if the given sender has the permission to update the job attribute.
     * If the sender has the permission, the job attribute will be updated and the player jobs instance will be updated.
     * If the sender does not have the permission, nothing will happen.
     *
     * @param sender        the sender who is updating the job attribute
     * @param offlinePlayer the offline player who owns the job attribute
     * @param name          the name of the job attribute
     * @param value         the new value of the job attribute
     * @param action        the admin action to perform
     * @param type          the type of the job attribute
     */
    void updatePlayerJobAttribute(CommandSender sender, OfflinePlayer offlinePlayer, String name, double value, AdminAction action, AttributeType type);

    /**
     * Loads the player jobs of the given uuid and calls the given consumer when finished.
     * This method is thread safe.
     * The given uuid must be a valid uuid.
     * The given consumer must be a valid consumer.
     * The given consumer will be called when the player jobs are loaded.
     * The given consumer will receive the player jobs instance as parameter.
     * This method should be used to load the player jobs of an offline player.
     * This method is more efficient than {@link #getPlayerJobs(UUID)} because it does not create a new player jobs instance.
     *
     * @param uuid     the uuid of the player to load the jobs for
     * @param consumer the consumer to call when the player jobs are loaded
     */
    void loadOfflinePlayer(UUID uuid, Consumer<PlayerJobs> consumer);

    /**
     * Update the job economies of all the players.
     * This method will loop on all the player jobs and update their economies.
     * This method is thread safe.
     */
    void updateJobEconomies();

    /**
     * Adds the given points to the player with the given uuid.
     * This method will update the player jobs instance and save the player jobs.
     * The given uuid must be a valid uuid.
     * The given points must be a valid number of points.
     * This method is thread safe.
     *
     * @param uniqueId the uuid of the player to add points to
     * @param points   the number of points to add
     */
    void addPoints(UUID uniqueId, long points);

    /**
     * Removes the given points from the player with the given uuid.
     * This method will update the player jobs instance and save the player jobs.
     * The given uuid must be a valid uuid.
     * The given points must be a valid number of points.
     * This method is thread safe.
     *
     * @param uniqueId the uuid of the player to remove points from
     * @param points   the number of points to remove
     */
    void removePoints(UUID uniqueId, long points);

    /**
     * Update the points of the given offline player by the given number of points.
     * The given sender will receive a message with the result of the action.
     * The given action will be used to determine the type of action to perform.
     * This method is thread safe.
     *
     * @param sender        the sender of the command
     * @param offlinePlayer the offline player to update the points for
     * @param points        the number of points to add or remove
     * @param action        the type of action to perform
     */
    void updatePoints(CommandSender sender, OfflinePlayer offlinePlayer, int points, AdminAction action);

    /**
     * Sends a message to the given sender with the total points of the given offline player.
     * The message will contain the name of the player and the total points of the player.
     * This method is thread safe.
     *
     * @param sender the sender to send the message to
     * @param player the offline player to show the points for
     */
    void showPoints(CommandSender sender, OfflinePlayer player);

    /**
     * Returns the target job of the given player.
     * The target job is the job that the player is currently looking at.
     * This method is thread safe.
     *
     * @param player the player to get the target job for
     * @return the target job of the player
     */
    Job getTargetJob(Player player);

    /**
     * Sets the target job of the given player.
     * The target job is the job that the player is currently looking at.
     * This method is thread safe.
     *
     * @param player the player to set the target job for
     * @param job    the new target job
     */
    void setTargetJob(Player player, Job job);

    /* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */

    /**
     * Sets the given reward for the given offline player.
     * The given sender will receive a message with the result of the action.
     * The given rewardId must be a valid reward id.
     * The given rewardStatus must be a valid reward status.
     * This method is thread safe.
     *
     * @param sender        the sender of the command
     * @param offlinePlayer the offline player to set the reward for
     * @param rewardId      the id of the reward to set
     * @param rewardStatus  the status of the reward to set
     */
    /* <<<<<<<<<<  a515beab-f3e1-49de-9842-703f6b92090e  >>>>>>>>>>> */
    void setReward(CommandSender sender, OfflinePlayer offlinePlayer, String rewardId, boolean rewardStatus);
}
