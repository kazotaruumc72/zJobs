package fr.maxlego08.jobs;

import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.boost.BoostManager;
import fr.maxlego08.jobs.api.hooks.BlockHook;
import fr.maxlego08.jobs.api.storage.StorageManager;
import fr.maxlego08.jobs.boost.ZBoostManager;
import fr.maxlego08.jobs.command.commands.CommandJobs;
import fr.maxlego08.jobs.component.PaperComponent;
import fr.maxlego08.jobs.hooks.BlockTrackerHook;
import fr.maxlego08.jobs.hooks.EmptyHook;
import fr.maxlego08.jobs.hooks.MythicMobsHook;
import fr.maxlego08.jobs.hooks.MythicMobsListener;
import fr.maxlego08.jobs.hooks.NexoHook;
import fr.maxlego08.jobs.hooks.NexoListener;
import fr.maxlego08.jobs.placeholder.LocalPlaceholder;
import fr.maxlego08.jobs.forge.ForgeClickListener;
import fr.maxlego08.jobs.forge.ForgeInputButton;
import fr.maxlego08.jobs.forge.ForgeManager;
import fr.maxlego08.jobs.forge.ForgeResultButton;
import fr.maxlego08.jobs.forge.ForgeTimerButton;
import fr.maxlego08.jobs.rafine.RafineClickListener;
import fr.maxlego08.jobs.rafine.RafineInputButton;
import fr.maxlego08.jobs.rafine.RafineManager;
import fr.maxlego08.jobs.rafine.RafineResultButton;
import fr.maxlego08.jobs.rafine.RafineTimerButton;
import fr.maxlego08.jobs.save.Config;
import fr.maxlego08.jobs.save.MessageLoader;
import fr.maxlego08.jobs.storage.ZStorageManager;
import fr.maxlego08.jobs.zcore.ZPlugin;
import fr.maxlego08.jobs.zcore.utils.plugins.Plugins;
import fr.maxlego08.jobs.zmenu.buttons.JobValueButton;
import fr.maxlego08.jobs.zmenu.loader.AddPointLoader;
import fr.maxlego08.jobs.zmenu.loader.ClaimRewardLoader;
import fr.maxlego08.jobs.zmenu.loader.HasLevelLoader;
import fr.maxlego08.jobs.zmenu.loader.HasPointLoader;
import fr.maxlego08.jobs.zmenu.loader.HasPrestigeLoader;
import fr.maxlego08.jobs.zmenu.loader.JobInfoLoader;
import fr.maxlego08.jobs.zmenu.loader.RemovePointLoader;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.exceptions.InventoryException;
import fr.maxlego08.menu.api.loader.NoneLoader;
import fr.maxlego08.menu.api.pattern.PatternManager;
import fr.maxlego08.menu.hooks.currencies.Currencies;
import fr.maxlego08.menu.hooks.currencies.CurrencyProvider;
import fr.maxlego08.menu.hooks.folialib.impl.PlatformScheduler;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * System to create your plugins very simple Projet:
 * <a href="https://github.com/Maxlego08/TemplatePlugin">https://github.com/Maxlego08/TemplatePlugin</a>
 *
 * @author Maxlego08
 */
public class JobsPlugin extends ZPlugin {

    private final JobManager jobManager = new ZJobManager(this);
    private final BoostManager boostManager = new ZBoostManager(this);
    private final StorageManager storageManager = new ZStorageManager(this);
    private final PaperComponent paperComponent = new PaperComponent();
    private final RafineManager rafineManager = new RafineManager(this);
    private final ForgeManager forgeManager = new ForgeManager(this);
    private final Set<String> knowRewards = new HashSet<>();
    private PatternManager patternManager;
    private InventoryManager inventoryManager;
    private ButtonManager buttonManager;
    private BlockHook blockHook = new EmptyHook();
    private NexoHook nexoHook;
    private MythicMobsHook mythicMobsHook;
    private CurrencyProvider currencyProvider;

    @Override
    public void onEnable() {

        LocalPlaceholder placeholder = LocalPlaceholder.getInstance();
        placeholder.setPrefix("zjobs");

        this.preEnable();
        this.saveDefaultConfig();

        var servicesManager = getServer().getServicesManager();
        servicesManager.register(JobManager.class, this.jobManager, this, ServicePriority.Highest);

        this.registerCommand("zjobs", new CommandJobs(this), "jobs");

        this.inventoryManager = getProvider(InventoryManager.class);
        this.patternManager = getProvider(PatternManager.class);
        this.buttonManager = getProvider(ButtonManager.class);

        this.loadActions();
        this.loadButtons();

        this.addSave(new MessageLoader(this));
        this.addListener(new JobListener(this));
        this.addListener(new RafineClickListener(this));
        this.addListener(new ForgeClickListener(this));

        this.jobManager.loadJobs();
        Config.getInstance().loadConfiguration(getConfig(), this);
        this.loadFiles();

        this.storageManager.load();

        this.rafineManager.load();
        this.rafineManager.startCompletionWatcher();

        this.forgeManager.load();
        this.forgeManager.startCompletionWatcher();
        this.registerForgePlaceholders(placeholder);

        if (isEnable(Plugins.BLOCKTRACKER)) {
            getLogger().info("Using BlockTracker");
            this.blockHook = new BlockTrackerHook();
        }

        if (isEnable(Plugins.NEXO)) {
            getLogger().info("Using Nexo");
            this.nexoHook = new NexoHook();
            this.addListener(new NexoListener(this));
        }

        if (isEnable(Plugins.MYTHICMOBS)) {
            getLogger().info("Using MythicMobs");
            this.mythicMobsHook = new MythicMobsHook();
            this.addListener(new MythicMobsListener(this));
        }

        this.loadInventories();
        this.loadCurrencyProvider();

        this.postEnable();
    }

