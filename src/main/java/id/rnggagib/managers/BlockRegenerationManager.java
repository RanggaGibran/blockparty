package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles advanced block regeneration for BlockParty
 */
public class BlockRegenerationManager {
    private final BlockParty plugin;
    private final Random random = new Random();
    private final ConcurrentHashMap<Location, RegenerationTask> pendingRegeneration = new ConcurrentHashMap<>();
    
    // Configuration settings
    private int minRegenerationTime;
    private int maxRegenerationTime;
    private boolean useEffects;
    private boolean useSound;
    private boolean veinMining;
    private int maxVeinSize;
    private RegenerationType defaultRegenerationType;
    private Map<Material, RegenerationConfig> materialConfigs;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public BlockRegenerationManager(BlockParty plugin) {
        this.plugin = plugin;
        this.materialConfigs = new HashMap<>();
        loadConfiguration();
    }
    
    /**
     * Load configuration for block regeneration
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection regSection = config.getConfigurationSection("regeneration");
        
        if (regSection != null) {
            minRegenerationTime = regSection.getInt("min-time", 5);
            maxRegenerationTime = regSection.getInt("max-time", 30);
            useEffects = regSection.getBoolean("use-effects", true);
            useSound = regSection.getBoolean("use-sound", true);
            veinMining = regSection.getBoolean("vein-mining", true);
            maxVeinSize = regSection.getInt("max-vein-size", 8);
            
            // Load default regeneration type
            String defaultType = regSection.getString("default-type", "DELAYED_RANDOM");
            try {
                defaultRegenerationType = RegenerationType.valueOf(defaultType);
            } catch (IllegalArgumentException e) {
                defaultRegenerationType = RegenerationType.DELAYED_RANDOM;
                plugin.getLogger().warning("Invalid regeneration type: " + defaultType + ". Using DELAYED_RANDOM instead.");
            }
            
            // Load material-specific configurations
            if (regSection.isConfigurationSection("materials")) {
                ConfigurationSection materialsSection = regSection.getConfigurationSection("materials");
                for (String key : materialsSection.getKeys(false)) {
                    try {
                        Material material = Material.valueOf(key);
                        ConfigurationSection materialSection = materialsSection.getConfigurationSection(key);
                        
                        if (materialSection != null) {
                            int minTime = materialSection.getInt("min-time", minRegenerationTime);
                            int maxTime = materialSection.getInt("max-time", maxRegenerationTime);
                            boolean useEffect = materialSection.getBoolean("use-effects", useEffects);
                            boolean useMatSound = materialSection.getBoolean("use-sound", useSound);
                            
                            String typeName = materialSection.getString("type", defaultType);
                            RegenerationType type;
                            try {
                                type = RegenerationType.valueOf(typeName);
                            } catch (IllegalArgumentException e) {
                                type = defaultRegenerationType;
                            }
                            
                            materialConfigs.put(material, new RegenerationConfig(
                                    minTime, maxTime, useEffect, useMatSound, type
                            ));
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in regeneration config: " + key);
                    }
                }
            }
        } else {
            // Default values if section doesn't exist
            minRegenerationTime = 5;
            maxRegenerationTime = 30;
            useEffects = true;
            useSound = true;
            veinMining = true;
            maxVeinSize = 8;
            defaultRegenerationType = RegenerationType.DELAYED_RANDOM;
        }
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        materialConfigs.clear();
        loadConfiguration();
    }
    
    /**
     * Schedule a block to regenerate
     * @param block The block to regenerate
     * @param state The original state of the block
     */
    public void scheduleRegeneration(Block block, BlockState state) {
        // Check if block is already scheduled for regeneration
        if (pendingRegeneration.containsKey(block.getLocation())) {
            return;
        }
        
        Material material = state.getType();
        RegenerationConfig config = materialConfigs.getOrDefault(material, 
                new RegenerationConfig(minRegenerationTime, maxRegenerationTime, 
                        useEffects, useSound, defaultRegenerationType));
        
        // Calculate regeneration delay
        int delay;
        if (config.minTime == config.maxTime) {
            delay = config.minTime;
        } else {
            delay = config.minTime + random.nextInt(config.maxTime - config.minTime + 1);
        }
        
        // Create regeneration task
        RegenerationTask task = new RegenerationTask(block, state, config);
        pendingRegeneration.put(block.getLocation(), task);
        
        // Handle different regeneration types
        switch (config.type) {
            case INSTANT:
                task.startTask(1);
                break;
                
            case DELAYED_FIXED:
                task.startTask(delay * 20L); // Convert to ticks
                break;
                
            case DELAYED_RANDOM:
                task.startTask(delay * 20L); // Convert to ticks
                break;
                
            case ANIMATED:
                task.startAnimatedTask(delay * 20L); // Convert to ticks
                break;
                
            default:
                task.startTask(delay * 20L); // Convert to ticks
                break;
        }
        
        // Check for vein mining if enabled
        if (veinMining && shouldVeinMine(material)) {
            processVein(block, state.getType());
        }
    }
    
