package fr.maxlego08.jobs;

import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.api.enums.JobActionType;
import fr.maxlego08.jobs.hooks.NexoHook;
import fr.maxlego08.jobs.save.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JobListener implements Listener {

    private final JobsPlugin plugin;
    private final JobManager jobManager;
    private final NamespacedKey playerKey;
    // Caches smithing table contents per player from PrepareSmithingEvent.
    // Index 0 = result, 1-3 = input slots (template, base, addition).
    // Used as fallback when Nexo clears the inventory before our MONITOR handler runs.
    private final Map<UUID, ItemStack[]> smithingCache = new HashMap<>();

    public JobListener(JobsPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.playerKey = new NamespacedKey(plugin, "player-uuid");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.jobManager.loadPlayerJobs(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.jobManager.playerQuit(event.getPlayer());
        this.smithingCache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Material material = block.getType();

        if (!(block.getBlockData() instanceof Ageable)) {

            if (this.plugin.getBlockHook().isTracked(block)) return;

            NexoHook nexoHook = this.plugin.getNexoHook();
            if (nexoHook != null && nexoHook.isNexoBlock(block)) {
                return;
            }

            this.jobManager.action(player, material, JobActionType.BLOCK_BREAK);

        } else if (block.getBlockData() instanceof Ageable ageable && ((material == Material.SUGAR_CANE || material == Material.KELP || material == Material.BAMBOO) || ageable.getAge() == ageable.getMaximumAge())) {

            NexoHook nexoHook = this.plugin.getNexoHook();
            if (nexoHook != null && nexoHook.isNexoBlock(block)) {
                return;
            }

            if (Config.forceBlockCheck.contains(material)) {

                if (this.plugin.getBlockHook().isTracked(block)) return;
            }

            this.jobManager.action(player, material, JobActionType.FARMING);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Material material = block.getType();

        NexoHook nexoHook = this.plugin.getNexoHook();
        if (nexoHook != null && nexoHook.isNexoBlock(block)) {
            return;
        }

        this.jobManager.action(player, material, JobActionType.BLOCK_PLACE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {

        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item item) {

            ItemStack itemStack = item.getItemStack();
            this.jobManager.action(player, itemStack.getType(), JobActionType.FISHING);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimalTame(EntityTameEvent event) {

        LivingEntity animal = event.getEntity();

        if (animal.isDead()) return;

        if (event.getOwner() instanceof Player player && player.isOnline()) {
            this.jobManager.action(player, animal.getType(), JobActionType.TAME);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();

        // Defer ALL entity access to the next tick to prevent packet encoding errors.
        // Plugins like MythicMobs/ModelEngine modify entity metadata during EntityDeathEvent,
        // and ANY entity method call (including getKiller()) can trigger entity metadata updates
        // and packet sends through the Netty pipeline, causing NullPointerException
        // in ByteBufCodecs when encoding set_entity_data packets because the entity is in an
        // invalid state during cleanup. By deferring to next tick, we ensure all plugins have
        // finished their entity cleanup before we access any entity data.
        EntityType entityType = entity.getType();
        UUID entityUuid = entity.getUniqueId();

        this.plugin.getScheduler().runNextTick(w -> {
            Player killer = entity.getKiller();
            if (killer == null) return;

            this.jobManager.action(killer, entityType, JobActionType.KILL_ENTITY);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory instanceof EnchantingInventory enchantingInventory) {
            ItemStack itemStack = enchantingInventory.getItem();
            if (itemStack == null) return;

            Player player = event.getEnchanter();
            this.jobManager.action(player, event, JobActionType.ENCHANT);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        var inventory = event.getInventory();
        if (event.getWhoClicked() instanceof Player player) {
            if ((inventory.getType() == InventoryType.BREWING || inventory.getType() == InventoryType.FURNACE || inventory.getType() == InventoryType.BLAST_FURNACE || inventory.getType() == InventoryType.SMOKER) && inventory.getHolder() instanceof Container container) {

                var block = container.getBlock();
                if (block.getType() == Material.BREWING_STAND || block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE || block.getType() == Material.SMOKER) {
                    var containerState = (Container) block.getState();

                    var playerUUID = player.getUniqueId();

                    var persistentDataContainer = containerState.getPersistentDataContainer();
                    persistentDataContainer.set(playerKey, PersistentDataType.STRING, playerUUID.toString());

                    containerState.update();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        var brewerInventory = event.getContents();
        var block = brewerInventory.getHolder().getBlock();

        if (block.getType() == Material.BREWING_STAND) {
            var brewingStand = (BrewingStand) block.getState();
            var container = brewingStand.getPersistentDataContainer();

            if (container.has(playerKey, PersistentDataType.STRING)) {
                var uuidString = container.get(playerKey, PersistentDataType.STRING);
                var playerUUID = UUID.fromString(uuidString);

                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    this.jobManager.action(player, event, JobActionType.BREW);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE || block.getType() == Material.SMOKER) {
            Furnace furnace = (Furnace) block.getState();
            PersistentDataContainer container = furnace.getPersistentDataContainer();

            if (container.has(playerKey, PersistentDataType.STRING)) {
                var uuidString = container.get(playerKey, PersistentDataType.STRING);
                var playerUUID = UUID.fromString(uuidString);

                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    this.jobManager.action(player, event.getResult().getType(), JobActionType.SMELT);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStrip(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (this.plugin.getBlockHook().isTracked(block)) return;

        ItemStack tool = event.getItem();
        if (tool == null || !Tag.ITEMS_AXES.isTagged(tool.getType())) return;

        Material before = block.getType();

        if (isNotStrippable(before)) return;

        Material expectedAfter = strippedOf(before);
        if (expectedAfter == null) return;

        plugin.getScheduler().runNextTick(w -> {
            if (block.getType() == expectedAfter) {
                Player player = event.getPlayer();
                this.jobManager.action(player, expectedAfter, JobActionType.STRIPLOGS);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilRepair(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var inventory = event.getInventory();
        if (inventory.getType() != InventoryType.ANVIL) return;
        if (event.getRawSlot() != 2) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        NexoHook nexoHook = this.plugin.getNexoHook();
        if (nexoHook != null) {
            String nexoId = nexoHook.getNexoItemId(result);
            // If result doesn't have Nexo data, check the first input item (slot 0)
            if (nexoId == null) {
                ItemStack firstItem = inventory.getItem(0);
                if (firstItem != null) {
                    nexoId = nexoHook.getNexoItemId(firstItem);
                }
            }
            if (nexoId != null) {
                this.jobManager.action(player, "nexo:" + nexoId, JobActionType.ANVIL_REPAIR);
                return;
            }
        }

        this.jobManager.action(player, result.getType(), JobActionType.ANVIL_REPAIR);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        ItemStack result = event.getResult();
        if (result != null && result.getType() != Material.AIR) {
            SmithingInventory inventory = event.getInventory();
            ItemStack[] cached = new ItemStack[4];
            cached[0] = result.clone();
            for (int i = 0; i < 3; i++) {
                ItemStack item = inventory.getItem(i);
                cached[i + 1] = item != null ? item.clone() : null;
            }
            this.smithingCache.put(player.getUniqueId(), cached);
        }
        // Do NOT clear the cache when result is null/AIR.  When the player picks up
        // the result, the server (or Nexo) removes the input items which fires a new
        // PrepareSmithingEvent with an empty result *during* InventoryClickEvent
        // processing.  Clearing here would wipe the cache before our MONITOR-priority
        // InventoryClickEvent handler gets a chance to use it.  The cache is consumed
        // (removed) in onSmithItem and cleaned up on player quit.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onSmithItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var inventory = event.getInventory();
        if (inventory.getType() != InventoryType.SMITHING) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        boolean debug = Config.enableDebug;

        // Always consume the cache entry when the result slot is clicked
        ItemStack[] cachedItems = this.smithingCache.remove(player.getUniqueId());

        if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] Player " + player.getName() + " clicked result slot in smithing table");
            this.plugin.getLogger().info("[SMITHING DEBUG] Event cancelled: " + event.isCancelled() + ", Click type: " + event.getClick() + ", Action: " + event.getAction());
            this.plugin.getLogger().info("[SMITHING DEBUG] Cache available: " + (cachedItems != null));
        }

        ItemStack result = event.getCurrentItem();

        // Fallback: use SmithingInventory.getResult() if getCurrentItem() is empty
        if ((result == null || result.getType() == Material.AIR) && inventory instanceof SmithingInventory smithingInventory) {
            result = smithingInventory.getResult();
        }

        // Fallback: use cached result from PrepareSmithingEvent if the inventory was
        // already cleared (e.g. Nexo handles the craft and empties slots before MONITOR)
        boolean usingCache = false;
        if ((result == null || result.getType() == Material.AIR) && cachedItems != null && event.isCancelled()) {
            result = cachedItems[0];
            usingCache = true;
            if (debug) {
                this.plugin.getLogger().info("[SMITHING DEBUG] Using cached items from PrepareSmithingEvent");
            }
        }

        // Fallback: check the cursor item.  When Nexo handles the craft it may place
        // the result directly on the player's cursor before our MONITOR handler runs.
        boolean usingCursor = false;
        if ((result == null || result.getType() == Material.AIR) && event.isCancelled()) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                result = cursorItem;
                usingCursor = true;
                if (debug) {
                    this.plugin.getLogger().info("[SMITHING DEBUG] Using cursor item as result fallback");
                }
            }
        }

        boolean hasResult = result != null && result.getType() != Material.AIR;

        if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] hasResult: " + hasResult + " (usingCache: " + usingCache + ", usingCursor: " + usingCursor + ")");
        }

        NexoHook nexoHook = this.plugin.getNexoHook();
        if (nexoHook != null) {

            Set<String> nexoIds = new LinkedHashSet<>();

            // Check result item
            if (hasResult) {
                String resultNexoId = nexoHook.getNexoItemId(result);
                if (resultNexoId != null) {
                    nexoIds.add(resultNexoId);
                }
            }

            // Check input items (slots 0=template, 1=base item, 2=addition)
            // Use cached items when the live inventory was cleared by Nexo
            for (int slot = 0; slot <= 2; slot++) {
                ItemStack inputItem = (usingCache || usingCursor) && cachedItems != null ? cachedItems[slot + 1] : inventory.getItem(slot);
                if (inputItem != null) {
                    String inputNexoId = nexoHook.getNexoItemId(inputItem);
                    if (inputNexoId != null) {
                        nexoIds.add(inputNexoId);
                    }
                }
            }

            if (debug) {
                this.plugin.getLogger().info("[SMITHING DEBUG] Collected Nexo IDs: " + nexoIds);
            }

            if (!nexoIds.isEmpty()) {
                if (hasResult || event.isCancelled()) {
                    for (String nexoId : nexoIds) {
                        if (debug) {
                            this.plugin.getLogger().info("[SMITHING DEBUG] Dispatching action for nexo:" + nexoId + " (SMITHING)");
                        }
                        this.jobManager.action(player, "nexo:" + nexoId, JobActionType.SMITHING);
                    }
                    return;
                }
            }
        }

        // For vanilla items, require a valid result and non-cancelled event
        if (!hasResult || event.isCancelled()) {
            if (debug) {
                this.plugin.getLogger().info("[SMITHING DEBUG] Vanilla path skipped: hasResult=" + hasResult + ", isCancelled=" + event.isCancelled());
            }
            return;
        }

        if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] Dispatching vanilla action for " + result.getType() + " (SMITHING)");
        }
        this.jobManager.action(player, result.getType(), JobActionType.SMITHING);
    }

    private boolean isNotStrippable(Material type) {
        return !Tag.LOGS.isTagged(type) && !Tag.CRIMSON_STEMS.isTagged(type) && !Tag.WARPED_STEMS.isTagged(type) && type != Material.BAMBOO_BLOCK;
    }

    private Material strippedOf(Material type) {
        if (isNotStrippable(type)) return null;
        return Material.matchMaterial("STRIPPED_" + type.name());
    }
}
