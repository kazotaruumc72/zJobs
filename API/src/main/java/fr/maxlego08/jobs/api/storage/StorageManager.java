package fr.maxlego08.jobs.api.storage;

import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface StorageManager {

    void load();

    PlayerJobs loadPlayerJobs(UUID uniqueId);

    void upsert(UUID uniqueId, PlayerJob playerJob, boolean force);

    void update(UUID uniqueId, Boost boost, boolean force);

    void upsert(UUID uniqueId, long points);

    void upsert(UUID uniqueId, Set<String> rewards);

    void deleteJob(UUID uniqueId, String fileName);

    void onDisable();

    long getPoints(UUID uniqueId);

    Set<String> getRewards(UUID uniqueId);

    List<Boost> getBoosts(UUID uniqueId);

    void createBoost(@NotNull UUID uniqueId, List<String> jobs, List<JobActionType> actions, List<String> targets, double moneyBoost, double experienceBoost, int amount, Consumer<Boost> consumer, Runnable errorRunnable);

    void deleteBoost(@NotNull UUID uniqueId, int boostId);

    void insertBoostLog(UUID uniqueId, Boost boost);
}
