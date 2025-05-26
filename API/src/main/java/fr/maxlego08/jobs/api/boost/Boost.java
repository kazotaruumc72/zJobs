package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.enums.JobActionType;

import java.util.Date;
import java.util.List;

public interface Boost {

    int getId();

    List<String> getJobs();

    List<JobActionType> getActions();

    List<String> getTargets();

    double getExperienceBoost();

    double getMoneyBoost();

    int getBoostAmount();

    int getRemainingBoost();

    void setRemainingBoost(int remainingBoost);

    boolean canProcess(Job job, JobAction<?> action, Object element);

    void removeRemainingBoost(int amount);

    boolean isEmpty();

    Date getCreatedAt();
}
