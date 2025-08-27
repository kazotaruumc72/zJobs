package fr.maxlego08.jobs.api;

import fr.maxlego08.menu.api.requirement.Action;

import java.util.List;

public interface JobReward {

    /**
     * Get the level of the reward.
     *
     * @return the level of the reward.
     */
    int getLevel();

    /**
     * Get the prestige of the reward.
     *
     * @return the prestige of the reward.
     */
    int getPrestige();

    /**
     * Get all the actions registered for this reward.
     *
     * @return A {@link List} containing all the actions registered for this reward.
     */
    List<Action> getActions();

}
