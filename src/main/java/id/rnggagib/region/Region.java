package id.rnggagib.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents a mining region for BlockParty
 */
public class Region {
    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    /**
     * Create a new region
     * @param name Region name
     * @param world World
     * @param pos1 First corner
     * @param pos2 Second corner
     */
    public Region(String name, World world, Location pos1, Location pos2) {
        this.name = name;
        this.worldName = world.getName();
        
        // Ensure min is always smaller than max
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }
    
    /**
     * Load a region from configuration
     * @param name Region name
     * @param section Configuration section
     */
    public Region(String name, ConfigurationSection section) {
        this.name = name;
        this.worldName = section.getString("world");
        this.minX = section.getInt("min.x");
        this.minY = section.getInt("min.y");
        this.minZ = section.getInt("min.z");
        this.maxX = section.getInt("max.x");
        this.maxY = section.getInt("max.y");
        this.maxZ = section.getInt("max.z");
    }
    
    /**
     * Check if a location is within this region
     * @param location Location to check
     * @return true if the location is in this region
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        return x >= minX && x <= maxX 
            && y >= minY && y <= maxY 
            && z >= minZ && z <= maxZ;
    }
    
    /**
     * Save this region to configuration
     * @param section Configuration section
     */
    public void save(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("min.x", minX);
        section.set("min.y", minY);
        section.set("min.z", minZ);
        section.set("max.x", maxX);
        section.set("max.y", maxY);
        section.set("max.z", maxZ);
    }
    
    /**
     * Get the name of this region
     * @return Region name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the world name of this region
     * @return World name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Get the volume of this region in blocks
     * @return Volume
     */
    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
    
    /**
     * Get a string representation of this region
     */
    @Override
    public String toString() {
        return name + " (" + worldName + ": " + minX + "," + minY + "," + minZ + " to " + maxX + "," + maxY + "," + maxZ + ")";
    }
}