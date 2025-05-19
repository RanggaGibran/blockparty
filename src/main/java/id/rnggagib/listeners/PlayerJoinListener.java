package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles player join events for BlockParty
 */
public class PlayerJoinListener implements Listener {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public PlayerJoinListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load player data when a player joins
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        plugin.getPlayerDataManager().getPlayerData(player);
    }
}