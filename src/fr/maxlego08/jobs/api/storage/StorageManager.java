package fr.maxlego08.jobs.api.storage;

import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface StorageManager {

    void load();

    PlayerJobs loadPlayerJobs(UUID uniqueId);

    void upsert(UUID uniqueId, PlayerJob playerJob, boolean force);

    void upsert(UUID uniqueId, long points);

    void upsert(UUID uniqueId, Set<String> rewards);

    void deleteJob(UUID uniqueId, String fileName);

    void onDisable();

    long getPoints(UUID uniqueId);

    Set<String> getRewards(UUID uniqueId);

    void createBoost(@NotNull UUID uniqueId, @Nullable String jobName, @Nullable JobActionType actionType, double experienceBoost, double moneyBoost, int amount, Consumer<Boost> consumer, Runnable errorRunnable);
}
