package fr.maxlego08.jobs.boost;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.boost.BoostResult;
import fr.maxlego08.jobs.api.boost.PlayerBoosts;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ZPlayerBoosts implements PlayerBoosts {

    private final JobsPlugin plugin;
    private final List<Boost> boosts;

    public ZPlayerBoosts(JobsPlugin plugin) {
        this.plugin = plugin;
        this.boosts = new ArrayList<>();
    }

    public ZPlayerBoosts(JobsPlugin plugin, List<Boost> boosts) {
        this.plugin = plugin;
        this.boosts = boosts;
    }

    @Override
    public List<Boost> getBoosts() {
        return this.boosts;
    }

    @Override
    public BoostResult processBoost(Job job, JobAction<?> jobAction, Object element) {
        return processBoost(job, jobAction, element, null);
    }

    @Override
    public BoostResult processBoost(Job job, JobAction<?> jobAction, Object element, Player player) {

        var optional = this.boosts.stream().filter(boost -> boost.canProcess(job, jobAction, element)).max(Comparator.comparingDouble(Boost::getExperienceBoost));
        double experience = jobAction.getExperience(player);
        double money = jobAction.getMoney(player);

        if (optional.isEmpty()) {
            return new BoostResult(experience, money, null);
        }

        var boost = optional.get();
        boost.removeRemainingBoost(1);

        return new BoostResult(experience * boost.getExperienceBoost(), money * boost.getMoneyBoost(), boost);
    }

    @Override
    public void addBoost(Boost boost) {
        this.boosts.add(boost);
    }

    @Override
    public boolean contains(int boostId) {
        return this.boosts.stream().anyMatch(boost -> boost.getId() == boostId);
    }

    @Override
    public void delete(int boostId) {
        this.boosts.removeIf(boost -> boost.getId() == boostId);
    }

    @Override
    public boolean isEmpty() {
        return this.boosts.isEmpty();
    }
}
