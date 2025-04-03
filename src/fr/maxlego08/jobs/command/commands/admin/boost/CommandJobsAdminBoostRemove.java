package fr.maxlego08.jobs.command.commands.admin.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;

public class CommandJobsAdminBoostRemove extends VCommand {

    public CommandJobsAdminBoostRemove(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_ADMIN_BOOST_REMOVE);
        this.addSubCommand("remove");
        this.setDescription(Message.DESCRIPTION_ADMIN_BOOST_REMOVE);
        this.addRequireArg("player");
        this.addRequireArg("boost id", (sender, args) -> {
            String playerName = args[1];
            var player = plugin.getServer().getPlayer(playerName);
            if (player == null) return new ArrayList<>();
            var optional = plugin.getJobManager().getPlayerJobs(player.getUniqueId());
            if (optional.isEmpty()) return new ArrayList<>();
            var playerJobs = optional.get();
            return playerJobs.getBoosts().getBoosts().stream().map(Boost::getId).map(String::valueOf).toList();
        });
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);
        int boostId = this.argAsInteger(1);

        plugin.getBoostManager().removeBoost(sender, offlinePlayer, boostId);

        return CommandType.SUCCESS;
    }

}
