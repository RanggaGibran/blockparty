package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Block Party mining sessions
 */
public class MiningSessionManager {
    private final BlockParty plugin;
    private final ConcurrentHashMap<UUID, Long> activeSessions;
    private final ConcurrentHashMap<UUID, BukkitTask> actionBarTasks;
    private final ConcurrentHashMap<UUID, BukkitTask> sessionEndTasks;
    
    // Configuration
    private int sessionDuration;
    private boolean showTimer;
    private int timerUpdateFrequency;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public MiningSessionManager(BlockParty plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.actionBarTasks = new ConcurrentHashMap<>();
        this.sessionEndTasks = new ConcurrentHashMap<>();
        loadConfiguration();
    }
    
    /**
     * Loads configuration settings
     */
    private void loadConfiguration() {
        // Get settings from config
        sessionDuration = plugin.getConfigManager().getConfig().getInt("settings.mining-session-duration", 300);
        showTimer = plugin.getConfigManager().getConfig().getBoolean("settings.show-timer", true);
        timerUpdateFrequency = plugin.getConfigManager().getConfig().getInt("settings.timer-update-frequency", 20);
    }
    
    /**
     * Starts a new mining session for a player
     * @param player The player
     * @return True if session started, false if player already has an active session
     */
    public boolean startSession(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check if player already has an active session
        if (hasActiveSession(uuid)) {
            return false;
        }
        
        // Calculate end time
        long endTime = Instant.now().getEpochSecond() + sessionDuration;
        activeSessions.put(uuid, endTime);
        
        // Send session start message
        plugin.getMessageManager().sendMessage(player, "timer.started");
        
        // Schedule session end
        BukkitTask sessionEndTask = new BukkitRunnable() {
            @Override
            public void run() {
                endSession(uuid, true); // true indicates the session expired naturally
            }
        }.runTaskLater(plugin, sessionDuration * 20L); // Convert seconds to ticks
        
        sessionEndTasks.put(uuid, sessionEndTask);
        
        // Set up action bar timer if enabled
        if (showTimer) {
            startActionBarTimer(player);
        }
        
        // Schedule warnings
        scheduleWarnings(player);
        
        return true;
    }
    
    /**
     * Schedule warnings before session ends
     * @param player The player
     */
    private void scheduleWarnings(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Warning at 60 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasActiveSession(uuid)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", "60 seconds");
                    plugin.getMessageManager().sendMessage(player, "timer.warning", placeholders);
                }
            }
        }.runTaskLater(plugin, (sessionDuration - 60) * 20L);
        
        // Warning at 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasActiveSession(uuid)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", "30 seconds");
                    plugin.getMessageManager().sendMessage(player, "timer.warning", placeholders);
                }
            }
        }.runTaskLater(plugin, (sessionDuration - 30) * 20L);
        
        // Warning at 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (hasActiveSession(uuid)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", "10 seconds");
                    plugin.getMessageManager().sendMessage(player, "timer.warning", placeholders);
                }
            }
        }.runTaskLater(plugin, (sessionDuration - 10) * 20L);
    }
    
    /**
     * Start the action bar timer for a player
     * @param player The player
     */
    private void startActionBarTimer(Player player) {
        UUID uuid = player.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if session is still active
                if (!hasActiveSession(uuid)) {
                    this.cancel();
                    return;
                }
                
                // Get remaining time
                long currentTime = Instant.now().getEpochSecond();
                long endTime = activeSessions.get(uuid);
                long timeRemaining = Math.max(0, endTime - currentTime);
                
                // Format time as minutes:seconds
                String formattedTime = String.format("%d:%02d", timeRemaining / 60, timeRemaining % 60);
                
                // Send action bar with enhanced formatting
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", formattedTime);
                
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    // Use more prominent formatting for the action bar timer
                    plugin.getMessageManager().sendActionBar(onlinePlayer, "timer.action-bar-enhanced", placeholders);
                    
                    // If time is running low (less than 30 seconds), play sound notification
                    if (timeRemaining <= 30 && timeRemaining % 5 == 0) {
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, timerUpdateFrequency);
        
        actionBarTasks.put(uuid, task);
    }
    
    /**
     * End a mining session
     * @param uuid The player's UUID
     * @param expired True if the session ended because time expired, false otherwise
     */
    public void endSession(UUID uuid, boolean expired) {
        if (!activeSessions.containsKey(uuid)) {
            return;
        }
        
        // Remove from active sessions
        activeSessions.remove(uuid);
        
        // Cancel tasks
        if (actionBarTasks.containsKey(uuid)) {
            actionBarTasks.get(uuid).cancel();
            actionBarTasks.remove(uuid);
        }
        
        if (sessionEndTasks.containsKey(uuid)) {
            sessionEndTasks.get(uuid).cancel();
            sessionEndTasks.remove(uuid);
        }
        
        // Send end message if player is online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            // If the session expired naturally, remove the access item
            if (expired) {
                removeAccessItem(player);
                plugin.getMessageManager().sendMessage(player, "timer.ended-item-removed");
            } else {
                plugin.getMessageManager().sendMessage(player, "timer.ended");
            }
        }
    }
    
    /**
     * Remove the access item from a player's inventory
     * @param player The player
     */
    private void removeAccessItem(Player player) {
        // Check inventory for access items and remove one
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getAccessManager().isAccessItem(item)) {
                // If only one item, set to null
                if (item.getAmount() == 1) {
                    player.getInventory().removeItem(item);
                } else {
                    // If multiple items, reduce by 1
                    item.setAmount(item.getAmount() - 1);
                }
                break; // Just remove one
            }
        }
    }
    
    /**
     * Check if a player has an active session
     * @param uuid The player's UUID
     * @return True if player has an active session
     */
    public boolean hasActiveSession(UUID uuid) {
        if (!activeSessions.containsKey(uuid)) {
            return false;
        }
        
        // Check if the session has expired
        long currentTime = Instant.now().getEpochSecond();
        long endTime = activeSessions.get(uuid);
        
        if (currentTime >= endTime) {
            // Session has expired, clean it up
            endSession(uuid, true);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the remaining time for a player's session
     * @param uuid The player's UUID
     * @return Remaining time in seconds, or 0 if no active session
     */
    public long getRemainingTime(UUID uuid) {
        if (!hasActiveSession(uuid)) {
            return 0;
        }
        
        long currentTime = Instant.now().getEpochSecond();
        long endTime = activeSessions.get(uuid);
        
        return Math.max(0, endTime - currentTime);
    }
    
    /**
     * Cancel all active sessions (used when disabling the plugin)
     */
    public void cancelAllSessions() {
        for (UUID uuid : activeSessions.keySet()) {
            endSession(uuid, false); // false because this is manually canceled, not expired
        }
        
        // Clear all collections just to be safe
        activeSessions.clear();
        actionBarTasks.clear();
        sessionEndTasks.clear();
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        loadConfiguration();
    }
}