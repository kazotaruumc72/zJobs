package fr.maxlego08.jobs.placeholder;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.save.Config;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import fr.maxlego08.menu.api.utils.Placeholders;

import java.util.Optional;
import java.util.stream.Collectors;

public class BoostPlaceholder extends ZUtils {

    public Placeholders getPlaceholders(Boost boost, JobManager manager) {

        var config = Config.boostPlaceholderConfig;
        Placeholders placeholders = new Placeholders();

        placeholders.register("boost-jobs", getJobs(boost, manager));
        placeholders.register("boost-actions", getActions(boost));
        placeholders.register("boost-targets", getTargets(boost));
        placeholders.register("boost-remaining", format(boost.getRemainingBoost()));
        placeholders.register("boost-amount", format(boost.getBoostAmount()));
        placeholders.register("boost-experience", format(boost.getExperienceBoost()));
        placeholders.register("boost-money", format(boost.getMoneyBoost()));
        placeholders.register("boost-id", String.valueOf(boost.getId()));
        return placeholders;
    }

    public String getJobs(Boost boost, JobManager manager) {
        var config = Config.boostPlaceholderConfig;
        return boost.getJobs().isEmpty() ? config.everyJobs() : boost.getJobs().stream().map(manager::getJob).filter(Optional::isPresent).map(Optional::get).map(Job::getName).collect(Collectors.joining(", "));
    }

    public String getActions(Boost boost) {
        var config = Config.boostPlaceholderConfig;
        return boost.getTargets().isEmpty() ? config.everyTargets() : String.join(", ", boost.getTargets());
    }

    public String getTargets(Boost boost) {
        var config = Config.boostPlaceholderConfig;
        return boost.getTargets().isEmpty() ? config.everyTargets() : String.join(", ", boost.getTargets());
    }

}
