package id.rnggagib.commands.subcommands;

import id.rnggagib.BlockParty;
import id.rnggagib.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Region command for managing BlockParty mining regions
 */
public class RegionCommand implements SubCommand {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public RegionCommand(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "region";
    }
    
    @Override
    public String getPermission() {
        return "blockparty.admin.region";
    }
    
    @Override
    public String[] getAliases() {
        return new String[] { "regions" };
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "plugin.player-only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            showHelp(sender);
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "wand":
                giveWand(player);
                break;
                
            case "create":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(player, "region.specify-name");
                    return;
                }
                createRegion(player, args[2]);
                break;
                
            case "remove":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(player, "region.specify-name");
                    return;
                }
                removeRegion(player, args[2]);
                break;
                
            case "list":
                listRegions(player);
                break;
                
            case "info":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(player, "region.specify-name");
                    return;
                }
                showRegionInfo(player, args[2]);
                break;
                
            default:
                showHelp(sender);
                break;
        }
    }
    
    /**
     * Give the region selection wand to a player
     * @param player The player
     */
    private void giveWand(Player player) {
        ItemStack wand = plugin.getSelectionWand().createWand();
        player.getInventory().addItem(wand);
        plugin.getMessageManager().sendMessage(player, "region.wand-given");
    }
    
    /**
     * Create a region from the player's selection
     * @param player The player
     * @param regionName The region name
     */
    private void createRegion(Player player, String regionName) {
        // Check if selection is complete
        if (!plugin.getRegionManager().hasCompleteSelection(player)) {
            plugin.getMessageManager().sendMessage(player, "region.incomplete-selection");
            return;
        }
        
        // Check if region already exists
        if (plugin.getRegionManager().regionExists(regionName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(player, "region.already-exists", placeholders);
            return;
        }
        
        // Create the region
        Region region = plugin.getRegionManager().createRegionFromSelection(player, regionName);
        
        if (region != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            placeholders.put("volume", String.valueOf(region.getVolume()));
            plugin.getMessageManager().sendMessage(player, "region.created", placeholders);
        } else {
            plugin.getMessageManager().sendMessage(player, "region.create-error");
        }
    }
    
    /**
     * Remove a region
     * @param player The player
     * @param regionName The region name
     */
    private void removeRegion(Player player, String regionName) {
        if (!plugin.getRegionManager().regionExists(regionName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(player, "region.not-found", placeholders);
            return;
        }
        
        if (plugin.getRegionManager().removeRegion(regionName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(player, "region.removed", placeholders);
        } else {
            plugin.getMessageManager().sendMessage(player, "region.remove-error");
        }
    }
    
    /**
     * List all regions
     * @param player The player
     */
    private void listRegions(Player player) {
        Set<String> regions = plugin.getRegionManager().getRegionNames();
        
        if (regions.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "region.no-regions");
            return;
        }
        
        plugin.getMessageManager().sendMessage(player, "region.list-header");
        
        for (String region : regions) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", region);
            plugin.getMessageManager().sendCustomMessage(player, "- <gold>{region}</gold>", false, placeholders);
        }
    }
    
    /**
     * Show info about a region
     * @param player The player
     * @param regionName The region name
     */
    private void showRegionInfo(Player player, String regionName) {
        // This would need additional methods in RegionManager to get region info
        // For now, just show a placeholder message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("region", regionName);
        plugin.getMessageManager().sendMessage(player, "region.info", placeholders);
    }
    
    /**
     * Show help for the region command
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        plugin.getMessageManager().sendMessageWithoutPrefix(sender, "region.help", null);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            List<String> subCommands = List.of("wand", "create", "remove", "list", "info");
            String partial = args[1].toLowerCase();
            
            for (String cmd : subCommands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3 && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("info"))) {
            // Tab complete with existing regions for remove/info commands
            String partial = args[2].toLowerCase();
            for (String region : plugin.getRegionManager().getRegionNames()) {
                if (region.toLowerCase().startsWith(partial)) {
                    completions.add(region);
                }
            }
        }
        
        return completions;
    }
}