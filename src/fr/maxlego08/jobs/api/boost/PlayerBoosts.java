package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;

import java.util.List;

public interface PlayerBoosts {

    List<Boost> getBoosts();

    BoostResult processBoost(Job job, JobAction<?> jobAction, Object element);

    void addBoost(Boost boost);

    boolean contains(int boostId);

    void delete(int boostId);

    boolean isEmpty();
}
