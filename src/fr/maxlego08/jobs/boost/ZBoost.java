package fr.maxlego08.jobs.boost;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;

public class ZBoost implements Boost {

    private final int id;
    private final String jobName;
    private final JobActionType actionType;
    private final double experienceBoost;
    private final double moneyBoost;
    private int remainingBoost;

    public ZBoost(int id, String jobName, JobActionType actionType, double experienceBoost, double moneyBoost) {
        this.id = id;
        this.jobName = jobName;
        this.actionType = actionType;
        this.experienceBoost = experienceBoost;
        this.moneyBoost = moneyBoost;
    }

    public ZBoost(int id, String jobName, JobActionType actionType, double experienceBoost, double moneyBoost, int remainingBoost) {
        this.id = id;
        this.jobName = jobName;
        this.actionType = actionType;
        this.experienceBoost = experienceBoost;
        this.moneyBoost = moneyBoost;
        this.remainingBoost = remainingBoost;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public JobActionType getAction() {
        return this.actionType;
    }

    @Override
    public double getMoneyBoost() {
        return moneyBoost;
    }

    @Override
    public double getExperienceBoost() {
        return experienceBoost;
    }

    @Override
    public int getRemainingBoost() {
        return remainingBoost;
    }

    @Override
    public void setRemainingBoost(int remainingBoost) {
        this.remainingBoost = remainingBoost;
    }

    @Override
    public boolean canProcess(Job job, JobAction<?> action) {
        return (this.jobName == null || this.jobName.equalsIgnoreCase(job.getName())) && (this.actionType == null || this.actionType == action.getType());
    }

    @Override
    public void removeRemainingBoost(int amount) {
        this.remainingBoost -= amount;
    }
}
