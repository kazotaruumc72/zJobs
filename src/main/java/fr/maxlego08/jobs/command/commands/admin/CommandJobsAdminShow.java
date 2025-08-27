package fr.maxlego08.jobs.command.commands.admin;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;
import org.bukkit.OfflinePlayer;

public class CommandJobsAdminShow extends VCommand {

    public CommandJobsAdminShow(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_ADMIN_SHOW);
        this.addSubCommand("show");
        this.setDescription(Message.DESCRIPTION_ADMIN_SHOW);
        this.addRequireArgOfflinePlayer();
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);

        plugin.getJobManager().loadOfflinePlayer(offlinePlayer.getUniqueId(), playerJobs -> {

            message(this.plugin, sender, Message.ADMIN_POINTS_INFO, "%player%", offlinePlayer.getName(), "%points%", playerJobs.getPoints());

            if (playerJobs.getJobs().isEmpty()) {
                message(this.plugin, sender, Message.ADMIN_SHOW_EMPTY, "%player%", offlinePlayer.getName());
                return;
            }

            int amount = playerJobs.getJobs().size();
            message(this.plugin, sender, Message.ADMIN_SHOW_HEADER, "%player%", offlinePlayer.getName(), "%amount%", amount, "%s%", amount > 1 ? "s" : "");

            for (PlayerJob playerJob : playerJobs.getJobs()) {
                plugin.getJobManager().getJob(playerJob.getJobId()).ifPresent(job -> {
                    double maxExperience = job.getExperience(playerJob.getLevel(), playerJob.getPrestige());
                    message(this.plugin, sender, Message.ADMIN_SHOW_JOB_INFO,
                            "%job%", job.getName(),
                            "%level%", playerJob.getLevel(),
                            "%prestige%", playerJob.getPrestige(),
                            "%experience%", format(playerJob.getExperience()),
                            "%max_experience%", format(maxExperience));
                });
            }

        });

        return CommandType.SUCCESS;
    }
}

