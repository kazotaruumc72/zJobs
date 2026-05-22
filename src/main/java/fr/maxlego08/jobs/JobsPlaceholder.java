package fr.maxlego08.jobs;

import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import fr.maxlego08.jobs.placeholder.BoostPlaceholder;
import fr.maxlego08.jobs.placeholder.LocalPlaceholder;
import fr.maxlego08.jobs.placeholder.ReturnConsumer;
import fr.maxlego08.jobs.rafine.RafineManager;
import fr.maxlego08.jobs.save.Config;
import fr.maxlego08.jobs.zcore.utils.TagRegistry;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
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

        // %zjobs_baseExperience_<id>% : returns the base experience configured for
        // the given material/tag/nexo id across the player's jobs, and refreshes
        // the progression bossbar of the matching job as a side effect.
        placeholder.register("baseExperience_", (player, rawId) -> this.placeholderBaseExperience(plugin, manager, player, rawId));

        // Refine system
        registerRafinePlaceholders(placeholder, plugin);
    }

    /**
     * Resolves {@code %zjobs_baseExperience_<id>%}.
     * <p>
     * The {@code <id>} part can be:
     * <ul>
     *     <li>a vanilla material name (e.g. {@code DIAMOND_ORE}),</li>
     *     <li>a {@link Tag} name (e.g. {@code LOGS}),</li>
     *     <li>a Nexo block / item id prefixed with {@code nexo:} (e.g. {@code nexo:ruby_ore}).</li>
     * </ul>
     * The lookup walks the player's jobs and returns the experience of the first
     * matching action it finds. As side effects:
     * <ul>
     *     <li>the player is credited with the returned experience on the
     *     matching job (and his progression bossbar is refreshed),</li>
     *     <li>boosts and money rewards configured on the matched action are
     *     applied just like if he had performed the action himself.</li>
     * </ul>
     * <b>Warning:</b> the placeholder grants experience every time it is
     * parsed. Avoid putting it in lores / messages that get re-rendered on a
     * timer — typically use it in zShop {@code sell}/{@code buy} formulas or
     * in one-shot reward actions.
     *
     * @param plugin  plugin instance
     * @param manager job manager
     * @param player  the parsing player
     * @param rawId   the material/tag/nexo identifier appended to the placeholder
     * @return the action experience formatted as a string, or {@code "0"} if no
     * action matches.
     */
    private String placeholderBaseExperience(JobsPlugin plugin, JobManager manager, Player player, String rawId) {
        if (rawId == null || rawId.isEmpty()) return "0";

        var optional = manager.getPlayerJobs(player.getUniqueId());
        if (optional.isEmpty()) return "0";
        PlayerJobs playerJobs = optional.get();

        Object target = resolvePlaceholderTarget(rawId);
        if (target == null) return "0";

        for (PlayerJob playerJob : playerJobs.getJobs()) {
            var jobOptional = manager.getJob(playerJob.getJobId());
            if (jobOptional.isEmpty()) continue;
            Job job = jobOptional.get();

            JobAction<?> matched = findAction(job, target);
            if (matched == null) continue;

            double experience = matched.getExperience(player);
            // Run the full action pipeline: boosts, money, exp gain, bossbar,
            // action bar notification. We dispatch the matched action's own
            // type (e.g. BLOCK_BREAK) with the resolved target so every job
            // that listens for that target gets credited consistently.
            manager.action(player, target, matched.getType());
            return Config.decimalFormat.format(experience);
        }
        return "0";
    }

    /**
     * Convert the textual identifier appended to {@code %zjobs_baseExperience_%}
     * to the concrete object job actions match against.
     * <ul>
     *     <li>{@code nexo:<id>} → the lowercase {@code nexo:<id>} String matched
     *     by {@link fr.maxlego08.jobs.actions.NexoAction}.</li>
     *     <li>otherwise → a {@link Material} if the name resolves to one, or a
     *     {@link Tag} if it matches a registered material tag (used by
     *     {@link fr.maxlego08.jobs.actions.TagAction}).</li>
     * </ul>
     * Returns {@code null} when nothing matches.
     */
    private Object resolvePlaceholderTarget(String rawId) {
        String id = rawId.trim();
        if (id.isEmpty()) return null;

        String lower = id.toLowerCase();
        if (lower.startsWith("nexo:") || lower.startsWith("orestack:")) {
            return lower;
        }

        try {
            return Material.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // not a vanilla material — fall through to tag lookup
        }

        Tag<Material> tag = TagRegistry.getTag(id.toUpperCase());
        return tag;
    }

    /**
     * Find the first {@link JobAction} of the given job that matches the
     * resolved target. Nexo actions (which match against a String) and
     * material / tag actions are all covered.
     */
    private JobAction<?> findAction(Job job, Object target) {
        for (JobAction<?> action : job.getActions()) {
            JobActionType type = action.getType();
            if (type == null || !type.isMaterial()) continue;
            if (action.isAction(target)) return action;
            if (target instanceof Tag<?> tag) {
                // TagAction matches Material — when the placeholder was a Tag we
                // want any MaterialAction whose target is in the tag (and the
                // TagAction itself, which fully equals the registered tag).
                Object actionTarget = action.getTarget();
                if (actionTarget == tag) return action;
                if (actionTarget instanceof Material material && ((Tag<Material>) tag).isTagged(material)) {
                    return action;
                }
            }
        }
        return null;
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
     *   <li>{@code %zjobs_rafine_percent_inverse%} : inverse of the refining percentage ({@code 100 - percent}, {@code 0} when idle), without the {@code %} sign. While a RAFINE action is being processed (e.g. inside an {@code experience-formula}), this resolves to the inverse of the deposit that triggered the action.</li>
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
        placeholder.register("rafine_percent_inverse", (player) -> {
            RafineManager.Deposit d = bestDeposit(plugin, player);
            return d == null ? "0" : String.valueOf(100 - d.getPercent());
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
     * Returns the most relevant deposit for placeholder display.
     * <p>
     * When a RAFINE action is being processed, {@link RafineManager#getActionContext()}
     * returns the deposit that triggered the action; this takes precedence so
     * placeholders used in {@code experience-formula} / {@code money-formula}
     * reflect the just-completed deposit even though it has already been
     * removed from the player's slot.
     * <p>
     * Otherwise the chosen deposit is the one with the shortest remaining
     * time still refining, or a ready one if nothing is refining anymore.
     * Returns {@code null} when the player has no deposit.
     */
    private RafineManager.Deposit bestDeposit(JobsPlugin plugin, Player player) {
        RafineManager.Deposit context = RafineManager.getActionContext();
        if (context != null) return context;
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
