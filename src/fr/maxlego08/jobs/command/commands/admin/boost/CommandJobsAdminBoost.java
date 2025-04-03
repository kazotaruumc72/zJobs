package fr.maxlego08.jobs.command.commands.admin.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;

public class CommandJobsAdminBoost extends VCommand {

    public CommandJobsAdminBoost(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_ADMIN_BOOST);
        this.addSubCommand("boost", "b");
        this.setDescription(Message.DESCRIPTION_ADMIN_BOOST);
        this.addSubCommand(new CommandJobsAdminBoostCreate(plugin));
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {
        syntaxMessage();
        return CommandType.SUCCESS;
    }

}
