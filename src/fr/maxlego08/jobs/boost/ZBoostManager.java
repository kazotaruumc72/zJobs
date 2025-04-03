package fr.maxlego08.jobs.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.boost.BoostManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ZBoostManager extends ZUtils implements BoostManager {

    private final JobsPlugin plugin;

    public ZBoostManager(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createBoost(CommandSender sender, OfflinePlayer offlinePlayer, List<String> jobs, List<JobActionType> actions, List<String> targets, double moneyBoost, int amount, double experienceBoost) {

        var jobManager = this.plugin.getJobManager();
        var storageManager = this.plugin.getStorageManager();

        jobManager.loadOfflinePlayer(offlinePlayer.getUniqueId(), playerJobs -> {
            storageManager.createBoost(offlinePlayer.getUniqueId(), jobs, actions, targets, moneyBoost, experienceBoost, amount, boost -> {

                playerJobs.addBoost(boost);
                message(sender, Message.BOOST_CREATE_SUCCESS, "%player%", offlinePlayer.getName(), "%amount%", amount, "%experience%", format(experienceBoost), "%money%", format(moneyBoost));

            }, () -> message(sender, Message.BOOST_CREATE_ERROR));
        });
    }

    @Override
    public void removeBoost(CommandSender sender, OfflinePlayer offlinePlayer, int boostId) {

        var jobManager = this.plugin.getJobManager();
        var storageManager = this.plugin.getStorageManager();

        jobManager.loadOfflinePlayer(offlinePlayer.getUniqueId(), playerJobs -> {

            if (!playerJobs.getBoosts().contains(boostId)) {
                message(sender, Message.BOOST_REMOVE_ERROR, "%player%", offlinePlayer.getName(), "%id%", boostId);
                return;
            }


            playerJobs.getBoosts().delete(boostId);
            storageManager.deleteBoost(offlinePlayer.getUniqueId(), boostId);

            message(sender, Message.BOOST_REMOVE_SUCCESS, "%player%", offlinePlayer.getName(), "%id%", boostId);
        });
    }
}
