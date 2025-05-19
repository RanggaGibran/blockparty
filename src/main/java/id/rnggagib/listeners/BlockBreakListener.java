package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles block breaking for BlockParty sessions
 */
public class BlockBreakListener implements Listener {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public BlockBreakListener(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle block breaking
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        
        // Check if the player is using a BlockParty access item for normal mining
        if (plugin.getAccessManager().isAccessItem(itemInHand) && 
                !plugin.getSessionManager().hasActiveSession(player.getUniqueId())) {
            // Cancel the event - can't use BlockParty pickaxe for normal mining
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "mining.inactive-pickaxe");
            return;
        }
        
        // Check if this is a minable block in the config
        if (!plugin.getRewardManager().isMinableBlock(material)) {
            return; // Not a configured block, ignore
        }
        
        // Check if player has an active session
        if (!plugin.getSessionManager().hasActiveSession(player.getUniqueId())) {
            // Only cancel if they're trying to mine a BlockParty block
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "mining.no-active-session");
            return;
        }
        
        // Check if player is using the BlockParty access item
        if (!plugin.getAccessManager().isAccessItem(itemInHand)) {
            // Cancel the event - must use BlockParty pickaxe for mining during session
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "mining.must-use-pickaxe");
            return;
        }
        
        // Store the block state for regeneration
        BlockState blockState = block.getState();
        
        // Check if should drop vanilla items
        if (!plugin.getRewardManager().shouldDropVanilla(material)) {
            event.setDropItems(false);
        }
        
        // Update player statistics
        plugin.getPlayerDataManager().getPlayerData(player).incrementBlocksMined();
        
        // Check if should give reward
        if (plugin.getRewardManager().shouldGiveReward(material)) {
            plugin.getRewardManager().giveRandomReward(player);
        }
        
        // Schedule block regeneration using the manager
        plugin.getBlockRegenerationManager().scheduleRegeneration(block, blockState);
        
        // Inform player about block regeneration
        plugin.getMessageManager().sendMessage(player, "mining.block-regenerate");
    }
}