    @Override
    public void onDisable() {

        this.preDisable();

        this.rafineManager.stopCompletionWatcher();
        this.forgeManager.stopCompletionWatcher();
        this.storageManager.onDisable();
        this.saveFiles();

        this.postDisable();
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public ButtonManager getButtonManager() {
        return buttonManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    @Override
    public void reloadFiles() {
        this.reloadConfig();
        this.knowRewards.clear();
        this.jobManager.loadJobs();
        Config.getInstance().loadConfiguration(getConfig(), this);
        this.loadInventories();
        this.rafineManager.load();
        this.forgeManager.load();
        super.reloadFiles();
    }

    /**
     * Register the PlaceholderAPI placeholders used by the FORGE feature YAML.
     */
    private void registerForgePlaceholders(LocalPlaceholder placeholder) {
        placeholder.register("forge_time", (player) -> {
            ForgeManager.Session session = this.forgeManager.getSession(player);
            if (session == null || !session.hasTimer()) return "--:--";
            if (session.isReady()) return "0:00";
            return ForgeManager.formatTime(session.getRemainingSeconds());
        });
        placeholder.register("forge_status", (player) -> this.forgeManager.getStatus(player));
        placeholder.register("forge_percent", (player) -> {
            ForgeManager.Session session = this.forgeManager.getSession(player);
            if (session == null || session.getRecipe() == null) return "0";
            return String.valueOf(session.getRecipe().getFailPercent());
        });
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PlatformScheduler getScheduler() {
        return this.inventoryManager.getScheduler();
    }

    public PaperComponent getPaperComponent() {
        return paperComponent;
    }

    public BlockHook getBlockHook() {
        return blockHook;
    }

    public NexoHook getNexoHook() {
        return nexoHook;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    private void loadActions() {
        this.buttonManager.registerAction(new AddPointLoader(this));
        this.buttonManager.registerAction(new RemovePointLoader(this));
        this.buttonManager.registerAction(new ClaimRewardLoader(this));
        this.buttonManager.registerPermissible(new HasPointLoader(this));
        this.buttonManager.registerPermissible(new HasPrestigeLoader(this));
        this.buttonManager.registerPermissible(new HasLevelLoader(this));
    }

    private void loadButtons() {
        this.buttonManager.register(new JobInfoLoader(this));
        this.buttonManager.register(new NoneLoader(this, JobValueButton.class, "ZJOBS_VALUES"));
        this.buttonManager.register(new NoneLoader(this, RafineInputButton.class, "ZJOBS_ITEM_RAFINE"));
        this.buttonManager.register(new NoneLoader(this, RafineResultButton.class, "ZJOBS_ITEM_RAFINE_RESULT"));
        this.buttonManager.register(new NoneLoader(this, RafineTimerButton.class, "ZJOBS_ITEM_RAFINE_TIMER"));
        this.buttonManager.register(new NoneLoader(this, ForgeInputButton.class, "ZJOBS_ITEM_FORGE"));
        this.buttonManager.register(new NoneLoader(this, ForgeResultButton.class, "ZJOBS_ITEM_FORGE_RESULT"));
        this.buttonManager.register(new NoneLoader(this, ForgeTimerButton.class, "ZJOBS_ITEM_FORGE_TIMER"));
    }

    public void loadInventories() {

        this.loadPatterns();

        File folder = new File(this.getDataFolder(), "inventories");
        if (!folder.exists()) {
            folder.mkdir();

            saveResource("inventories/jobs.yml", false);
            saveResource("inventories/job_info.yml", false);
        }

        // Always ensure rafine.yml exists (added after initial release)
        File rafineFile = new File(folder, "rafine.yml");
        if (!rafineFile.exists()) {
            saveResource("inventories/rafine.yml", false);
        }

        // Always ensure the default forge inventory exists
        File forgeSwordsFile = new File(folder, "forge/weapons/swords.yml");
        if (!forgeSwordsFile.exists()) {
            forgeSwordsFile.getParentFile().mkdirs();
            saveResource("inventories/forge/weapons/swords.yml", false);
        }

        this.inventoryManager.deleteInventories(this);
        this.files(folder, file -> {
            try {
                this.inventoryManager.loadInventory(this, file);
            } catch (InventoryException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void loadPatterns() {

        File folder = new File(this.getDataFolder(), "patterns");
        if (!folder.exists()) {
            folder.mkdir();
        }

        this.files(folder, file -> {
            try {
                this.patternManager.loadPattern(file);
            } catch (InventoryException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void loadCurrencyProvider() {
        Currencies currencies = Currencies.valueOf(getConfig().getString("default-economy", Currencies.VAULT.name()));
        switch (currencies) {
            case ZESSENTIALS, ECOBITS, COINSENGINE, REDISECONOMY -> {
                this.currencyProvider = currencies.createProvider(getConfig().getString("currency-name", "money"));
            }
            default -> this.currencyProvider = currencies.createProvider();
        }
    }

    private void files(File folder, Consumer<File> consumer) {
        try (Stream<Path> s = Files.walk(Paths.get(folder.getPath()))) {
            s.skip(1).map(Path::toFile).filter(File::isFile).filter(e -> e.getName().endsWith(".yml")).forEach(consumer);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public Set<String> getKnowRewards() {
        return knowRewards;
    }

    public CurrencyProvider getCurrencyProvider() {
        return currencyProvider;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    public RafineManager getRafineManager() {
        return rafineManager;
    }

    public ForgeManager getForgeManager() {
        return forgeManager;
    }
}
