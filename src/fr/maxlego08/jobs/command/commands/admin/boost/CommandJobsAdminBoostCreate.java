package fr.maxlego08.jobs.command.commands.admin.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandJobsAdminBoostCreate extends VCommand {

    public CommandJobsAdminBoostCreate(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_ADMIN_BOOST);
        this.addSubCommand("create");
        this.setDescription(Message.DESCRIPTION_ADMIN_BOOST);
        this.addRequireArg("player");
        this.addRequireArg("boost amount", (a, b) -> Arrays.asList("100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"));
        this.addRequireArg("experience boost", (a, b) -> Arrays.asList("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9"));
        this.addRequireArg("money boost", (a, b) -> Arrays.asList("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9"));
        this.addOptionalArg("job", (a, b) -> Stream.concat(plugin.getJobManager().getJobsName().stream(), Stream.of("all")).collect(Collectors.toList()));
        this.addOptionalArg("action", (a, b) -> Stream.concat(Arrays.stream(JobActionType.values()).map(JobActionType::name), Stream.of("all")).collect(Collectors.toList()));
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);
        int boostAmount = this.argAsInteger(1);
        double experienceBoost = this.argAsDouble(2);
        double moneyBoost = this.argAsDouble(3);

        String job = this.argAsString(4, null);
        if (job != null && job.equalsIgnoreCase("all")) {
            job = null;
        }

        String actionName = this.argAsString(5, null);
        JobActionType jobActionType = null;

        if (actionName != null && !actionName.equalsIgnoreCase("all")) {
            jobActionType = JobActionType.valueOf(actionName.toUpperCase());
        }

        plugin.getBoostManager().giveBoost(sender, offlinePlayer, job, jobActionType, experienceBoost, moneyBoost, boostAmount);

        return CommandType.SUCCESS;
    }

}
