package fr.maxlego08.jobs.api.boost;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.enums.JobActionType;

public interface Boost {

    int getId();

    String getJobName();

    JobActionType getAction();

    double getExperienceBoost();

    double getMoneyBoost();

    int getRemainingBoost();

    void setRemainingBoost(int remainingBoost);

    boolean canProcess(Job job, JobAction<?> action);

    void removeRemainingBoost(int amount);
}
