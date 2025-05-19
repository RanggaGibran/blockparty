package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles message formatting and sending using MiniMessage format
 */
public class MessageManager {
    private final BlockParty plugin;
    private final MiniMessage miniMessage;
    private final BukkitAudiences adventure;
    private String prefix;
    private Map<UUID, Long> lastRegenerationMessage = new HashMap<>();
    private static final int REGENERATION_MESSAGE_COOLDOWN = 3; // seconds
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public MessageManager(BlockParty plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.adventure = BukkitAudiences.create(plugin);
        loadPrefix();
    }
    
    /**
     * Load the prefix from the configuration
     */
    private void loadPrefix() {
        FileConfiguration messages = plugin.getConfigManager().getMessages();
        this.prefix = messages.getString("prefix", "<dark_gray>[<gold>BlockParty</gold>]</dark_gray> ");
    }
    
    /**
     * Get a message from the configuration
     * @param path The path to the message
     * @return The message, or an error message if not found
     */
    public String getMessage(String path) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();
        return messages.getString("messages." + path, "<red>Missing message: " + path + "</red>");
    }
    
    /**
     * Format a message with MiniMessage
     * @param message The message to format
     * @return The formatted Component
     */
    public Component formatMessage(String message) {
        return miniMessage.deserialize(message);
    }
    
    /**
     * Replace placeholders in messages
     * @param message The message to replace placeholders in
     * @param placeholders Map of placeholders and their values
     * @return The message with replaced placeholders
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }
        
        String result = message;
        
        if (placeholders != null) {
            // Replace both {placeholder} and %placeholder% formats for compatibility
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Replace {placeholder} format
                result = result.replace("{" + key + "}", value);
                
                // Replace %placeholder% format for backward compatibility
                result = result.replace("%" + key + "%", value);
            }
        }
        
        return result;
    }
    
    /**
     * Send a message to a player or command sender
     * @param sender The recipient
     * @param path The message path
     * @param addPrefix Whether to add the prefix
     * @param placeholders Placeholders to replace
     */
    public void sendMessage(CommandSender sender, String path, boolean addPrefix, Map<String, String> placeholders) {
        String message = getMessage(path);
        message = replacePlaceholders(message, placeholders);
        
        if (addPrefix) {
            message = prefix + message;
        }
        
        Component component = formatMessage(message);
        
        if (sender instanceof Player player) {
            adventure.player(player).sendMessage(component);
        } else {
            // Convert to legacy format for console
            sender.sendMessage(stripMiniMessage(message));
        }
    }
    
    /**
     * Send a message to a player or command sender with prefix
     * @param sender The recipient
     * @param path The message path
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, true, null);
    }
    
    /**
     * Send a message to a player or command sender with prefix and placeholders
     * @param sender The recipient
     * @param path The message path
     * @param placeholders Placeholders to replace
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sendMessage(sender, path, true, placeholders);
    }
    
    /**
     * Send a message to a player or command sender without prefix
     * @param sender The recipient
     * @param path The message path
     * @param placeholders Placeholders to replace
     */
    public void sendMessageWithoutPrefix(CommandSender sender, String path, Map<String, String> placeholders) {
        sendMessage(sender, path, false, placeholders);
    }
    
    /**
     * Send a custom message to a player or command sender
     * @param sender The recipient
     * @param message The message
     * @param addPrefix Whether to add the prefix
     * @param placeholders Placeholders to replace
     */
    public void sendCustomMessage(CommandSender sender, String message, boolean addPrefix, Map<String, String> placeholders) {
        String formattedMessage = replacePlaceholders(message, placeholders);
        
        if (addPrefix) {
            formattedMessage = prefix + formattedMessage;
        }
        
        Component component = formatMessage(formattedMessage);
        
        if (sender instanceof Player player) {
            adventure.player(player).sendMessage(component);
        } else {
            // Convert to legacy format for console
            sender.sendMessage(stripMiniMessage(formattedMessage));
        }
    }
    
    /**
     * Send an action bar message to a player
     * @param player The player
     * @param path The message path
     * @param placeholders Placeholders to replace
     */
    public void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        message = replacePlaceholders(message, placeholders);
        Component component = formatMessage(message);
        
        adventure.player(player).sendActionBar(component);
    }
    
    /**
     * Send a title to a player
     * @param player The player
     * @param titlePath The title message path
     * @param subtitlePath The subtitle message path
     * @param fadeIn The fade in time in ticks
     * @param stay The stay time in ticks
     * @param fadeOut The fade out time in ticks
     * @param placeholders Placeholders to replace
     */
    public void sendTitle(Player player, String titlePath, String subtitlePath, int fadeIn, int stay, int fadeOut, Map<String, String> placeholders) {
        String titleMessage = getMessage(titlePath);
        String subtitleMessage = getMessage(subtitlePath);
        
        titleMessage = replacePlaceholders(titleMessage, placeholders);
        subtitleMessage = replacePlaceholders(subtitleMessage, placeholders);
        
        Component title = formatMessage(titleMessage);
        Component subtitle = formatMessage(subtitleMessage);
        
        Title titleObj = Title.title(
            title, 
            subtitle,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)
            )
        );
        
        adventure.player(player).showTitle(titleObj);
    }
    
    /**
     * Create a map for placeholders
     * @return A new HashMap for placeholders
     */
    public Map<String, String> createPlaceholderMap() {
        return new HashMap<>();
    }
    
    /**
     * Strips MiniMessage formatting for console output
     * @param input The input string with MiniMessage formatting
     * @return Plain text string
     */
    private String stripMiniMessage(String input) {
        // Simple pattern to remove MiniMessage tags
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("");
    }
    
    /**
     * Send a block regeneration message to a player with cooldown
     * @param player The player
     */
    public void sendRegenerationMessage(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis() / 1000;
        
        // Check if we should send the message based on cooldown
        if (!lastRegenerationMessage.containsKey(uuid) || 
                (currentTime - lastRegenerationMessage.get(uuid) >= REGENERATION_MESSAGE_COOLDOWN)) {
            // Send the message and update the timestamp
            sendMessage(player, "mining.block-regenerate");
            lastRegenerationMessage.put(uuid, currentTime);
        }
    }
}