package fr.maxlego08.jobs.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.boost.BoostResult;
import fr.maxlego08.jobs.api.boost.PlayerBoosts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ZPlayerBoosts implements PlayerBoosts {

    private final JobsPlugin plugin;
    private final List<Boost> boosts = new ArrayList<>();

    public ZPlayerBoosts(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<Boost> getBoosts() {
        return this.boosts;
    }

    @Override
    public BoostResult processBoost(Job job, JobAction<?> jobAction) {

        var optional = this.boosts.stream().filter(boost -> boost.canProcess(job, jobAction)).max(Comparator.comparingDouble(Boost::getExperienceBoost));
        if (optional.isEmpty()) {
            return new BoostResult(jobAction.getExperience(), jobAction.getMoney(), null);
        }

        var boost = optional.get();
        boost.removeRemainingBoost(1);

        // ToDo, update storage

        return new BoostResult(jobAction.getExperience() * boost.getExperienceBoost(), jobAction.getMoney() * boost.getMoneyBoost(), boost);
    }

    @Override
    public void addBoost(Boost boost) {
        this.boosts.add(boost);
    }
}
