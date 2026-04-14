package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import org.bukkit.entity.Player;

import java.util.List;

public interface PlayerBoosts {

    List<Boost> getBoosts();

    BoostResult processBoost(Job job, JobAction<?> jobAction, Object element);

    /**
     * Process a boost for a job action, resolving any PlaceholderAPI placeholders
     * in experience/money values for the given player.
     *
     * @param job       the job
     * @param jobAction the job action
     * @param element   the target element
     * @param player    the player to resolve placeholders for
     * @return the boost result with resolved values
     */
    default BoostResult processBoost(Job job, JobAction<?> jobAction, Object element, Player player) {
        return processBoost(job, jobAction, element);
    }

    void addBoost(Boost boost);

    boolean contains(int boostId);

    void delete(int boostId);

    boolean isEmpty();
}
