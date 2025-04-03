package fr.maxlego08.jobs.boost;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.dto.PlayerBoostDTO;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ZBoost implements Boost {

    private final int id;
    private final Date createdAt;
    private final List<String> jobs;
    private final List<JobActionType> actions;
    private final List<String> targets;
    private final int boostAmount;
    private final double experienceBoost;
    private final double moneyBoost;
    private int remainingBoost;

    public ZBoost(PlayerBoostDTO dto) {
        this.id = dto.id();
        this.createdAt = dto.created_at();
        this.jobs = Arrays.stream(dto.jobs().split(",")).toList();
        this.targets = Arrays.stream(dto.targets().split(",")).toList();
        this.actions = Arrays.stream(dto.actions().split(",")).map(JobActionType::valueOf).toList();
        this.boostAmount = dto.boost_amount();
        this.experienceBoost = dto.experience_boost();
        this.moneyBoost = dto.money_boost();
        this.remainingBoost = dto.remaining_boost();
    }

    public ZBoost(int id, List<String> jobs, List<JobActionType> actions, List<String> targets, int boostAmount, double experienceBoost, double moneyBoost) {
        this.id = id;
        this.createdAt = new Date();
        this.jobs = jobs;
        this.actions = actions;
        this.targets = targets;
        this.boostAmount = boostAmount;
        this.experienceBoost = experienceBoost;
        this.moneyBoost = moneyBoost;
        this.remainingBoost = boostAmount;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public List<String> getJobs() {
        return this.jobs;
    }

    @Override
    public List<String> getTargets() {
        return targets;
    }

    @Override
    public List<JobActionType> getActions() {
        return this.actions;
    }

    @Override
    public double getMoneyBoost() {
        return moneyBoost;
    }

    @Override
    public int getBoostAmount() {
        return this.boostAmount;
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
    public boolean canProcess(Job job, JobAction<?> action, Object element) {

        if (remainingBoost <= 0) return false;

        if (!jobs.isEmpty() && jobs.stream().noneMatch(jobName -> job.getFileName().equalsIgnoreCase(jobName))) return false;

        if (!actions.isEmpty() && actions.stream().noneMatch(actionType -> action.getType() == actionType)) return false;

        return element == null || targets.isEmpty() || targets.stream().anyMatch(target -> target.equalsIgnoreCase(element.toString()));
    }

    @Override
    public void removeRemainingBoost(int amount) {
        this.remainingBoost -= amount;
    }

    @Override
    public boolean isEmpty() {
        return this.remainingBoost <= 0;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }
}
