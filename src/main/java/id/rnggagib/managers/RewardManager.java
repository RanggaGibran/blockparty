package id.rnggagib.managers;

import id.rnggagib.BlockParty;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Manages rewards for Block Party plugin
 */
public class RewardManager {
    private final BlockParty plugin;
    private final Random random;
    
    // Minable blocks configuration
    private final Map<Material, BlockConfig> minableBlocks;
    
    // Reward type configurations
    private boolean mmoItemsEnabled;
    private double mmoItemsChance;
    private List<MMOItemReward> mmoItemRewards;
    
    private boolean crateKeysEnabled;
    private double crateKeysChance;
    private List<CrateKeyReward> crateKeyRewards;
    
    private boolean vanillaItemsEnabled;
    private double vanillaItemsChance;
    private List<VanillaItemReward> vanillaItemRewards;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public RewardManager(BlockParty plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.minableBlocks = new HashMap<>();
        this.mmoItemRewards = new ArrayList<>();
        this.crateKeyRewards = new ArrayList<>();
        this.vanillaItemRewards = new ArrayList<>();
        
        loadConfiguration();
    }
    
    /**
     * Load configuration from blocks.yml
     */
    private void loadConfiguration() {
        FileConfiguration blocksConfig = plugin.getConfigManager().getBlocks();
        
        // Load minable blocks
        minableBlocks.clear();
        ConfigurationSection blocksSection = blocksConfig.getConfigurationSection("minable-blocks");
        if (blocksSection != null) {
            Set<String> blockKeys = blocksSection.getKeys(false);
            for (String key : blockKeys) {
                try {
                    Material material = Material.valueOf(key);
                    boolean enabled = blocksSection.getBoolean(key + ".enabled", true);
                    double rewardChance = blocksSection.getDouble(key + ".reward-chance", 0.5);
                    boolean dropVanilla = blocksSection.getBoolean(key + ".drop-vanilla", false);
                    
                    minableBlocks.put(material, new BlockConfig(enabled, rewardChance, dropVanilla));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in blocks.yml: " + key);
                }
            }
        }
        
        // Load MMOItems rewards
        ConfigurationSection mmoItemsSection = blocksConfig.getConfigurationSection("rewards.mmoitems");
        mmoItemsEnabled = mmoItemsSection != null && mmoItemsSection.getBoolean("enabled", true);
        mmoItemsChance = mmoItemsSection != null ? mmoItemsSection.getDouble("chance", 0.3) : 0.3;
        
        mmoItemRewards.clear();
        if (mmoItemsEnabled && mmoItemsSection != null) {
            List<Map<?, ?>> items = mmoItemsSection.getMapList("items");
            for (Map<?, ?> item : items) {
                String type = item.get("type").toString();
                String id = item.get("id").toString();
                double chance = Double.parseDouble(item.get("chance").toString());
                
                mmoItemRewards.add(new MMOItemReward(type, id, chance));
            }
        }
        
        // Load crate key rewards
        ConfigurationSection crateKeysSection = blocksConfig.getConfigurationSection("rewards.crate-keys");
        crateKeysEnabled = crateKeysSection != null && crateKeysSection.getBoolean("enabled", true);
        crateKeysChance = crateKeysSection != null ? crateKeysSection.getDouble("chance", 0.2) : 0.2;
        
        crateKeyRewards.clear();
        if (crateKeysEnabled && crateKeysSection != null) {
            List<Map<?, ?>> keys = crateKeysSection.getMapList("keys");
            for (Map<?, ?> key : keys) {
                String name = key.get("name").toString();
                String command = key.get("command").toString();
                double chance = Double.parseDouble(key.get("chance").toString());
                String message = key.get("message").toString();
                
                crateKeyRewards.add(new CrateKeyReward(name, command, chance, message));
            }
        }
        
        // Load vanilla item rewards
        ConfigurationSection vanillaItemsSection = blocksConfig.getConfigurationSection("rewards.vanilla-items");
        vanillaItemsEnabled = vanillaItemsSection != null && vanillaItemsSection.getBoolean("enabled", true);
        vanillaItemsChance = vanillaItemsSection != null ? vanillaItemsSection.getDouble("chance", 0.5) : 0.5;
        
        vanillaItemRewards.clear();
        if (vanillaItemsEnabled && vanillaItemsSection != null) {
            List<Map<?, ?>> items = vanillaItemsSection.getMapList("items");
            for (Map<?, ?> item : items) {
                String materialName = item.get("material").toString();
                int minAmount = Integer.parseInt(item.get("min-amount").toString());
                int maxAmount = Integer.parseInt(item.get("max-amount").toString());
                double chance = Double.parseDouble(item.get("chance").toString());
                
                try {
                    Material material = Material.valueOf(materialName);
                    vanillaItemRewards.add(new VanillaItemReward(material, minAmount, maxAmount, chance));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in vanilla rewards: " + materialName);
                }
            }
        }
    }
    
