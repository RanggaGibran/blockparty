package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import id.rnggagib.region.Region;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages BlockParty mining regions
 */
public class RegionManager {
    private final BlockParty plugin;
    private final Map<String, Region> regions = new HashMap<>();
    private final File regionsFile;
    
    // Region selection storage (for wand usage)
    private final Map<String, Location> firstSelections = new HashMap<>();
    private final Map<String, Location> secondSelections = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public RegionManager(BlockParty plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        loadRegions();
    }
    
    /**
     * Load regions from file
     */
    public void loadRegions() {
        regions.clear();
        
        if (!regionsFile.exists()) {
            // Create empty regions file
            try {
                regionsFile.createNewFile();
                return;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create regions file", e);
                return;
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");
        
        if (regionsSection == null) {
            return;
        }
        
        for (String regionName : regionsSection.getKeys(false)) {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionName);
            if (regionSection != null) {
                Region region = new Region(regionName, regionSection);
                regions.put(regionName.toLowerCase(), region);
            }
        }
        
        plugin.getLogger().info("Loaded " + regions.size() + " mining regions.");
    }
    
    /**
     * Save regions to file
     */
    public void saveRegions() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection regionsSection = config.createSection("regions");
        
        for (Region region : regions.values()) {
            ConfigurationSection regionSection = regionsSection.createSection(region.getName());
            region.save(regionSection);
        }
        
        try {
            config.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save regions file", e);
        }
    }
    
    /**
     * Add a region
     * @param region Region to add
     */
    public void addRegion(Region region) {
        regions.put(region.getName().toLowerCase(), region);
        saveRegions();
    }
    
    /**
     * Remove a region
     * @param regionName Region name to remove
     * @return true if the region was removed
     */
    public boolean removeRegion(String regionName) {
        if (regions.remove(regionName.toLowerCase()) != null) {
            saveRegions();
            return true;
        }
        return false;
    }
    
    /**
     * Check if a region exists
     * @param regionName Region name
     * @return true if the region exists
     */
    public boolean regionExists(String regionName) {
        return regions.containsKey(regionName.toLowerCase());
    }
    
    /**
     * Get all region names
     * @return Set of region names
     */
    public Set<String> getRegionNames() {
        return new HashSet<>(regions.keySet());
    }
    
    /**
     * Check if a location is within any mining region
     * @param location Location to check
     * @return true if the location is in a mining region
     */
    public boolean isInRegion(Location location) {
        for (Region region : regions.values()) {
            if (region.contains(location)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Store first selection position
     * @param player Player making the selection
     * @param location Selected location
     */
    public void setFirstSelection(Player player, Location location) {
        firstSelections.put(player.getName(), location.clone());
    }
    
    /**
     * Store second selection position
     * @param player Player making the selection
     * @param location Selected location
     */
    public void setSecondSelection(Player player, Location location) {
        secondSelections.put(player.getName(), location.clone());
    }
    
    /**
     * Get first selection position
     * @param player Player
     * @return First selection position or null if not set
     */
    public Location getFirstSelection(Player player) {
        return firstSelections.get(player.getName());
    }
    
    /**
     * Get second selection position
     * @param player Player
     * @return Second selection position or null if not set
     */
    public Location getSecondSelection(Player player) {
        return secondSelections.get(player.getName());
    }
    
    /**
     * Clear selections for a player
     * @param player Player
     */
    public void clearSelections(Player player) {
        firstSelections.remove(player.getName());
        secondSelections.remove(player.getName());
    }
    
    /**
     * Check if a player has a complete selection
     * @param player Player
     * @return true if both first and second positions are selected
     */
    public boolean hasCompleteSelection(Player player) {
        return firstSelections.containsKey(player.getName()) && 
               secondSelections.containsKey(player.getName());
    }
    
    /**
     * Create a region from a player's selection
     * @param player Player
     * @param regionName Name for the new region
     * @return The created region or null if selection is incomplete
     */
    public Region createRegionFromSelection(Player player, String regionName) {
        if (!hasCompleteSelection(player)) {
            return null;
        }
        
        Location pos1 = firstSelections.get(player.getName());
        Location pos2 = secondSelections.get(player.getName());
        
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return null; // Selections in different worlds
        }
        
        Region region = new Region(regionName, pos1.getWorld(), pos1, pos2);
        addRegion(region);
        return region;
    }
}