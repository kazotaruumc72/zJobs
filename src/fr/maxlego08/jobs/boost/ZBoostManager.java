package fr.maxlego08.jobs.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.boost.BoostManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class ZBoostManager extends ZUtils  implements BoostManager {

    private final JobsPlugin plugin;

    public ZBoostManager(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void giveBoost(CommandSender sender, OfflinePlayer offlinePlayer, String jobName, JobActionType actionType, double experienceBoost, double moneyBoost, int amount) {

        var jobManager = this.plugin.getJobManager();
        var storageManager = this.plugin.getStorageManager();

        jobManager.loadOfflinePlayer(offlinePlayer.getUniqueId(), playerJobs -> {
            storageManager.createBoost(offlinePlayer.getUniqueId(), jobName, actionType, experienceBoost, moneyBoost, amount, boost -> {

                playerJobs.addBoost(boost);
                message(sender, Message.BOOST_CREATE_SUCCESS, "%player%", offlinePlayer.getName(), "%amount%", amount, "%experience%", format(experienceBoost), "%money%", format(moneyBoost));

            }, () -> message(sender, Message.BOOST_CREATE_ERROR));
        });
    }

    @Override
    public void loadBoostItems() {

    }
}
