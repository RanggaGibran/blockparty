package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
        // Only handle main hand interactions
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        // Only handle right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Check if player is holding our access item
        ItemStack item = event.getItem();
        if (item == null || !plugin.getAccessManager().isAccessItem(item)) {
            return;
        }
        
        // Cancel the event to prevent normal item use
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Check for permission
        if (!player.hasPermission("blockparty.use")) {
            plugin.getMessageManager().sendMessage(player, "plugin.no-permission");
            return;
        }
        
        // Check if player already has an active session
        if (plugin.getSessionManager().hasActiveSession(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "access.already-active");
            return;
        }
        
        // Check if player is in a BlockParty region
        boolean hasRegions = !plugin.getRegionManager().getRegionNames().isEmpty();
        boolean inRegion = plugin.getRegionManager().isInRegion(player.getLocation());
        
        // Only allow access in regions, if regions are defined
        if (hasRegions && !inRegion) {
            plugin.getMessageManager().sendMessage(player, "access.denied");
            return;
        }
        
        // Start mining session
        boolean sessionStarted = plugin.getSessionManager().startSession(player);
        
        if (sessionStarted) {
            // Send access granted message
            plugin.getMessageManager().sendMessage(player, "access.granted");
            
            // DON'T consume the access item here - the session will handle item removal when it expires
            // Instead, play a sound and effect to indicate successful activation
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
        }
    }
}