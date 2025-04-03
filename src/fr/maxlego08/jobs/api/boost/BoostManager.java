package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface BoostManager {

    /**
     * Creates a boost for a specified offline player.
     *
     * @param sender          the command sender issuing the boost creation
     * @param offlinePlayer   the offline player for whom the boost is created
     * @param jobs            the list of job names to which the boost applies, empty if applicable to all jobs
     * @param actions         the list of job action types to which the boost applies, empty if applicable to all actions
     * @param targets         the list of specific targets for the boost, empty if applicable to all targets
     * @param moneyBoost      the multiplier for money earnings during the boost
     * @param amount          the total number of boosts available
     * @param experienceBoost the multiplier for experience earnings during the boost
     */
    void createBoost(CommandSender sender, OfflinePlayer offlinePlayer, List<String> jobs, List<JobActionType> actions, List<String> targets, double moneyBoost, int amount, double experienceBoost);

    /**
     * Removes a boost from a specified offline player.
     *
     * @param sender        the command sender requesting the boost removal
     * @param offlinePlayer the offline player from whom the boost is to be removed
     * @param boostId       the unique identifier of the boost to be removed
     */
    void removeBoost(CommandSender sender, OfflinePlayer offlinePlayer, int boostId);

    /**
     * Shows all the boosts of a specified offline player.
     *
     * @param sender        the command sender requesting the boost list
     * @param offlinePlayer the offline player for whom the boosts are shown
     */
    void showBoosts(CommandSender sender, OfflinePlayer offlinePlayer);
}
