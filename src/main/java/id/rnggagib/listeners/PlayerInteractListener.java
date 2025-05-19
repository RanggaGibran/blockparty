package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles player interactions with BlockParty access items
 */
public class PlayerInteractListener implements Listener {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public PlayerInteractListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player interactions with access items
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Skip if not a right-click action
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Skip if off-hand (to prevent double activations)
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if the item is a BlockParty access item
        if (item == null || !plugin.getAccessManager().isAccessItem(item)) {
            return;
        }
        
        // Cancel the event to prevent normal interactions
        event.setCancelled(true);
        
        // Check if player has permission
        if (!player.hasPermission("blockparty.use")) {
            plugin.getMessageManager().sendMessage(player, "plugin.no-permission");
            return;
        }
        
        // Check if player already has an active session
        if (plugin.getSessionManager().hasActiveSession(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "access.already-active");
            return;
        }
        
        // Check WorldGuard regions if enabled
        if (!plugin.getWorldGuardManager().canUseBlockParty(player, player.getLocation())) {
            plugin.getMessageManager().sendMessage(player, "access.denied");
            return;
        }
        
        // Start mining session
        boolean sessionStarted = plugin.getSessionManager().startSession(player);
        
        if (sessionStarted) {
            // Send access granted message
            plugin.getMessageManager().sendMessage(player, "access.granted");
            
            // Consume one item if not in creative mode
            if (!player.getGameMode().toString().equals("CREATIVE")) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}