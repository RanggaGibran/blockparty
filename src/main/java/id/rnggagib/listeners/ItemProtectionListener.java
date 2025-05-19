package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles protection of BlockParty items from being dropped or transferred to other inventories
 */
public class ItemProtectionListener implements Listener {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public ItemProtectionListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent players from dropping BlockParty pickaxes
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Check if the dropped item is a BlockParty pickaxe
        if (plugin.getAccessManager().isAccessItem(item)) {
            // Cancel the drop event
            event.setCancelled(true);
            
            // Notify the player
            plugin.getMessageManager().sendMessage(event.getPlayer(), "access.cannot-drop");
        }
    }
    
    /**
     * Prevent players from moving BlockParty pickaxes to other inventories
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // Skip if it's not a player
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Allow in creative mode
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("blockparty.admin")) {
            return;
        }
        
        // Get the clicked item
        ItemStack clickedItem = event.getCurrentItem();
        
        // Check if the clicked item is a BlockParty pickaxe
        boolean isPickaxe = clickedItem != null && plugin.getAccessManager().isAccessItem(clickedItem);
        
        // If not a pickaxe, check the cursor item too (for placing into inventories)
        if (!isPickaxe) {
            ItemStack cursorItem = event.getCursor();
            isPickaxe = cursorItem != null && !cursorItem.getType().isAir() && 
                      plugin.getAccessManager().isAccessItem(cursorItem);
        }
        
        // If no BlockParty pickaxe involved, allow the action
        if (!isPickaxe) {
            return;
        }
        
        // Always allow moving within player inventory
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() == InventoryType.PLAYER && 
            event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            return;
        }
        
        // Block transfers to other inventories
        // Check if destination is not player inventory
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "access.cannot-transfer");
            return;
        }
        
        // Prevent SHIFT+click to move to other inventory
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            // Check if top inventory is not player inventory (must be some container)
            if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "access.cannot-transfer");
                return;
            }
        }
    }
}