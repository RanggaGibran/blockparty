package id.rnggagib;

import id.rnggagib.commands.CommandManager;
import id.rnggagib.config.ConfigManager;
import id.rnggagib.listeners.BlockBreakListener;
import id.rnggagib.listeners.PlayerInteractListener;
import id.rnggagib.listeners.PlayerJoinListener;
import id.rnggagib.listeners.PlayerQuitListener;
import id.rnggagib.managers.AccessManager;
import id.rnggagib.managers.MessageManager;
import id.rnggagib.managers.MiningSessionManager;
import id.rnggagib.managers.RewardManager;
import id.rnggagib.managers.PlayerDataManager;
import id.rnggagib.managers.WorldGuardManager;
import id.rnggagib.managers.BlockRegenerationManager;
import id.rnggagib.placeholder.PlaceholderManager;

import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the BlockParty plugin
 */
public class BlockParty extends JavaPlugin {
    // Static instance for easy access
    private static BlockParty instance;
    private static final Logger LOGGER = Logger.getLogger("BlockParty");
    
    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private AccessManager accessManager;
    private MiningSessionManager sessionManager;
    private RewardManager rewardManager;
    private PlayerDataManager playerDataManager;
    private WorldGuardManager worldGuardManager;
    private BlockRegenerationManager blockRegenerationManager;
    private PlaceholderManager placeholderManager;
    
    // Command handler
    private CommandManager commandManager;
    
    @Override
    public void onEnable() {
        // Set instance
        instance = this;
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Initialize managers
        messageManager = new MessageManager(this);
        playerDataManager = new PlayerDataManager(this);
        worldGuardManager = new WorldGuardManager(this);
        accessManager = new AccessManager(this);
        sessionManager = new MiningSessionManager(this);
        rewardManager = new RewardManager(this);
        blockRegenerationManager = new BlockRegenerationManager(this);
        
        // Register events
        registerListeners();
        
        // Register commands
        commandManager = new CommandManager(this);
        getCommand("blockparty").setExecutor(commandManager);
        
        // Setup PlaceholderAPI integration if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            LOGGER.info("PlaceholderAPI found, registering placeholders");
            placeholderManager = new PlaceholderManager(this);
            placeholderManager.register();
        } else {
            LOGGER.info("PlaceholderAPI not found, placeholders won't be available");
        }
        
        LOGGER.info("BlockParty has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save player data if enabled
        if (configManager.getConfig().getBoolean("statistics.save-on-disable")) {
            playerDataManager.saveAllPlayerData();
        }
        
        // Cancel any active sessions
        sessionManager.cancelAllSessions();
        
        // Cancel block regeneration tasks
        blockRegenerationManager.cancelAllTasks();
        
        LOGGER.info("BlockParty has been disabled!");
    }
    
    /**
     * Register all event listeners
     */
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
    }
    
    /**
     * Get the plugin instance
     * @return BlockParty instance
     */
    public static BlockParty getInstance() {
        return instance;
    }
    
    // Manager getters
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public AccessManager getAccessManager() {
        return accessManager;
    }
    
    public MiningSessionManager getSessionManager() {
        return sessionManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }
    
    /**
     * Get the block regeneration manager
     * @return BlockRegenerationManager instance
     */
    public BlockRegenerationManager getBlockRegenerationManager() {
        return blockRegenerationManager;
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reload() {
        // Cancel active sessions
        sessionManager.cancelAllSessions();
        
        // Reload configurations
        configManager.reloadConfigs();
        
        // Reinitialize managers that need reloading
        rewardManager.reload();
        accessManager.reload();
        blockRegenerationManager.reload();
    }
}