    /**
     * Check if a material should be vein mined
     * @param material The material to check
     * @return True if the material should be vein mined
     */
    private boolean shouldVeinMine(Material material) {
        return material.name().contains("ORE") || material == Material.ANCIENT_DEBRIS;
    }
    
    /**
     * Process a vein of connected blocks of the same type
     * @param startBlock The starting block
     * @param material The material to look for
     */
    private void processVein(Block startBlock, Material material) {
        Map<Location, Block> processedBlocks = new HashMap<>();
        processedBlocks.put(startBlock.getLocation(), startBlock);
        
        // Process connected blocks
        processConnectedBlocks(startBlock, material, processedBlocks);
    }
    
    /**
     * Recursively process connected blocks of the same material
     * @param block The current block
     * @param material The material to look for
     * @param processedBlocks Map of already processed block locations
     */
    private void processConnectedBlocks(Block block, Material material, Map<Location, Block> processedBlocks) {
        // Stop if we've reached the maximum vein size
        if (processedBlocks.size() >= maxVeinSize) {
            return;
        }
        
        // Check all adjacent blocks
        for (BlockFace face : new BlockFace[] { 
                BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, 
                BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST }) {
            
            Block adjacent = block.getRelative(face);
            
            // Skip if already processed
            if (processedBlocks.containsKey(adjacent.getLocation())) {
                continue;
            }
            
            // Check if it's the same material and schedule regeneration
            if (adjacent.getType() == material) {
                BlockState adjacentState = adjacent.getState();
                processedBlocks.put(adjacent.getLocation(), adjacent);
                
                // Store the state before changing it
                scheduleRegeneration(adjacent, adjacentState);
                
                // Process connected blocks recursively
                processConnectedBlocks(adjacent, material, processedBlocks);
            }
        }
    }
    
    /**
     * Cancel all pending regeneration tasks
     */
    public void cancelAllTasks() {
        for (RegenerationTask task : pendingRegeneration.values()) {
            task.cancel();
        }
        pendingRegeneration.clear();
    }
    
    /**
     * Inner class to handle regeneration tasks
     */
    private class RegenerationTask {
        private final Block block;
        private final BlockState originalState;
        private final RegenerationConfig config;
        private BukkitTask task;
        
        public RegenerationTask(Block block, BlockState originalState, RegenerationConfig config) {
            this.block = block;
            this.originalState = originalState;
            this.config = config;
        }
        
        /**
         * Start a regular regeneration task
         * @param delay Delay in ticks
         */
        public void startTask(long delay) {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    regenerateBlock();
                }
            }.runTaskLater(plugin, delay);
        }
        
        /**
         * Start an animated regeneration task
         * @param delay Delay in ticks
         */
        public void startAnimatedTask(long delay) {
            // First wait for the main delay
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    // Then start the animation
                    startAnimation();
                }
            }.runTaskLater(plugin, delay);
        }
        
        /**
         * Start the block regeneration animation
         */
        private void startAnimation() {
            task = new BukkitRunnable() {
                private int step = 0;
                private final int totalSteps = 10;
                
                @Override
                public void run() {
                    if (step >= totalSteps) {
                        regenerateBlock();
                        this.cancel();
                        return;
                    }
                    
                    // Create particles at block location
                    if (config.useEffects) {
                        Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
                        block.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 0.25, 0.25, 0.25, 0.01);
                        
                        if (step % 3 == 0 && config.useSound) {
                            block.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
                        }
                    }
                    
                    step++;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
        
        /**
         * Execute the block regeneration
         */
        private void regenerateBlock() {
            // Play regeneration effect if enabled
            if (config.useEffects) {
                Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
                block.getWorld().spawnParticle(Particle.CLOUD, loc, 15, 0.4, 0.4, 0.4, 0.05);
            }
            
            // Play sound if enabled
            if (config.useSound) {
                Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
                block.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
            }
            
            // Restore the block state
            block.setType(originalState.getType());
            
            // Remove from pending regeneration
            pendingRegeneration.remove(block.getLocation());
        }
        
        /**
         * Cancel the regeneration task
         */
        public void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }
    
    /**
     * Configuration for block regeneration
     */
    private static class RegenerationConfig {
        private final int minTime;
        private final int maxTime;
        private final boolean useEffects;
        private final boolean useSound;
        private final RegenerationType type;
        
        public RegenerationConfig(int minTime, int maxTime, boolean useEffects, 
                                 boolean useSound, RegenerationType type) {
            this.minTime = minTime;
            this.maxTime = maxTime;
            this.useEffects = useEffects;
            this.useSound = useSound;
            this.type = type;
        }
    }
    
    /**
     * Regeneration types for different visual effects
     */
    public enum RegenerationType {
        INSTANT,        // Regenerate instantly
        DELAYED_FIXED,  // Regenerate after fixed time
        DELAYED_RANDOM, // Regenerate after random time
        ANIMATED        // Regenerate with animation
    }
}