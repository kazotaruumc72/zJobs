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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class JobListener implements Listener {

    private final JobsPlugin plugin;
    private final JobManager jobManager;
    private final NamespacedKey playerKey;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null) {
            this.jobManager.action(entity.getKiller(), entity, JobActionType.KILL_ENTITY);
        }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onSmithItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var inventory = event.getInventory();
        if (inventory.getType() != InventoryType.SMITHING) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        boolean debug = Config.enableDebug;

        if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] Player " + player.getName() + " clicked result slot in smithing table");
            this.plugin.getLogger().info("[SMITHING DEBUG] Event cancelled: " + event.isCancelled() + ", Click type: " + event.getClick() + ", Action: " + event.getAction());
        }

        ItemStack result = event.getCurrentItem();

        if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] getCurrentItem(): " + (result != null ? result.getType() + " x" + result.getAmount() : "null"));
        }

        // Fallback: use SmithingInventory.getResult() if getCurrentItem() is empty
        // This handles cases where another plugin (e.g. Nexo) modified the inventory during event processing
        if ((result == null || result.getType() == Material.AIR) && inventory instanceof SmithingInventory smithingInventory) {
            result = smithingInventory.getResult();
            if (debug) {
                this.plugin.getLogger().info("[SMITHING DEBUG] Fallback SmithingInventory.getResult(): " + (result != null ? result.getType() + " x" + result.getAmount() : "null"));
            }
        }

        boolean hasResult = result != null && result.getType() != Material.AIR;

        if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] hasResult: " + hasResult);
        }

        NexoHook nexoHook = this.plugin.getNexoHook();
        if (nexoHook != null) {
            if (debug) {
                this.plugin.getLogger().info("[SMITHING DEBUG] NexoHook is available");
            }

            Set<String> nexoIds = new LinkedHashSet<>();

            // Check result item
            if (hasResult) {
                String resultNexoId = nexoHook.getNexoItemId(result);
                if (debug) {
                    this.plugin.getLogger().info("[SMITHING DEBUG] Result Nexo ID: " + resultNexoId);
                }
                if (resultNexoId != null) {
                    nexoIds.add(resultNexoId);
                }
            }

            // Always check input items too (slots 0=template, 1=base item, 2=addition)
            // The result may be a different Nexo item than the inputs, and the user
            // may configure actions based on any item involved in the smithing.
            for (int slot = 0; slot <= 2; slot++) {
                ItemStack inputItem = inventory.getItem(slot);
                if (inputItem != null) {
                    String inputNexoId = nexoHook.getNexoItemId(inputItem);
                    if (debug) {
                        this.plugin.getLogger().info("[SMITHING DEBUG] Slot " + slot + ": " + inputItem.getType() + " x" + inputItem.getAmount() + ", Nexo ID: " + inputNexoId);
                    }
                    if (inputNexoId != null) {
                        nexoIds.add(inputNexoId);
                    }
                } else if (debug) {
                    this.plugin.getLogger().info("[SMITHING DEBUG] Slot " + slot + ": empty");
                }
            }

            if (debug) {
                this.plugin.getLogger().info("[SMITHING DEBUG] Collected Nexo IDs: " + nexoIds);
            }

            if (!nexoIds.isEmpty()) {
                // Fire actions if there's a valid result (normal case), or if the event
                // was cancelled AND Nexo items are in inputs (Nexo likely handled the
                // smithing itself and consumed the result before our handler ran).
                if (hasResult || event.isCancelled()) {
                    for (String nexoId : nexoIds) {
                        if (debug) {
                            this.plugin.getLogger().info("[SMITHING DEBUG] Dispatching action for nexo:" + nexoId + " (SMITHING)");
                        }
                        this.jobManager.action(player, "nexo:" + nexoId, JobActionType.SMITHING);
                    }
                    return;
                } else if (debug) {
                    this.plugin.getLogger().info("[SMITHING DEBUG] Nexo IDs found but skipped: hasResult=" + hasResult + ", isCancelled=" + event.isCancelled());
                }
            }
        } else if (debug) {
            this.plugin.getLogger().info("[SMITHING DEBUG] NexoHook is null (Nexo not loaded)");
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
