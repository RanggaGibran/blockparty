package id.rnggagib.placeholder;

import id.rnggagib.BlockParty;
import id.rnggagib.managers.PlayerDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Manages PlaceholderAPI integration for BlockParty
 */
public class PlaceholderManager extends PlaceholderExpansion {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public PlaceholderManager(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * The placeholder identifier for this expansion
     * @return The identifier in "%<identifier>_<value>%" format
     */
    @Override
    public String getIdentifier() {
        return "blockparty";
    }
    
    /**
     * The author of this expansion
     * @return The name of the author
     */
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    /**
     * The version of this expansion
     * @return The version string
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    /**
     * This method is called when a placeholder with our identifier is found
     */
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null || !player.isOnline()) {
            return "";
        }
        
        // Get player data
        PlayerDataManager.PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        // Handle placeholders
        switch (identifier.toLowerCase()) {
            case "blocks_mined":
                return String.valueOf(playerData.getBlocksMined());
                
            case "rewards_found":
                return String.valueOf(playerData.getRewardsFound());
                
            case "mmoitems_found":
                return String.valueOf(playerData.getMmoItemsFound());
                
            case "keys_found":
                return String.valueOf(playerData.getKeysFound());
                
            case "session_active":
                return plugin.getSessionManager().hasActiveSession(player.getUniqueId()) ? "yes" : "no";
                
            case "session_time_left":
                if (!plugin.getSessionManager().hasActiveSession(player.getUniqueId())) {
                    return "0:00";
                }
                long timeLeft = plugin.getSessionManager().getRemainingTime(player.getUniqueId());
                return String.format("%d:%02d", timeLeft / 60, timeLeft % 60);
                
            case "can_mine_here":
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer == null) {
                    return "no";
                }
                //return plugin.getWorldGuardManager().canUseBlockParty(onlinePlayer, onlinePlayer.getLocation()) ? "yes" : "no";
        }
        
        return null; // Placeholder not found
    }
    
    // No need to override the register() method; use the inherited one from PlaceholderExpansion.
}