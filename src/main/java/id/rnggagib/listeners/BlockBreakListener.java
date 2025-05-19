package id.rnggagib.listeners;

import id.rnggagib.BlockParty;
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
        
        boolean isBlockPartyPickaxe = plugin.getAccessManager().isAccessItem(itemInHand);
        boolean hasActiveSession = plugin.getSessionManager().hasActiveSession(player.getUniqueId());
        boolean isBlockPartyBlock = plugin.getRewardManager().isMinableBlock(material);
        boolean hasRegions = !plugin.getRegionManager().getRegionNames().isEmpty();
        boolean inBlockPartyRegion = plugin.getRegionManager().isInRegion(block.getLocation());
        
        // ===== FIRST: HANDLE OUTSIDE REGION MINING =====
        // If we have regions defined and player is outside them,
        // allow normal mining with regular tools immediately
        if (hasRegions && !inBlockPartyRegion) {
            // Only restrict using BlockParty pickaxes outside regions
            if (isBlockPartyPickaxe) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "mining.outside-region");
            }
            // Allow normal mining outside regions with ANY other tool
            return;
        }
        
        // ===== SECOND: HANDLE INACTIVE BLOCKPARTY PICKAXES =====
        // Don't allow using BlockParty pickaxes without an active session
        if (isBlockPartyPickaxe && !hasActiveSession) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "mining.inactive-pickaxe");
            return;
        }
        
        // ===== THIRD: HANDLE MINING INSIDE REGIONS =====
        if (hasRegions && inBlockPartyRegion) {
            // Only apply special rules to BlockParty blocks
            if (isBlockPartyBlock) {
                // Must use BlockParty pickaxe with active session for BlockParty blocks
                if (!isBlockPartyPickaxe || !hasActiveSession) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "mining.protected-region");
                    return;
                }
                // Will continue to BlockParty mining logic below
            } else {
                // Not a BlockParty block, allow normal mining
                return;
            }
        }
        // ===== FOURTH: NO REGIONS CONFIGURED =====
        else if (!hasRegions) {
            // If no regions are configured, enforce BlockParty pickaxe for BlockParty blocks
            if (isBlockPartyBlock && !isBlockPartyPickaxe) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "mining.must-use-pickaxe");
                return;
            }
        }
        
        // ===== FINALLY: HANDLE BLOCKPARTY MINING =====
        // Only reach here if:
        // 1. Player is in a BlockParty region or no regions configured
        // 2. Player is using BlockParty pickaxe with active session
        // 3. Block is a BlockParty block
        if (isBlockPartyPickaxe && hasActiveSession && isBlockPartyBlock) {
            // Continue with BlockParty mining logic
            BlockState blockState = block.getState();
            
            // Check if should drop vanilla items
            if (!plugin.getRewardManager().shouldDropVanilla(material)) {
                event.setDropItems(false);
            }
            
            // Update player statistics
            plugin.getPlayerDataManager().getPlayerData(player).incrementBlocksMined();
            
            // Increment combo for the player
            int combo = plugin.getComboManager().incrementCombo(player);
            double multiplier = plugin.getComboManager().getPlayerComboMultiplier(player.getUniqueId());
            
            // Check if should give reward (with combo multiplier)
            if (plugin.getRewardManager().shouldGiveReward(material)) {
                // Pass the combo multiplier to adjust reward chances
                plugin.getRewardManager().giveRandomReward(player, multiplier);
            }
            
            // Schedule block regeneration using the manager
            plugin.getBlockRegenerationManager().scheduleRegeneration(block, blockState);
        }
    }
}