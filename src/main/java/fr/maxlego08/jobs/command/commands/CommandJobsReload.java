package fr.maxlego08.jobs.command.commands;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;

public class CommandJobsReload extends VCommand {

    public CommandJobsReload(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_RELOAD);
        this.addSubCommand("reload", "rl");
        this.setDescription(Message.DESCRIPTION_RELOAD);
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {

        plugin.reloadFiles();
        message(this.plugin, sender, Message.RELOAD);

        return CommandType.SUCCESS;
    }

}
