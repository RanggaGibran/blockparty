package id.rnggagib.utils;

import id.rnggagib.BlockParty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and validates the region selection wand
 */
public class SelectionWand {
    private final BlockParty plugin;
    private final NamespacedKey wandKey;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public SelectionWand(BlockParty plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "region_wand");
    }
    
    /**
     * Create a region selection wand item
     * @return The wand item
     */
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        
        // Set name and lore
        meta.setDisplayName(MiniMessage.miniMessage().stripTags("<gradient:gold:yellow>Region Selection Wand</gradient>"));
        
        List<String> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().stripTags("<gray>Left-click to select first position</gray>"));
        lore.add(MiniMessage.miniMessage().stripTags("<gray>Right-click to select second position</gray>"));
        lore.add(MiniMessage.miniMessage().stripTags("<yellow>For BlockParty mining regions</yellow>"));
        meta.setLore(lore);
        
        // Add enchant glow effect
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Add persistent data to identify this as a region wand
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        
        wand.setItemMeta(meta);
        return wand;
    }
    
    /**
     * Check if an item is a region selection wand
     * @param item The item to check
     * @return true if the item is a region wand
     */
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }
    
    /**
     * Get the namespaced key for the wand
     * @return The key
     */
    public NamespacedKey getWandKey() {
        return wandKey;
    }
}