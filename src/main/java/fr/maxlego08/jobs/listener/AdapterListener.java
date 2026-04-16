package fr.maxlego08.jobs.listener;

import fr.maxlego08.jobs.zcore.utils.ZUtils;
import fr.maxlego08.jobs.JobsPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener that delegates Bukkit events to registered {@link fr.maxlego08.jobs.zcore.utils.plugins.Plugins} listener adapters.
 */
@SuppressWarnings("deprecation")
public class AdapterListener extends ZUtils implements Listener {

    private final JobsPlugin plugin;

    /**
     * Constructs a new {@code AdapterListener}.
     *
     * @param plugin the jobs plugin instance.
     */
    public AdapterListener(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player join events.
     *
     * @param event the player join event.
     */
    @EventHandler
    public void onConnect(PlayerJoinEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onConnect(event, event.getPlayer()));
    }

    /**
     * Handles player quit events.
     *
     * @param event the player quit event.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onQuit(event, event.getPlayer()));
    }


    /*@EventHandler
    public void onMove(PlayerMoveEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onMove(event, event.getPlayer()));
        if (event.getFrom().getBlockX() >> 1 == event.getTo().getBlockX() >> 1 && event.getFrom().getBlockZ() >> 1 == event.getTo().getBlockZ() >> 1 && event.getFrom().getWorld() == event.getTo().getWorld()) {
            return;
        }
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onPlayerWalk(event, event.getPlayer(), 1));
    }*/


    /**
     * Handles inventory click events.
     *
     * @param event the inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        this.plugin.getListenerAdapters()
                .forEach(adapter -> adapter.onInventoryClick(event, (Player) event.getWhoClicked()));
    }

    /**
     * Handles block break events.
     *
     * @param event the block break event.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onBlockBreak(event, event.getPlayer()));
    }

    /**
     * Handles block place events.
     *
     * @param event the block place event.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onBlockPlace(event, event.getPlayer()));
    }

    /**
     * Handles entity death events.
     *
     * @param event the entity death event.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        try {
            this.plugin.getListenerAdapters().forEach(adapter -> adapter.onEntityDeath(event, event.getEntity()));
        } catch (Exception e) {
            // Catch any exceptions to prevent entity metadata corruption from crashing the plugin
            // or disconnecting players. MythicMobs/ModelEngine entities may have corrupted metadata.
            this.plugin.getLogger().warning("Error in EntityDeathEvent adapter: " + e.getMessage());
        }
    }

    /**
     * Handles player interact events.
     *
     * @param event the player interact event.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onInteract(event, event.getPlayer()));
    }

    /**
     * Handles async player chat events.
     *
     * @param event the async player chat event.
     */
    @EventHandler
    public void onPlayerTalk(AsyncPlayerChatEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onPlayerTalk(event, event.getMessage()));
    }

    /**
     * Handles craft item events.
     *
     * @param event the craft item event.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onCraftItem(event));
    }

    /**
     * Handles inventory drag events.
     *
     * @param event the inventory drag event.
     */
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        this.plugin.getListenerAdapters()
                .forEach(adapter -> adapter.onInventoryDrag(event, (Player) event.getWhoClicked()));
    }

    /**
     * Handles inventory close events.
     *
     * @param event the inventory close event.
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        this.plugin.getListenerAdapters()
                .forEach(adapter -> adapter.onInventoryClose(event, (Player) event.getPlayer()));
    }

    /**
     * Handles player command preprocess events.
     *
     * @param event the player command preprocess event.
     */
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        this.plugin.getListenerAdapters()
                .forEach(adapter -> adapter.onCommand(event, event.getPlayer(), event.getMessage()));
    }

    /**
     * Handles player game mode change events.
     *
     * @param event the player game mode change event.
     */
    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onGamemodeChange(event, event.getPlayer()));
    }

    /*
     * @EventHandler public void onDrop(PlayerDropItemEvent event) {
     * this.plugin.getListenerAdapters().forEach(adapter ->
     * adapter.onDrop(event, event.getPlayer())); if (!Config.useItemFallEvent)
     * return; Item item = event.getItemDrop(); AtomicBoolean hasSendEvent = new
     * AtomicBoolean(false); scheduleFix(100, (task, isActive) -> { if
     * (!isActive) return; this.plugin.getListenerAdapters().forEach(adapter ->
     * adapter.onItemMove(event, event.getPlayer(), item, item.getLocation(),
     * item.getLocation().getBlock())); if (item.isOnGround() &&
     * !hasSendEvent.get()) { task.cancel(); hasSendEvent.set(true);
     * this.plugin.getListenerAdapters().forEach( adapter ->
     * adapter.onItemisOnGround(event, event.getPlayer(), item,
     * item.getLocation())); } }); }
     */

    /**
     * Handles player item pickup events.
     *
     * @param event the player pickup item event.
     */
    @EventHandler
    public void onPick(PlayerPickupItemEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onPickUp(event, event.getPlayer()));
    }

    /**
     * Handles creature spawn events.
     *
     * @param event the creature spawn event.
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        this.plugin.getListenerAdapters().forEach(adapter -> adapter.onMobSpawn(event));
    }

    /**
     * Handles entity damage by entity events.
     *
     * @param event the entity damage by entity event.
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (event.getEntity() instanceof LivingEntity && event.getDamager() instanceof LivingEntity) {
            this.plugin.getListenerAdapters().forEach(adapter -> adapter.onDamageByEntity(event, event.getCause(),
                    event.getDamage(), (LivingEntity) event.getDamager(), (LivingEntity) event.getEntity()));
        }

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            this.plugin.getListenerAdapters().forEach(adapter -> adapter.onPlayerDamagaByPlayer(event, event.getCause(),
                    event.getDamage(), (Player) event.getDamager(), (Player) event.getEntity()));
        }

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Projectile) {
            this.plugin.getListenerAdapters().forEach(adapter -> adapter.onPlayerDamagaByArrow(event, event.getCause(),
                    event.getDamage(), (Projectile) event.getDamager(), (Player) event.getEntity()));
        }
    }
}
