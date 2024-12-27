package fr.maxlego08.jobs;

import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.hooks.BlockHook;
import fr.maxlego08.jobs.api.storage.StorageManager;
import fr.maxlego08.jobs.command.commands.CommandJobs;
import fr.maxlego08.jobs.component.PaperComponent;
import fr.maxlego08.jobs.hooks.BlockTrackerHook;
import fr.maxlego08.jobs.hooks.EmptyHook;
import fr.maxlego08.jobs.placeholder.LocalPlaceholder;
import fr.maxlego08.jobs.save.Config;
import fr.maxlego08.jobs.save.MessageLoader;
import fr.maxlego08.jobs.storage.ZStorageManager;
import fr.maxlego08.jobs.zcore.ZPlugin;
import fr.maxlego08.jobs.zcore.utils.plugins.Plugins;
import fr.maxlego08.jobs.zmenu.buttons.JobValueButton;
import fr.maxlego08.jobs.zmenu.loader.AddPointLoader;
import fr.maxlego08.jobs.zmenu.loader.ClaimRewardLoader;
import fr.maxlego08.jobs.zmenu.loader.JobInfoLoader;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.scheduler.ZScheduler;
import fr.maxlego08.menu.button.loader.NoneLoader;
import fr.maxlego08.menu.exceptions.InventoryException;
import fr.traqueur.currencies.Currencies;
import fr.traqueur.currencies.CurrencyProvider;
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
 * System to create your plugins very simply Projet:
 * <a href="https://github.com/Maxlego08/TemplatePlugin">https://github.com/Maxlego08/TemplatePlugin</a>
 *
 * @author Maxlego08
 */
public class ZJobsPlugin extends ZPlugin {

    private final JobManager jobManager = new ZJobManager(this);
    private final StorageManager storageManager = new ZStorageManager(this);
    private final PaperComponent paperComponent = new PaperComponent();
    private final Set<String> knowRewards = new HashSet<>();
    private InventoryManager inventoryManager;
    private ButtonManager buttonManager;
    private BlockHook blockHook = new EmptyHook();
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
        this.buttonManager = getProvider(ButtonManager.class);

        this.loadActions();
        this.loadButtons();

        this.addSave(new MessageLoader(this));
        this.addListener(new JobListener(this));

        this.jobManager.loadJobs();
        Config.getInstance().loadConfiguration(getConfig(), this);
        this.loadFiles();

        this.storageManager.load();

        if (isEnable(Plugins.BLOCKTRACKER)) {
            getLogger().info("Using BlockTracker");
            this.blockHook = new BlockTrackerHook();
        }

        this.loadInventories();
        this.loadCurrencyProvider();

        this.postEnable();
    }

    @Override
    public void onDisable() {

        this.preDisable();

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
        super.reloadFiles();
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public ZScheduler getScheduler() {
        return this.inventoryManager.getScheduler();
    }

    public PaperComponent getPaperComponent() {
        return paperComponent;
    }

    public BlockHook getBlockHook() {
        return blockHook;
    }

    private void loadActions() {
        this.buttonManager.registerAction(new AddPointLoader(this));
        this.buttonManager.registerAction(new ClaimRewardLoader(this));
    }

    private void loadButtons() {
        this.buttonManager.register(new JobInfoLoader(this));
        this.buttonManager.register(new NoneLoader(this, JobValueButton.class, "ZJOBS_VALUES"));
    }

    public void loadInventories() {

        File folder = new File(this.getDataFolder(), "inventories");
        if (!folder.exists()) {
            folder.mkdir();

            saveResource("inventories/jobs.yml", false);
            saveResource("inventories/job_info.yml", false);
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
}
