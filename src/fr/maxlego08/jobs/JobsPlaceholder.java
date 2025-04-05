package fr.maxlego08.jobs;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import fr.maxlego08.jobs.placeholder.LocalPlaceholder;
import fr.maxlego08.jobs.placeholder.ReturnConsumer;
import fr.maxlego08.jobs.save.Config;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.stream.Collectors;

public class JobsPlaceholder extends ZUtils {

    public void register(JobsPlugin plugin, JobManager manager) {
        LocalPlaceholder placeholder = LocalPlaceholder.getInstance();

        // Player
        placeholder.register("level_", (player, jobId) -> {
            var optional = manager.getPlayerJobs(player.getUniqueId());
            if (optional.isEmpty()) return "0";
            var playerJobs = optional.get();
            return playerJobs.get(jobId).map(PlayerJob::getLevel).orElse(0).toString();
        });
        placeholder.register("points", (player) -> {
            var optional = manager.getPlayerJobs(player.getUniqueId());
            return optional.map(PlayerJobs::getPoints).orElse(0L).toString();
        });

        placeholder.register("reward_is_claim_", (player, rewardId) -> {
            try {
                var optional = manager.getPlayerJobs(player.getUniqueId());
                return optional.map(playerJobs -> playerJobs.getRewards().contains(rewardId)).orElse(false).toString();
            } catch (Exception exception) {
                return "Reward " + rewardId + " is not an integer !";
            }
        });

        // Jobs
        placeholder.register("max_level_", (player, jobId) -> {
            var optional = manager.getJob(jobId);
            return optional.map(Job::getMaxLevels).orElse(0).toString();
        });

        // Target
        placeholder.register("current_jobs_model_data", (player) -> {
            var job = manager.getTargetJob(player);
            return job == null ? "0" : String.valueOf(job.getCustomModelData());
        });

        // Boosts
        placeholder.register("boosts", this.placeholderBoosts(manager));
    }

    private ReturnConsumer<Player, String> placeholderBoosts(JobManager manager) {
        return player -> {

            var config = Config.boostPlaceholderConfig;
            var optional = manager.getPlayerJobs(player.getUniqueId());
            if (optional.isEmpty()) {
                return config.empty();
            }

            var boosts = optional.get().getBoosts();
            if (boosts.getBoosts().isEmpty()) {
                return config.empty();
            }

            return boosts.getBoosts().stream().map(boost -> {

                Placeholders placeholders = new Placeholders();
                placeholders.register("boost-jobs", boost.getJobs().isEmpty() ? config.everyJobs() : boost.getJobs().stream().map(manager::getJob).filter(Optional::isPresent).map(Optional::get).map(Job::getName).collect(Collectors.joining(", ")));
                placeholders.register("boost-actions", boost.getActions().isEmpty() ? config.everyActions() : boost.getActions().stream().map(JobActionType::name).collect(Collectors.joining(", ")));
                placeholders.register("boost-targets", boost.getTargets().isEmpty() ? config.everyTargets() : String.join(", ", boost.getTargets()));
                placeholders.register("boost-remaining", format(boost.getRemainingBoost()));
                placeholders.register("boost-amount", format(boost.getBoostAmount()));
                placeholders.register("boost-experience", format(boost.getExperienceBoost()));
                placeholders.register("boost-money", format(boost.getMoneyBoost()));
                placeholders.register("boost-id", String.valueOf(boost.getId()));

                return placeholders.parse(config.result());
            }).collect(Collectors.joining(config.between()));
        };
    }

}
