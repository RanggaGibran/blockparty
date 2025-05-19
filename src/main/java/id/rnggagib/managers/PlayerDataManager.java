package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player data storage and statistics
 */
public class PlayerDataManager {
    private final BlockParty plugin;
    private final ConcurrentHashMap<UUID, PlayerData> playerData;
    private final File dataFolder;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public PlayerDataManager(BlockParty plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        
        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * Gets player data for a specific player, loading it if necessary
     * @param player The player
     * @return The player's data
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    /**
     * Gets player data for a specific UUID, loading it if necessary
     * @param uuid The player's UUID
     * @return The player's data
     */
    public PlayerData getPlayerData(UUID uuid) {
        // Check if data is already loaded in memory
        if (!playerData.containsKey(uuid)) {
            // Load player data
            PlayerData data = loadPlayerData(uuid);
            playerData.put(uuid, data);
        }
        
        return playerData.get(uuid);
    }
    
    /**
     * Loads player data from file
     * @param uuid The player's UUID
     * @return The loaded player data or a new instance if not found
     */
    private PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            return new PlayerData(uuid);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        
        // Load statistics
        int blocksMined = config.getInt("stats.blocks-mined", 0);
        int rewardsFound = config.getInt("stats.rewards-found", 0);
        int mmoItemsFound = config.getInt("stats.mmoitems-found", 0);
        int keysFound = config.getInt("stats.keys-found", 0);
        
        return new PlayerData(uuid, blocksMined, rewardsFound, mmoItemsFound, keysFound);
    }
    
    /**
     * Saves player data to file
     * @param uuid The player's UUID
     */
    public void savePlayerData(UUID uuid) {
        if (!playerData.containsKey(uuid)) {
            return;
        }
        
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        PlayerData data = playerData.get(uuid);
        
        // Save statistics
        config.set("stats.blocks-mined", data.getBlocksMined());
        config.set("stats.rewards-found", data.getRewardsFound());
        config.set("stats.mmoitems-found", data.getMmoItemsFound());
        config.set("stats.keys-found", data.getKeysFound());
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + uuid, e);
        }
    }
    
    /**
     * Saves all player data to files
     */
    public void saveAllPlayerData() {
        for (UUID uuid : playerData.keySet()) {
            savePlayerData(uuid);
        }
    }
    
    /**
     * Removes player data from memory (call when a player disconnects)
     * @param uuid The player's UUID
     */
    public void unloadPlayerData(UUID uuid) {
        savePlayerData(uuid);
        playerData.remove(uuid);
    }
    
    /**
     * Gets a map of statistics placeholders and values for a player
     * @param uuid The player's UUID
     * @return Map of placeholder -> value
     */
    public Map<String, String> getStatisticsPlaceholders(UUID uuid) {
        Map<String, String> placeholders = new HashMap<>();
        PlayerData data = getPlayerData(uuid);
        
        placeholders.put("blocks_mined", String.valueOf(data.getBlocksMined()));
        placeholders.put("rewards_found", String.valueOf(data.getRewardsFound()));
        placeholders.put("mmoitems_found", String.valueOf(data.getMmoItemsFound()));
        placeholders.put("keys_found", String.valueOf(data.getKeysFound()));
        
        return placeholders;
    }
    
    /**
     * Inner class to store player statistics
     */
    public class PlayerData {
        private final UUID uuid;
        private int blocksMined;
        private int rewardsFound;
        private int mmoItemsFound;
        private int keysFound;
        
        /**
         * Constructor for new player data
         * @param uuid The player's UUID
         */
        public PlayerData(UUID uuid) {
            this(uuid, 0, 0, 0, 0);
        }
        
        /**
         * Constructor for loading existing player data
         * @param uuid The player's UUID
         * @param blocksMined Number of blocks mined
         * @param rewardsFound Number of rewards found
         * @param mmoItemsFound Number of MMOItems found
         * @param keysFound Number of keys found
         */
        public PlayerData(UUID uuid, int blocksMined, int rewardsFound, int mmoItemsFound, int keysFound) {
            this.uuid = uuid;
            this.blocksMined = blocksMined;
            this.rewardsFound = rewardsFound;
            this.mmoItemsFound = mmoItemsFound;
            this.keysFound = keysFound;
        }
        
        /**
         * Increment blocks mined counter
         */
        public void incrementBlocksMined() {
            blocksMined++;
        }
        
        /**
         * Increment rewards found counter
         */
        public void incrementRewardsFound() {
            rewardsFound++;
        }
        
        /**
         * Increment MMOItems found counter
         */
        public void incrementMmoItemsFound() {
            mmoItemsFound++;
        }
        
        /**
         * Increment keys found counter
         */
        public void incrementKeysFound() {
            keysFound++;
        }
        
        // Getters
        public int getBlocksMined() {
            return blocksMined;
        }
        
        public int getRewardsFound() {
            return rewardsFound;
        }
        
        public int getMmoItemsFound() {
            return mmoItemsFound;
        }
        
        public int getKeysFound() {
            return keysFound;
        }
        
        public UUID getUuid() {
            return uuid;
        }
    }
}