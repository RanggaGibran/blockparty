package id.rnggagib.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import id.rnggagib.BlockParty;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles WorldGuard integration for region protection
 */
public class WorldGuardManager {
    private final BlockParty plugin;
    private boolean worldGuardEnabled;
    private boolean useWorldGuard;
    private Set<String> allowedRegions;
    private Set<String> deniedRegions;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public WorldGuardManager(BlockParty plugin) {
        this.plugin = plugin;
        this.worldGuardEnabled = false;
        this.allowedRegions = new HashSet<>();
        this.deniedRegions = new HashSet<>();
        loadConfiguration();
        setupWorldGuard();
    }
    
    /**
     * Load WorldGuard configuration from config.yml
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        useWorldGuard = config.getBoolean("worldguard.enabled", true);
        
        // Load allowed regions
        List<String> allowedRegionsList = config.getStringList("worldguard.allowed-regions");
        allowedRegions.clear();
        allowedRegions.addAll(allowedRegionsList);
        
        // Load denied regions
        List<String> deniedRegionsList = config.getStringList("worldguard.denied-regions");
        deniedRegions.clear();
        deniedRegions.addAll(deniedRegionsList);
    }
    
    /**
     * Setup WorldGuard integration if available
     */
    private void setupWorldGuard() {
        Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuardPlugin != null && worldGuardPlugin instanceof WorldGuardPlugin) {
            worldGuardEnabled = true;
            plugin.getLogger().info("WorldGuard found, enabling region protection support.");
        } else {
            worldGuardEnabled = false;
            plugin.getLogger().warning("WorldGuard not found, region protection will be disabled.");
        }
    }
    
    /**
     * Reload the WorldGuard configuration
     */
    public void reload() {
        loadConfiguration();
    }
    
    /**
     * Check if a player can use Block Party at a specific location
     * @param player The player
     * @param location The location
     * @return True if allowed, false if denied
     */
    public boolean canUseBlockParty(Player player, Location location) {
        // If WorldGuard is disabled in config or not found, always allow
        if (!useWorldGuard || !worldGuardEnabled) {
            return true;
        }
        
        // If player has bypass permission, allow
        if (player.hasPermission("blockparty.bypass.region")) {
            return true;
        }
        
        // Get regions at the player's location
        Set<String> regions = getRegionsAt(location);
        
        // Check for denied regions first (denial takes priority)
        for (String region : regions) {
            if (deniedRegions.contains(region)) {
                return false;
            }
        }
        
        // If allowed regions list is empty, allow in any non-denied region
        if (allowedRegions.isEmpty()) {
            return true;
        }
        
        // Check if player is in any allowed region
        for (String region : regions) {
            if (allowedRegions.contains(region)) {
                return true;
            }
        }
        
        // If we have an allowed regions list but player isn't in any, deny
        return false;
    }
    
    /**
     * Get all region IDs at a location
     * @param location The location to check
     * @return Set of region IDs at the location
     */
    private Set<String> getRegionsAt(Location location) {
        Set<String> regions = new HashSet<>();
        
        if (!worldGuardEnabled) {
            return regions;
        }
        
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet regionSet = query.getApplicableRegions(
            BukkitAdapter.adapt(location)
        );
        
        for (ProtectedRegion region : regionSet) {
            regions.add(region.getId());
        }
        
        return regions;
    }
    
    /**
     * Check if WorldGuard integration is enabled
     * @return True if enabled, false if disabled
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled && useWorldGuard;
    }
    
    /**
     * Add a region to the allowed regions list
     * @param regionName The region name to add
     * @return True if the region was added, false if it already exists
     */
    public boolean addAllowedRegion(String regionName) {
        if (allowedRegions.contains(regionName)) {
            return false;
        }
        
        allowedRegions.add(regionName);
        saveRegions();
        return true;
    }

    /**
     * Remove a region from the allowed regions list
     * @param regionName The region name to remove
     * @return True if the region was removed, false if not found
     */
    public boolean removeAllowedRegion(String regionName) {
        boolean removed = allowedRegions.remove(regionName);
        
        if (removed) {
            saveRegions();
        }
        
        return removed;
    }

    /**
     * Get all allowed mining regions
     * @return Set of allowed region names
     */
    public Set<String> getAllowedRegions() {
        return new HashSet<>(allowedRegions);
    }

    /**
     * Save regions to the configuration file
     */
    private void saveRegions() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        config.set("worldguard.allowed-regions", new ArrayList<>(allowedRegions));
        plugin.getConfigManager().saveMainConfig();
    }
}