    /**
     * Reload the configuration
     */
    public void reload() {
        loadConfiguration();
    }
    
    /**
     * Check if a block is minable in BlockParty
     * @param material The block material
     * @return True if the block is minable
     */
    public boolean isMinableBlock(Material material) {
        return minableBlocks.containsKey(material) && minableBlocks.get(material).isEnabled();
    }
    
    /**
     * Check if a block should drop its vanilla drops
     * @param material The block material
     * @return True if the block should drop its vanilla drops
     */
    public boolean shouldDropVanilla(Material material) {
        return minableBlocks.containsKey(material) && minableBlocks.get(material).isDropVanilla();
    }
    
    /**
     * Check if a block should give a reward
     * @param material The block material
     * @return True if the block should give a reward
     */
    public boolean shouldGiveReward(Material material) {
        if (!minableBlocks.containsKey(material)) {
            return false;
        }
        
        double chance = minableBlocks.get(material).getRewardChance();
        return random.nextDouble() < chance;
    }
    
    /**
     * Give a random reward to a player
     * @param player The player to give the reward to
     * @return True if a reward was given
     */
    public boolean giveRandomReward(Player player) {
        // Calculate chance for each reward type based on their relative weights
        double totalChance = 0;
        if (mmoItemsEnabled) totalChance += mmoItemsChance;
        if (crateKeysEnabled) totalChance += crateKeysChance;
        if (vanillaItemsEnabled) totalChance += vanillaItemsChance;
        
        if (totalChance <= 0) {
            return false;
        }
        
        double roll = random.nextDouble() * totalChance;
        double currentChance = 0;
        
        // Increment player stats
        plugin.getPlayerDataManager().getPlayerData(player).incrementRewardsFound();
        
        // Give MMOItems reward
        if (mmoItemsEnabled && roll < (currentChance += mmoItemsChance)) {
            return giveMmoItemReward(player);
        }
        
        // Give crate key reward
        if (crateKeysEnabled && roll < (currentChance += crateKeysChance)) {
            return giveCrateKeyReward(player);
        }
        
        // Give vanilla item reward
        if (vanillaItemsEnabled) {
            return giveVanillaItemReward(player);
        }
        
        return false;
    }
    
