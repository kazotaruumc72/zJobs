package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public interface BoostManager {

    void giveBoost(CommandSender sender, OfflinePlayer offlinePlayer, String jobName, JobActionType actionType, double experienceBoost, double moneyBoost, int amount);

    void loadBoostItems();
}
