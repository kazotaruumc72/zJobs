package fr.maxlego08.jobs.storage;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.Tables;
import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.api.players.PlayerJob;
import fr.maxlego08.jobs.api.players.PlayerJobs;
import fr.maxlego08.jobs.api.storage.StorageManager;
import fr.maxlego08.jobs.api.storage.StorageType;
import fr.maxlego08.jobs.boost.ZBoost;
import fr.maxlego08.jobs.dto.PlayerJobDTO;
import fr.maxlego08.jobs.dto.PlayerPointsDTO;
import fr.maxlego08.jobs.dto.PlayerRewardDTO;
import fr.maxlego08.jobs.migrations.CreateJobPlayerMigration;
import fr.maxlego08.jobs.migrations.CreatePlayerBoostMigration;
import fr.maxlego08.jobs.migrations.CreatePlayerPointsMigration;
import fr.maxlego08.jobs.migrations.CreatePlayerRewardMigration;
import fr.maxlego08.jobs.players.ZPlayerJob;
import fr.maxlego08.jobs.players.ZPlayerJobs;
import fr.maxlego08.jobs.zcore.utils.GlobalDatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.HikariDatabaseConnection;
import fr.maxlego08.sarah.MigrationManager;
import fr.maxlego08.sarah.MySqlConnection;
import fr.maxlego08.sarah.RequestHelper;
import fr.maxlego08.sarah.SqliteConnection;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.logger.JULogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZStorageManager implements StorageManager {

    private final JobsPlugin plugin;
    private final Map<UUID, Map<String, PlayerJob>> pendingUpdates = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> lastUpdateTime = new ConcurrentHashMap<>();
    private RequestHelper requestHelper;

    public ZStorageManager(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {

        FileConfiguration configuration = plugin.getConfig();
        StorageType storageType = StorageType.valueOf(configuration.getString("storage-type", "SQLITE").toUpperCase());

        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(configuration, storageType);
        DatabaseConnection connection = switch (storageType) {
            case MYSQL -> new MySqlConnection(databaseConfiguration);
            case SQLITE -> new SqliteConnection(databaseConfiguration, this.plugin.getDataFolder());
            case HIKARICP -> new HikariDatabaseConnection(databaseConfiguration);
        };
        this.requestHelper = new RequestHelper(connection, JULogger.from(plugin.getLogger()));

        if (!connection.isValid()) {
            plugin.getLogger().severe("Unable to connect to database!");
            Bukkit.getPluginManager().disablePlugin(plugin);
        } else {
            if (storageType == StorageType.SQLITE) {
                plugin.getLogger().info("The database connection is valid! (SQLITE)");
            } else {
                plugin.getLogger().info("The database connection is valid! (" + connection.getDatabaseConfiguration().getHost() + ")");
            }
        }

        MigrationManager.setMigrationTableName("zjobs_migrations");
        MigrationManager.setDatabaseConfiguration(databaseConfiguration);

        MigrationManager.registerMigration(new CreateJobPlayerMigration());
        MigrationManager.registerMigration(new CreatePlayerPointsMigration());
        MigrationManager.registerMigration(new CreatePlayerRewardMigration());
        MigrationManager.registerMigration(new CreatePlayerBoostMigration());

        MigrationManager.execute(connection, JULogger.from(this.plugin.getLogger()));

        startUpdateTask(configuration.getLong("update-jobs-ticks", 200));
    }

    private DatabaseConfiguration getDatabaseConfiguration(FileConfiguration configuration, StorageType storageType) {
        GlobalDatabaseConfiguration globalDatabaseConfiguration = new GlobalDatabaseConfiguration(configuration);
        String tablePrefix = globalDatabaseConfiguration.getTablePrefix();
        String host = globalDatabaseConfiguration.getHost();
        int port = globalDatabaseConfiguration.getPort();
        String user = globalDatabaseConfiguration.getUser();
        String password = globalDatabaseConfiguration.getPassword();
        String database = globalDatabaseConfiguration.getDatabase();
        boolean debug = globalDatabaseConfiguration.isDebug();

        return new DatabaseConfiguration(tablePrefix, user, password, port, host, database, debug, storageType == StorageType.SQLITE ? DatabaseType.SQLITE : DatabaseType.MYSQL);
    }

    @Override
    public PlayerJobs loadPlayerJobs(UUID uniqueId) {
        List<PlayerJobDTO> playerJobDTOS = this.requestHelper.select(Tables.JOBS, PlayerJobDTO.class, table -> table.where("unique_id", uniqueId));

        long points = getPoints(uniqueId);
        Set<String> integers = getRewards(uniqueId);

        return new ZPlayerJobs(this.plugin, uniqueId, playerJobDTOS.stream().map(ZPlayerJob::new).collect(Collectors.toList()), points, integers);
    }

    @Override
    public void upsert(UUID uniqueId, PlayerJob playerJob, boolean force) {

        if (force) {
            executeUpsert(uniqueId, playerJob);
            return;
        }

        long currentTime = System.currentTimeMillis();

        Map<String, Long> jobLastUpdateTimes = lastUpdateTime.computeIfAbsent(uniqueId, k -> new ConcurrentHashMap<>());
        long lastTime = jobLastUpdateTimes.getOrDefault(playerJob.getJobId(), 0L);

        if (currentTime - lastTime < 5000) {
            Map<String, PlayerJob> jobs = pendingUpdates.computeIfAbsent(uniqueId, k -> new ConcurrentHashMap<>());
            jobs.put(playerJob.getJobId(), playerJob);
            return;
        }

        executeUpsert(uniqueId, playerJob);
        jobLastUpdateTimes.put(playerJob.getJobId(), currentTime);
    }

    @Override
    public void upsert(UUID uniqueId, long points) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> {
            this.requestHelper.upsert(Tables.POINTS, table -> {
                table.uuid("unique_id", uniqueId).primary();
                table.bigInt("points", points);
            });
        });
    }

    @Override
    public void upsert(UUID uniqueId, Set<String> rewards) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> {
            this.requestHelper.upsert(Tables.REWARDS, table -> {
                table.uuid("unique_id", uniqueId).primary();
                table.string("content", rewards.stream().map(String::valueOf).collect(Collectors.joining(",")));
            });
        });
    }

    private void executeUpsert(UUID uniqueId, PlayerJob playerJob) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> {
            this.requestHelper.upsert(Tables.JOBS, table -> {
                table.uuid("unique_id", uniqueId).primary();
                table.string("job_id", playerJob.getJobId()).primary();
                table.bigInt("level", playerJob.getLevel());
                table.bigInt("prestige", playerJob.getPrestige());
                table.decimal("experience", playerJob.getExperience());
            });
        });
    }

    private void startUpdateTask(long ticks) {
        this.plugin.getScheduler().runTaskTimerAsynchronously(ticks, ticks, this::update);
    }

    private void update() {
        this.plugin.getJobManager().updateJobEconomies();

        long currentTime = System.currentTimeMillis();

        this.pendingUpdates.forEach((uniqueId, jobsMap) -> {

            Map<String, Long> jobLastUpdateTimes = lastUpdateTime.getOrDefault(uniqueId, new ConcurrentHashMap<>());
            Iterator<Map.Entry<String, PlayerJob>> iterator = jobsMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, PlayerJob> entry = iterator.next();
                String jobId = entry.getKey();
                PlayerJob playerJob = entry.getValue();

                long lastTime = jobLastUpdateTimes.getOrDefault(jobId, 0L);

                if (currentTime - lastTime >= 5000) {
                    executeUpsert(uniqueId, playerJob);
                    jobLastUpdateTimes.put(jobId, currentTime);
                    iterator.remove();
                }
            }
        });
    }

    @Override
    public void deleteJob(UUID uniqueId, String jobId) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> {
            this.requestHelper.delete(Tables.JOBS, table -> {
                table.where("unique_id", uniqueId).primary();
                table.where("job_id", jobId);
            });

            Map<String, PlayerJob> jobs = this.pendingUpdates.get(uniqueId);
            if (jobs != null) {
                jobs.remove(jobId);
            }
        });
    }

    @Override
    public long getPoints(UUID uniqueId) {
        List<PlayerPointsDTO> playerPointsDTOS = this.requestHelper.select(Tables.POINTS, PlayerPointsDTO.class, table -> table.where("unique_id", uniqueId));
        return playerPointsDTOS.isEmpty() ? 0 : playerPointsDTOS.get(0).points();
    }

    public RequestHelper getRequestHelper() {
        return requestHelper;
    }

    @Override
    public void onDisable() {
        this.update();
    }

    @Override
    public Set<String> getRewards(UUID uniqueId) {
        List<PlayerRewardDTO> playerRewardDTOS = this.requestHelper.select(Tables.REWARDS, PlayerRewardDTO.class, table -> table.where("unique_id", uniqueId));
        var reward = playerRewardDTOS.isEmpty() ? "" : playerRewardDTOS.getFirst().content();
        return reward.isEmpty() ? new HashSet<>() : Arrays.stream(reward.split(",")).collect(Collectors.toSet());
    }

    @Override
    public void createBoost(@NotNull UUID uniqueId, @Nullable String jobName, @Nullable JobActionType actionType, double experienceBoost, double moneyBoost, int amount, Consumer<Boost> consumer, Runnable errorRunnable) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> this.requestHelper.insert(Tables.BOOSTS, table -> {
            table.uuid("unique_id", uniqueId).primary();
            if (jobName != null) table.string("job_id", jobName);
            if (actionType != null) table.string("action_type", actionType.name());
            table.bigInt("remaining_boost", amount);
            table.decimal("experience_boost", experienceBoost);
            table.decimal("money_boost", moneyBoost);
        }, id -> consumer.accept(new ZBoost(id, jobName, actionType, experienceBoost, moneyBoost, amount))));
    }
}