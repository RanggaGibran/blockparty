package id.rnggagib.config;

import id.rnggagib.BlockParty;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Handles loading and managing all configuration files
 */
public class ConfigManager {
    private final BlockParty plugin;
    
    // Configuration files
    private File configFile;
    private File blocksFile;
    private File messagesFile;
    
    // FileConfigurations
    private FileConfiguration config;
    private FileConfiguration blocks;
    private FileConfiguration messages;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public ConfigManager(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        // Create plugin directory if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load main config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load blocks.yml
        blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (!blocksFile.exists()) {
            plugin.saveResource("blocks.yml", false);
        }
        blocks = YamlConfiguration.loadConfiguration(blocksFile);
        
        // Load messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadConfigs() {
        // Reload main config
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Reload blocks config
        blocks = YamlConfiguration.loadConfiguration(blocksFile);
        
        // Reload messages config
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Save a configuration file
     * @param config The configuration to save
     * @param file The file to save to
     */
    public void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + file, e);
        }
    }
    
    /**
     * Get the main configuration
     * @return The main configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Get the blocks configuration
     * @return The blocks configuration
     */
    public FileConfiguration getBlocks() {
        return blocks;
    }
    
    /**
     * Get the messages configuration
     * @return The messages configuration
     */
    public FileConfiguration getMessages() {
        return messages;
    }
    
    /**
     * Save the main configuration
     */
    public void saveMainConfig() {
        saveConfig(config, configFile);
    }
    
    /**
     * Save the blocks configuration
     */
    public void saveBlocksConfig() {
        saveConfig(blocks, blocksFile);
    }
    
    /**
     * Save the messages configuration
     */
    public void saveMessagesConfig() {
        saveConfig(messages, messagesFile);
    }
}