package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles region selection wand interactions
 */
public class RegionWandListener implements Listener {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public RegionWandListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main hand interactions
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if player is holding a region wand
        if (!plugin.getSelectionWand().isWand(item)) {
            return;
        }
        
        // Cancel the event to prevent normal item use
        event.setCancelled(true);
        
        // Check permission
        if (!player.hasPermission("blockparty.admin.region")) {
            plugin.getMessageManager().sendMessage(player, "region.no-permission");
            return;
        }
        
        // Get click location
        Location clickedLoc = null;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            clickedLoc = event.getClickedBlock().getLocation();
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
            // For air clicks, use the block the player is looking at up to 100 blocks away
            clickedLoc = player.getTargetBlock(null, 100).getLocation();
        } else {
            return;
        }
        
        // Handle different click types
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            // First position
            plugin.getRegionManager().setFirstSelection(player, clickedLoc);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(clickedLoc.getBlockX()));
            placeholders.put("y", String.valueOf(clickedLoc.getBlockY()));
            placeholders.put("z", String.valueOf(clickedLoc.getBlockZ()));
            plugin.getMessageManager().sendMessage(player, "region.pos1-selected", placeholders);
            
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            // Second position
            plugin.getRegionManager().setSecondSelection(player, clickedLoc);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(clickedLoc.getBlockX()));
            placeholders.put("y", String.valueOf(clickedLoc.getBlockY()));
            placeholders.put("z", String.valueOf(clickedLoc.getBlockZ()));
            plugin.getMessageManager().sendMessage(player, "region.pos2-selected", placeholders);
        }
    }
}