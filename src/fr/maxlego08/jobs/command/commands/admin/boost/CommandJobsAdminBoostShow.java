package fr.maxlego08.jobs.command.commands.admin.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;

public class CommandJobsAdminBoostShow extends VCommand {

    public CommandJobsAdminBoostShow(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_ADMIN_BOOST_SHOW);
        this.addSubCommand("show");
        this.setDescription(Message.DESCRIPTION_ADMIN_BOOST_SHOW);
        this.addRequireArg("player");
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);
        plugin.getBoostManager().showBoosts(sender, offlinePlayer);

        return CommandType.SUCCESS;
    }

}
