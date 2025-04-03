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
import fr.maxlego08.jobs.boost.ZPlayerBoosts;
import fr.maxlego08.jobs.dto.PlayerBoostDTO;
import fr.maxlego08.jobs.dto.PlayerJobDTO;
import fr.maxlego08.jobs.dto.PlayerPointsDTO;
import fr.maxlego08.jobs.dto.PlayerRewardDTO;
import fr.maxlego08.jobs.migrations.CreateJobPlayerMigration;
import fr.maxlego08.jobs.migrations.CreatePlayerBoostLogMigration;
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
import fr.maxlego08.sarah.SchemaBuilder;
import fr.maxlego08.sarah.SqliteConnection;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.logger.JULogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZStorageManager implements StorageManager {

    private final JobsPlugin plugin;
    private final Map<UUID, PendingUpdate> pendingUpdates = new ConcurrentHashMap<>();
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
        MigrationManager.registerMigration(new CreatePlayerBoostLogMigration());

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

        var points = getPoints(uniqueId);
        var rewards = getRewards(uniqueId);
        var boosts = getBoosts(uniqueId);
        var playerBoosts = new ZPlayerBoosts(this.plugin, boosts);

        return new ZPlayerJobs(this.plugin, uniqueId, playerJobDTOS.stream().map(ZPlayerJob::new).collect(Collectors.toList()), points, rewards, playerBoosts);
    }

    @Override
    public void upsert(UUID uniqueId, PlayerJob playerJob, boolean force) {

        if (force) {
            executeUpsert(uniqueId, playerJob);
            return;
        }

        var pendingUpdate = pendingUpdates.computeIfAbsent(uniqueId, k -> new PendingUpdate());
        pendingUpdate.setJob(playerJob.getJobId(), playerJob);
    }

    public void update(UUID uniqueId, Boost boost, boolean force) {
        if (force) {
            this.requestHelper.update(Tables.BOOSTS, table -> {
                table.where("id", boost.getId());
                table.bigInt("remaining_boost", boost.getRemainingBoost());
            });
            return;
        }

        var pendingUpdate = pendingUpdates.computeIfAbsent(uniqueId, k -> new PendingUpdate());
        pendingUpdate.getBoosts().add(boost);
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
        this.plugin.getScheduler().runTaskAsynchronously(() -> this.requestHelper.upsert(Tables.JOBS, this.toPlayerJobTable(uniqueId, playerJob)));
    }

    private Consumer<Schema> toPlayerJobTable(UUID uniqueId, PlayerJob playerJob) {
        return table -> {
            table.uuid("unique_id", uniqueId).primary();
            table.string("job_id", playerJob.getJobId()).primary();
            table.bigInt("level", playerJob.getLevel());
            table.bigInt("prestige", playerJob.getPrestige());
            table.decimal("experience", playerJob.getExperience());
        };
    }

    private void startUpdateTask(long ticks) {
        this.plugin.getScheduler().runTaskTimerAsynchronously(ticks, ticks, this::update);
    }

    private void update() {
        this.plugin.getJobManager().updateJobEconomies();

        List<Schema> updateJobs = new ArrayList<>();
        List<Schema> updateBoots = new ArrayList<>();

        this.pendingUpdates.forEach((uniqueId, pendingUpdate) -> {
            pendingUpdate.getJobs().values().forEach(playerJob -> updateJobs.add(SchemaBuilder.upsert(Tables.JOBS, this.toPlayerJobTable(uniqueId, playerJob))));
            pendingUpdate.getBoosts().forEach(boost -> updateBoots.add(SchemaBuilder.update(Tables.BOOSTS, table -> {
                table.bigInt("remaining_boost", boost.getRemainingBoost());
                table.where("id", boost.getId());
            })));
        });
        this.pendingUpdates.clear();

        this.plugin.getScheduler().runTaskAsynchronously(() -> {
            this.requestHelper.upsertMultiple(updateJobs);
            this.requestHelper.updateMultiple(updateBoots);
        });
    }

    @Override
    public void deleteJob(UUID uniqueId, String jobId) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> {

            this.requestHelper.delete(Tables.JOBS, table -> {
                table.where("unique_id", uniqueId).primary();
                table.where("job_id", jobId);
            });

            var pendingUpdate = this.pendingUpdates.get(uniqueId);
            if (pendingUpdate != null) {
                pendingUpdate.remove(jobId);
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
    public List<Boost> getBoosts(UUID uniqueId) {
        return this.requestHelper.select(Tables.BOOSTS, PlayerBoostDTO.class, table -> table.where("unique_id", uniqueId).where("remaining_boost", ">", 0)).stream().map(ZBoost::new).collect(Collectors.toList());
    }

    @Override
    public void createBoost(@NotNull UUID uniqueId, List<String> jobs, List<JobActionType> actions, List<String> targets, double moneyBoost, double experienceBoost, int amount, Consumer<Boost> consumer, Runnable errorRunnable) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> this.requestHelper.insert(Tables.BOOSTS, table -> {
            table.uuid("unique_id", uniqueId).primary();
            if (!jobs.isEmpty()) table.string("jobs", String.join(",", jobs));
            if (!actions.isEmpty()) {
                table.string("actions", actions.stream().map(JobActionType::name).collect(Collectors.joining(",")));
            }
            if (!targets.isEmpty()) table.string("targets", String.join(",", targets));
            table.bigInt("boost_amount", amount);
            table.bigInt("remaining_boost", amount);
            table.decimal("experience_boost", experienceBoost);
            table.decimal("money_boost", moneyBoost);
        }, id -> consumer.accept(new ZBoost(id, jobs, actions, targets, amount, experienceBoost, moneyBoost)), errorRunnable));
    }

    @Override
    public void deleteBoost(@NotNull UUID uniqueId, int boostId) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> this.requestHelper.delete(Tables.BOOSTS, table -> table.where("id", boostId)));
    }

    @Override
    public void insertBoostLog(UUID uniqueId, Boost boost) {
        this.plugin.getScheduler().runTaskAsynchronously(() -> this.requestHelper.insert(Tables.BOOST_LOGS, table -> {
            table.uuid("unique_id", uniqueId).primary();
            if (!boost.getJobs().isEmpty()) table.string("jobs", String.join(",", boost.getJobs()));
            if (!boost.getActions().isEmpty()) {
                table.string("actions", boost.getActions().stream().map(JobActionType::name).collect(Collectors.joining(",")));
            }
            if (!boost.getTargets().isEmpty()) table.string("targets", String.join(",", boost.getTargets()));
            table.bigInt("boost_amount", boost.getBoostAmount());
            table.decimal("experience_boost", boost.getExperienceBoost());
            table.decimal("money_boost", boost.getMoneyBoost());
            table.object("started_at", boost.getCreatedAt());
            table.object("finished_at", new Date());
        }));
    }
}