package fr.maxlego08.jobs.command.commands.admin.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.command.VCommand;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.Permission;
import fr.maxlego08.jobs.zcore.utils.commands.CommandType;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandJobsAdminBoostCreate extends VCommand {

    public CommandJobsAdminBoostCreate(JobsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ZJOBS_ADMIN_BOOST_CREATE);
        this.addSubCommand("create");
        this.setDescription(Message.DESCRIPTION_ADMIN_BOOST_REMOVE);
        this.addRequireArg("player");
        this.addRequireArg("boost amount", (a, b) -> Arrays.asList("100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"));
        this.addRequireArg("experience boost", (a, b) -> Arrays.asList("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9"));
        this.addRequireArg("money boost", (a, b) -> Arrays.asList("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9"));
        this.addOptionalArg("jobs", (a, args) -> {
            if (args.length < 6) return getJobsName(null);
            return getJobsName(args[5]);
        });
        this.addOptionalArg("actions", (a, args) -> {
            if (args.length < 7) return getActionsName(null);
            return getActionsName(args[6]);
        });
        this.addOptionalArg("targets", (a, args) -> {
            if (args.length < 8) return getTargetName(null);
            return getTargetName(args[7]);
        });
    }

    @Override
    protected CommandType perform(JobsPlugin plugin) {

        OfflinePlayer offlinePlayer = this.argAsOfflinePlayer(0);
        int boostAmount = this.argAsInteger(1);
        double experienceBoost = this.argAsDouble(2);
        double moneyBoost = this.argAsDouble(3);

        String jobName = this.argAsString(4, "all");
        String actionName = this.argAsString(5, "all");
        String targetName = this.argAsString(6, "all");

        List<String> jobs = jobName.equals("all") ? List.of() : Arrays.stream(jobName.split(",")).toList();
        List<JobActionType> actions = actionName.equals("all") ? List.of() : Arrays.stream(actionName.split(",")).map(JobActionType::valueOf).toList();
        List<String> targets = targetName.equals("all") ? List.of() : Arrays.stream(targetName.split(",")).toList();

        plugin.getBoostManager().createBoost(sender, offlinePlayer, jobs, actions, targets, moneyBoost, boostAmount, experienceBoost);

        return CommandType.SUCCESS;
    }

    private List<String> getJobsName(String currentValues) {
        return getCompletion(new ArrayList<>(this.plugin.getJobManager().getJobsName()), currentValues);
    }

    private List<String> getActionsName(String currentValues) {
        return getCompletion(Arrays.stream(JobActionType.values()).map(JobActionType::name).collect(Collectors.toList()), currentValues);
    }

    private List<String> getTargetName(String currentValues) {
        List<String> values = new ArrayList<>();
        values.addAll(Arrays.stream(Material.values()).filter(e -> !e.isLegacy()).map(Material::name).toList());
        values.addAll(Arrays.stream(EntityType.values()).filter(EntityType::isAlive).map(EntityType::name).toList());
        return getCompletion(values, currentValues);
    }

    private List<String> getCompletion(List<String> values, String currentValues) {

        if (currentValues == null || currentValues.isEmpty()) {
            values.add("all");
            return values;
        }

        int count = (int) currentValues.chars().filter(ch -> ch == ',').count();
        if (count == 0) {
            values.add("all");
            return values;
        }

        List<String> results = new ArrayList<>();
        for (String jobName : currentValues.split(",")) {
            values.removeIf(e -> e.equalsIgnoreCase(jobName));
        }

        int lastIndex = currentValues.lastIndexOf(',');
        if (lastIndex != -1) {
            String prefix = currentValues.substring(0, lastIndex);
            for (String job : values) {
                results.add(prefix + "," + job);
            }
        }
        return results;
    }

}
