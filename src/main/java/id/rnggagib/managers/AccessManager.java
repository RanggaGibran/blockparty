package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the creation and validation of Block Party access items
 */
public class AccessManager {
    private final BlockParty plugin;
    private final NamespacedKey accessKey;
    private final MiniMessage miniMessage;
    
    // Configuration settings
    private Material accessMaterial;
    private int customModelData;
    private String accessName;
    private List<String> accessLore;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public AccessManager(BlockParty plugin) {
        this.plugin = plugin;
        this.accessKey = new NamespacedKey(plugin, "blockparty_access");
        this.miniMessage = MiniMessage.miniMessage();
        loadConfiguration();
    }
    
    /**
     * Loads access item configuration from config.yml
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        
        // Load material
        String materialName = config.getString("access-item.material", "DIAMOND_PICKAXE");
        try {
            this.accessMaterial = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            this.accessMaterial = Material.DIAMOND_PICKAXE;
            plugin.getLogger().warning("Invalid material in config: " + materialName + ". Using DIAMOND_PICKAXE instead.");
        }
        
        // Load custom model data
        this.customModelData = config.getInt("access-item.custom-model-data", 1001);
        
        // Load name
        this.accessName = config.getString("access-item.name", "<gold>Block Party Access</gold>");
        
        // Load lore
        this.accessLore = config.getStringList("access-item.lore");
        if (this.accessLore.isEmpty()) {
            this.accessLore = new ArrayList<>();
            this.accessLore.add("<gray>Right-click to activate Block Party</gray>");
            this.accessLore.add("<gray>Grants 5 minutes of mining time</gray>");
        }
    }
    
    /**
     * Reloads the access item configuration
     */
    public void reload() {
        loadConfiguration();
    }
    
    /**
     * Creates a Block Party access item
     * @param amount The amount of items to create
     * @return The created ItemStack
     */
    public ItemStack createAccessItem(int amount) {
        ItemStack item = new ItemStack(accessMaterial, amount);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with MiniMessage formatting
        meta.setDisplayName(MiniMessage.miniMessage().stripTags(accessName));
        
        // Set custom model data
        meta.setCustomModelData(customModelData);
        
        // Set lore with MiniMessage formatting
        List<String> formattedLore = new ArrayList<>();
        for (String line : accessLore) {
            formattedLore.add(MiniMessage.miniMessage().stripTags(line));
        }
        meta.setLore(formattedLore);
        
        // Add enchant glow effect
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Add persistent data to identify this as a Block Party access item
        meta.getPersistentDataContainer().set(accessKey, PersistentDataType.BYTE, (byte) 1);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Checks if an item is a Block Party access item
     * @param item The item to check
     * @return True if the item is a Block Party access item
     */
    public boolean isAccessItem(ItemStack item) {
        if (item == null || item.getType() != accessMaterial) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check for the persistent data that identifies this as a Block Party access item
        return meta.getPersistentDataContainer().has(accessKey, PersistentDataType.BYTE);
    }
    
    /**
     * Gives a player Block Party access items
     * @param player The player to give the items to
     * @param amount The amount of items to give
     */
    public void giveAccessItem(Player player, int amount) {
        ItemStack accessItem = createAccessItem(amount);
        player.getInventory().addItem(accessItem);
    }
    
    /**
     * Gets the NamespacedKey used to identify Block Party access items
     * @return The NamespacedKey
     */
    public NamespacedKey getAccessKey() {
        return accessKey;
    }
}