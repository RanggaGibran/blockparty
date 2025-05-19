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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles block breaking for BlockParty sessions
 */
public class BlockBreakListener implements Listener {
    private final BlockParty plugin;
    // Store blocks that should regenerate
    private final Map<Block, BlockState> blocksToRegenerate;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public BlockBreakListener(BlockParty plugin) {
        this.plugin = plugin;
        this.blocksToRegenerate = new HashMap<>();
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
        
        // Schedule block regeneration
        scheduleBlockRegeneration(block, blockState);
        
        // Inform player about block regeneration
        plugin.getMessageManager().sendMessage(player, "mining.block-regenerate");
    }
    
    /**
     * Schedule a block to regenerate after a delay
     * @param block The block to regenerate
     * @param originalState The original state of the block
     */
    private void scheduleBlockRegeneration(Block block, BlockState originalState) {
        // Store original state
        blocksToRegenerate.put(block, originalState);
        
        // Schedule regeneration (5-30 seconds, random)
        int delay = 5 + (int)(Math.random() * 25);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Make sure the block hasn't been changed by something else
            if (blocksToRegenerate.containsKey(block)) {
                BlockState storedState = blocksToRegenerate.get(block);
                block.setType(storedState.getType());
                blocksToRegenerate.remove(block);
            }
        }, delay * 20L); // Convert to ticks (20 ticks = 1 second)
    }
}