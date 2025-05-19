package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mining combos for BlockParty
 */
public class ComboManager {
    private final BlockParty plugin;
    
    // Combo tracking
    private final ConcurrentHashMap<UUID, Integer> currentCombos;
    private final ConcurrentHashMap<UUID, Long> lastMineTime;
    private final ConcurrentHashMap<UUID, BukkitTask> comboTimeoutTasks;
    
    // Configuration
    private boolean comboEnabled;
    private int comboExpiryTime; // seconds
    private int comboExpireWarningTime; // seconds
    private double baseRewardMultiplier;
    private int comboLevelUpThreshold;
    private boolean useSound;
    private boolean useParticles;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public ComboManager(BlockParty plugin) {
        this.plugin = plugin;
        this.currentCombos = new ConcurrentHashMap<>();
        this.lastMineTime = new ConcurrentHashMap<>();
        this.comboTimeoutTasks = new ConcurrentHashMap<>();
        loadConfiguration();
    }
    
    /**
     * Load configuration settings
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        comboEnabled = config.getBoolean("combo.enabled", true);
        comboExpiryTime = config.getInt("combo.expiry-time", 5);
        comboExpireWarningTime = config.getInt("combo.warning-time", 3);
        baseRewardMultiplier = config.getDouble("combo.base-multiplier", 0.1);
        comboLevelUpThreshold = config.getInt("combo.level-threshold", 5);
        useSound = config.getBoolean("combo.use-sound", true);
        useParticles = config.getBoolean("combo.use-particles", true);
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        loadConfiguration();
    }
    
    /**
     * Register a successful block mine and update combo
     * @param player The player who mined the block
     * @return The current combo count
     */
    public int incrementCombo(Player player) {
        if (!comboEnabled) {
            return 0;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Get current combo or default to 0
        int currentCombo = currentCombos.getOrDefault(uuid, 0);
        int newCombo = currentCombo + 1;
        
        // Update combo count
        currentCombos.put(uuid, newCombo);
        lastMineTime.put(uuid, Instant.now().getEpochSecond());
        
        // Cancel existing timeout task if it exists
        if (comboTimeoutTasks.containsKey(uuid)) {
            comboTimeoutTasks.get(uuid).cancel();
        }
        
        // Schedule new timeout task
        scheduleComboTimeout(player);
        
        // Play effects based on combo level
        playComboEffects(player, newCombo);
        
        // Send combo message
        if (newCombo % comboLevelUpThreshold == 0) {
            // Level up message for milestone combos (5, 10, 15, etc)
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("combo", String.valueOf(newCombo));
            placeholders.put("multiplier", String.format("%.1fx", getComboMultiplier(newCombo)));
            plugin.getMessageManager().sendMessage(player, "combo.milestone", placeholders);
        } else {
            // Simple combo update
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("combo", String.valueOf(newCombo));
            plugin.getMessageManager().sendActionBar(player, "combo.increment", placeholders);
        }
        
        return newCombo;
    }
    
    /**
     * Schedule the combo timeout task
     * @param player The player
     */
    private void scheduleComboTimeout(Player player) {
        UUID uuid = player.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                long now = Instant.now().getEpochSecond();
                long last = lastMineTime.getOrDefault(uuid, 0L);
                
                // If combo should expire
                if (now - last >= comboExpiryTime) {
                    // Send message only if they had a combo
                    if (currentCombos.containsKey(uuid) && currentCombos.get(uuid) > 1) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("combo", String.valueOf(currentCombos.get(uuid)));
                        plugin.getMessageManager().sendMessage(player, "combo.expired", placeholders);
                    }
                    
                    // Reset combo
                    resetCombo(uuid);
                    this.cancel();
                } 
                // If about to expire, send warning
                else if (now - last >= comboExpiryTime - comboExpireWarningTime) {
                    // Only send warning for actual combos
                    if (currentCombos.getOrDefault(uuid, 0) > 1) {
                        Map<String, String> placeholders = new HashMap<>();
                        int timeLeft = comboExpiryTime - (int)(now - last);
                        placeholders.put("time", String.valueOf(timeLeft));
                        plugin.getMessageManager().sendActionBar(player, "combo.warning", placeholders);
                        
                        // Play warning sound
                        if (useSound && player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
        
        comboTimeoutTasks.put(uuid, task);
    }
    
    /**
     * Reset a player's combo
     * @param uuid The player's UUID
     */
    public void resetCombo(UUID uuid) {
        currentCombos.remove(uuid);
        lastMineTime.remove(uuid);
        
        if (comboTimeoutTasks.containsKey(uuid)) {
            comboTimeoutTasks.get(uuid).cancel();
            comboTimeoutTasks.remove(uuid);
        }
    }
    
    /**
     * Play effects based on combo level
     * @param player The player
     * @param combo The current combo
     */
    private void playComboEffects(Player player, int combo) {
        if (!player.isOnline()) return;
        
        // Determine effect level based on combo
        int level = Math.min(5, (combo / comboLevelUpThreshold) + 1); // Max 5 levels
        
        // Play sound effects if enabled
        if (useSound) {
            float pitch = Math.min(2.0f, 0.8f + (level * 0.2f));
            float volume = Math.min(1.0f, 0.5f + (level * 0.1f));
            
            Sound sound;
            if (combo % comboLevelUpThreshold == 0) {
                // Special sound for milestones
                sound = Sound.ENTITY_PLAYER_LEVELUP;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                // Regular sound for combos
                sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
                player.playSound(player.getLocation(), sound, volume * 0.5f, pitch);
            }
        }
        
        // Play particle effects if enabled
        if (useParticles) {
            int particleCount = Math.min(30, 5 + (combo / 2));
            double spread = Math.min(0.5, 0.2 + (level * 0.05));
            
            Particle particle;
            if (combo % comboLevelUpThreshold == 0) {
                particle = Particle.TOTEM_OF_UNDYING;
                player.getWorld().spawnParticle(
                        particle, 
                        player.getLocation().add(0, 1.0, 0),
                        particleCount, 
                        spread, 0.5, spread, 
                        0.05);
            } else if (combo > comboLevelUpThreshold) {
                particle = Particle.HAPPY_VILLAGER;
                player.getWorld().spawnParticle(
                        particle, 
                        player.getLocation().add(0, 1.0, 0),
                        particleCount / 2, 
                        spread, 0.5, spread, 
                        0);
            }
        }
    }
    
    /**
     * Get a player's current combo
     * @param uuid The player's UUID
     * @return The current combo count
     */
    public int getCurrentCombo(UUID uuid) {
        return currentCombos.getOrDefault(uuid, 0);
    }
    
    /**
     * Get the reward multiplier based on combo
     * @param combo The current combo count
     * @return The reward multiplier
     */
    public double getComboMultiplier(int combo) {
        if (!comboEnabled || combo <= 1) {
            return 1.0;
        }
        
        return 1.0 + (baseRewardMultiplier * (combo - 1));
    }
    
    /**
     * Get the combo multiplier for a player
     * @param uuid The player's UUID
     * @return The reward multiplier
     */
    public double getPlayerComboMultiplier(UUID uuid) {
        return getComboMultiplier(getCurrentCombo(uuid));
    }
    
    /**
     * Clean up on plugin disable
     */
    public void cancelAllTasks() {
        for (BukkitTask task : comboTimeoutTasks.values()) {
            task.cancel();
        }
        comboTimeoutTasks.clear();
        currentCombos.clear();
        lastMineTime.clear();
    }
}