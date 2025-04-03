package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface BoostManager {

    void createBoost(CommandSender sender, OfflinePlayer offlinePlayer, List<String> jobs, List<JobActionType> actions, List<String> targets, double moneyBoost, int amount, double experienceBoost);

    void removeBoost(CommandSender sender, OfflinePlayer offlinePlayer, int boostId);
}
