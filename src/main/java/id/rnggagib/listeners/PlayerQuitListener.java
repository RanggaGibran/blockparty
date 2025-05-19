package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player quit events for BlockParty
 */
public class PlayerQuitListener implements Listener {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public PlayerQuitListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Save player data and clean up when a player quits
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // End any active mining session
        if (plugin.getSessionManager().hasActiveSession(player.getUniqueId())) {
            plugin.getSessionManager().endSession(player.getUniqueId(), false);
        }
        
        // Save and unload player data
        plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());
    }
}