    /**
     * Give an MMOItem reward to a player
     * @param player The player
     * @return True if successful
     */
    private boolean giveMmoItemReward(Player player) {
        if (mmoItemRewards.isEmpty() || !isMMOItemsAvailable()) {
            return false;
        }
        
        // Select a random MMOItem based on weights
        MMOItemReward reward = selectRandomReward(mmoItemRewards);
        if (reward == null) {
            return false;
        }
        
        try {
            // Get type from string
            Type type = Type.get(reward.getType());
            if (type == null) {
                plugin.getLogger().warning("Invalid MMOItems type: " + reward.getType());
                return false;
            }

            // Create MMOItem using the updated API
            MMOItem mmoItem = MMOItems.plugin.getMMOItem(type, reward.getId());
            if (mmoItem == null) {
                plugin.getLogger().warning("Invalid MMOItems id: " + reward.getId());
                return false;
            }

            // Convert to ItemStack and give to player
            ItemStack item = mmoItem.newBuilder().build();
            player.getInventory().addItem(item);

            // Increment MMOItems found counter
            plugin.getPlayerDataManager().getPlayerData(player).incrementMmoItemsFound();

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error giving MMOItem: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if MMOItems is available
     * @return True if MMOItems is available
     */
    private boolean isMMOItemsAvailable() {
        return Bukkit.getPluginManager().getPlugin("MMOItems") != null;
    }
    
    /**
     * Give a crate key reward to a player
     * @param player The player
     * @return True if successful
     */
    private boolean giveCrateKeyReward(Player player) {
        if (crateKeyRewards.isEmpty()) {
            return false;
        }
        
        // Select a random crate key based on weights
        CrateKeyReward reward = selectRandomReward(crateKeyRewards);
        if (reward == null) {
            return false;
        }
        
        // Execute command with player placeholder
        String command = reward.getCommand().replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        
        // Send message to player
        plugin.getMessageManager().sendCustomMessage(player, reward.getMessage(), true, null);
        
        // Increment keys found counter
        plugin.getPlayerDataManager().getPlayerData(player).incrementKeysFound();
        
        return true;
    }
    
    /**
     * Give a vanilla item reward to a player
     * @param player The player
     * @return True if successful
     */
    private boolean giveVanillaItemReward(Player player) {
        if (vanillaItemRewards.isEmpty()) {
            return false;
        }
        
        // Select a random vanilla item based on weights
        VanillaItemReward reward = selectRandomReward(vanillaItemRewards);
        if (reward == null) {
            return false;
        }
        
        // Calculate random amount within range
        int amount = reward.getMinAmount();
        if (reward.getMaxAmount() > reward.getMinAmount()) {
            amount += random.nextInt(reward.getMaxAmount() - reward.getMinAmount() + 1);
        }
        
        // Create and give item
        ItemStack item = new ItemStack(reward.getMaterial(), amount);
        player.getInventory().addItem(item);
        
        return true;
    }
    
    /**
     * Select a random reward based on weights
     * @param <T> The reward type
     * @param rewards List of rewards
     * @return Selected reward or null if list is empty
     */
    private <T extends WeightedReward> T selectRandomReward(List<T> rewards) {
        if (rewards.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        double totalWeight = 0;
        for (T reward : rewards) {
            totalWeight += reward.getChance();
        }
        
        // Roll random number
        double roll = random.nextDouble() * totalWeight;
        
        // Find selected reward
        double currentWeight = 0;
        for (T reward : rewards) {
            currentWeight += reward.getChance();
            if (roll < currentWeight) {
                return reward;
            }
        }
        
        // Fallback to first reward
        return rewards.get(0);
    }
    
    /**
     * Block configuration class
     */
    private static class BlockConfig {
        private final boolean enabled;
        private final double rewardChance;
        private final boolean dropVanilla;
        
        public BlockConfig(boolean enabled, double rewardChance, boolean dropVanilla) {
            this.enabled = enabled;
            this.rewardChance = rewardChance;
            this.dropVanilla = dropVanilla;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public double getRewardChance() {
            return rewardChance;
        }
        
        public boolean isDropVanilla() {
            return dropVanilla;
        }
    }
    
    /**
     * Interface for weighted rewards
     */
    private interface WeightedReward {
        double getChance();
    }
    
    /**
     * MMOItem reward class
     */
    private static class MMOItemReward implements WeightedReward {
        private final String type;
        private final String id;
        private final double chance;
        
        public MMOItemReward(String type, String id, double chance) {
            this.type = type;
            this.id = id;
            this.chance = chance;
        }
        
        public String getType() {
            return type;
        }
        
        public String getId() {
            return id;
        }
        
        @Override
        public double getChance() {
            return chance;
        }
    }
    
    /**
     * Crate key reward class
     */
    private static class CrateKeyReward implements WeightedReward {
        private final String name;
        private final String command;
        private final double chance;
        private final String message;
        
        public CrateKeyReward(String name, String command, double chance, String message) {
            this.name = name;
            this.command = command;
            this.chance = chance;
            this.message = message;
        }
        
        public String getName() {
            return name;
        }
        
        public String getCommand() {
            return command;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public double getChance() {
            return chance;
        }
    }
    
    /**
     * Vanilla item reward class
     */
    private static class VanillaItemReward implements WeightedReward {
        private final Material material;
        private final int minAmount;
        private final int maxAmount;
        private final double chance;
        
        public VanillaItemReward(Material material, int minAmount, int maxAmount, double chance) {
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = chance;
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public int getMinAmount() {
            return minAmount;
        }
        
        public int getMaxAmount() {
            return maxAmount;
        }
        
        @Override
        public double getChance() {
            return chance;
        }
    }
}