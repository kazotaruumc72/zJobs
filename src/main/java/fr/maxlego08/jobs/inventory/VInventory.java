package fr.maxlego08.jobs.inventory;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.exceptions.InventoryOpenException;
import fr.maxlego08.jobs.zcore.utils.ZUtils;
import fr.maxlego08.jobs.zcore.utils.inventory.InventoryResult;
import fr.maxlego08.jobs.zcore.utils.inventory.ItemButton;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class VInventory extends ZUtils implements Cloneable {

    protected int id;
    protected JobsPlugin plugin;
    protected Map<Integer, ItemButton> items = new HashMap<Integer, ItemButton>();
    protected Player player;
    protected int page;
    protected Object[] args;
    protected Inventory inventory;
    protected String guiName;
    protected boolean disableClick = true;
    protected boolean openAsync = false;

    public int getId() {
        return id;
    }

    /**
     * Inventory Id
     *
     * @param id
     * @return
     */
    public VInventory setId(int id) {
        this.id = id;
        return this;
    }

    protected void createInventory(String name) {
        createInventory(name, 54);
    }

    protected void createInventory(String name, int size) {
        this.guiName = name;
        this.inventory = Bukkit.createInventory(null, size, name);
    }

    /**
     * Create default inventory with default size and name
     */
    private void createDefaultInventory() {
        if (this.inventory == null) {
            this.inventory = Bukkit.createInventory(null, 54, "§cDefault Inventory");
        }
    }

    /**
     * Adding an item to the inventory
     * Creates the default inventory if it does not exist
     *
     * @param slot - Inventory slot
     * @param item - ItemStack
     * @return ItemButton
     */
    public ItemButton addItem(int slot, ItemStack item) {

        createDefaultInventory();

        ItemButton button = new ItemButton(item, slot);
        this.items.put(slot, button);

        if (this.openAsync) {
            runAsync(this.plugin, () -> this.inventory.setItem(slot, item));
        } else {
            this.inventory.setItem(slot, item);
        }
        return button;
    }

    /**
     * Allows you to remove an item from the list of items
     *
     * @param slot
     */
    public void removeItem(int slot) {
        this.items.remove(slot);
    }

    /**
     * Allows you to delete all items
     */
    public void clearItem() {
        this.items.clear();
    }

    /**
     * Allows you to retrieve all items
     *
     * @return
     */
    public Map<Integer, ItemButton> getItems() {
        return items;
    }

    /**
     * If the click in the inventory is disabled (which is the default)
     * then it will return true
     *
     * @return vrai ou faux
     */
    public boolean isDisableClick() {
        return disableClick;
    }

    /**
     * Change the ability to click in the inventory
     *
     * @param disableClick
     */
    protected void setDisableClick(boolean disableClick) {
        this.disableClick = disableClick;
    }

    /**
     * Allows to recover the player
     *
     * @return player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Allows you to retrieve the page
     *
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * @return the args
     */
    public Object[] getObjets() {
        return args;
    }

    /**
     * @return the inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * @return the guiName
     */
    public String getGuiName() {
        return guiName;
    }

    protected InventoryResult preOpenInventory(JobsPlugin main, Player player, int page, Object... args) throws InventoryOpenException {

        this.page = page;
        this.args = args;
        this.player = player;
        this.plugin = main;

        return openInventory(main, player, page, args);
    }

    public abstract InventoryResult openInventory(JobsPlugin main, Player player, int page, Object... args) throws InventoryOpenException;

    /**
     * @param event
     * @param plugin
     * @param player
     */
    protected void onClose(InventoryCloseEvent event, JobsPlugin plugin, Player player) {
    }

    /**
     * @param event
     * @param plugin
     * @param player
     */
    protected void onDrag(InventoryDragEvent event, JobsPlugin plugin, Player player) {
    }

    @Override
    protected VInventory clone() {
        try {
            return getClass().newInstance();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
