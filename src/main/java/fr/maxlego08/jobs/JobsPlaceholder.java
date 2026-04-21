package fr.maxlego08.jobs;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import fr.maxlego08.jobs.placeholder.BoostPlaceholder;
import fr.maxlego08.jobs.placeholder.LocalPlaceholder;
import fr.maxlego08.jobs.placeholder.ReturnConsumer;
import fr.maxlego08.jobs.rafine.RafineManager;
import fr.maxlego08.jobs.save.Config;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class JobsPlaceholder extends ZUtils {

    private final BoostPlaceholder boostPlaceholder = new BoostPlaceholder();

    public void register(JobsPlugin plugin, JobManager manager) {
        LocalPlaceholder placeholder = LocalPlaceholder.getInstance();

        // Player
        placeholder.register("has_", (player, jobId) -> manager.getPlayerJobs(player.getUniqueId()).map(playerJobs -> String.valueOf(playerJobs.get(jobId).isPresent())).orElse("false"));
        placeholder.register("level_", (player, jobId) -> {
            var optional = manager.getPlayerJobs(player.getUniqueId());
            if (optional.isEmpty()) return "0";
            var playerJobs = optional.get();
            return playerJobs.get(jobId).map(PlayerJob::getLevel).orElse(0).toString();
        });
        placeholder.register("prestige_", (player, jobId) -> {
            var optional = manager.getPlayerJobs(player.getUniqueId());
            if (optional.isEmpty()) return "0";
            var playerJobs = optional.get();
            return playerJobs.get(jobId).map(PlayerJob::getPrestige).orElse(0).toString();
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

        // Refine system
        registerRafinePlaceholders(placeholder, plugin);
    }

    /**
     * Registers all placeholders related to the refining system. Every one of
     * them reflects live state and is therefore automatically updated each
     * time PlaceholderAPI (or the menu framework) re-parses them.
     *
     * <ul>
     *   <li>{@code %zjobs_rafine_status%} : {@code idle} / {@code refining} / {@code ready}</li>
     *   <li>{@code %zjobs_rafine_time%} : shortest remaining time formatted as {@code m:ss} (empty when idle)</li>
     *   <li>{@code %zjobs_rafine_seconds%} : shortest remaining seconds as an integer ({@code 0} when idle)</li>
     *   <li>{@code %zjobs_rafine_percent%} : refining percentage of the best deposit ({@code 0} when idle)</li>
     *   <li>{@code %zjobs_rafine_count%} : number of items currently being refined</li>
     *   <li>{@code %zjobs_rafine_ready%} : {@code true} when at least one deposit is ready, {@code false} otherwise</li>
     * </ul>
     */
    private void registerRafinePlaceholders(LocalPlaceholder placeholder, JobsPlugin plugin) {
        placeholder.register("rafine_status", (player) -> {
            RafineManager.Deposit d = bestDeposit(plugin, player);
            if (d == null) return "idle";
            return d.isReady() ? "ready" : "refining";
        });
        placeholder.register("rafine_time", (player) -> {
            RafineManager.Deposit d = bestDeposit(plugin, player);
            if (d == null) return "";
            if (d.isReady()) return "0:00";
            return RafineManager.formatTime(d.getRemainingSeconds());
        });
        placeholder.register("rafine_seconds", (player) -> {
            RafineManager.Deposit d = bestDeposit(plugin, player);
            if (d == null || d.isReady()) return "0";
            return String.valueOf(d.getRemainingSeconds());
        });
        placeholder.register("rafine_percent", (player) -> {
            RafineManager.Deposit d = bestDeposit(plugin, player);
            return d == null ? "0" : String.valueOf(d.getPercent());
        });
        placeholder.register("rafine_count", (player) -> String.valueOf(plugin.getRafineManager().getDeposits(player).size()));
        placeholder.register("rafine_ready", (player) -> {
            for (RafineManager.Deposit d : plugin.getRafineManager().getDeposits(player).values()) {
                if (d.isReady()) return "true";
            }
            return "false";
        });
    }

    /**
     * Returns the most relevant deposit for placeholder display: the one with
     * the shortest remaining time still refining, or a ready one if nothing
     * is refining anymore. {@code null} when the player has no deposit.
     */
    private RafineManager.Deposit bestDeposit(JobsPlugin plugin, Player player) {
        RafineManager.Deposit shortest = null;
        RafineManager.Deposit ready = null;
        for (RafineManager.Deposit d : plugin.getRafineManager().getDeposits(player).values()) {
            if (d.isRefining()) {
                if (shortest == null || d.getRemainingSeconds() < shortest.getRemainingSeconds()) {
                    shortest = d;
                }
            } else if (d.isReady() && ready == null) {
                ready = d;
            }
        }
        return shortest != null ? shortest : ready;
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

            return boosts.getBoosts().stream().map(boost -> this.boostPlaceholder.getPlaceholders(boost, manager).parse(config.result())).collect(Collectors.joining(config.between()));
        };
    }

}
