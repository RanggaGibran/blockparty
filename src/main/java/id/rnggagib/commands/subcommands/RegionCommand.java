package id.rnggagib.commands.subcommands;

import id.rnggagib.BlockParty;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        
        if (args.length < 2) {
            showHelp(sender);
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "add":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(sender, "region.specify-name");
                    return;
                }
                addRegion(sender, args[2]);
                break;
                
            case "remove":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(sender, "region.specify-name");
                    return;
                }
                removeRegion(sender, args[2]);
                break;
                
            case "list":
                listRegions(sender);
                break;
                
            default:
                showHelp(sender);
                break;
        }
    }
    
    /**
     * Add a region to allowed mining regions
     * @param sender The command sender
     * @param regionName The region name
     */
    private void addRegion(CommandSender sender, String regionName) {
        boolean added = plugin.getWorldGuardManager().addAllowedRegion(regionName);
        
        if (added) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(sender, "region.added", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(sender, "region.already-exists", placeholders);
        }
    }
    
    /**
     * Remove a region from allowed mining regions
     * @param sender The command sender
     * @param regionName The region name
     */
    private void removeRegion(CommandSender sender, String regionName) {
        boolean removed = plugin.getWorldGuardManager().removeAllowedRegion(regionName);
        
        if (removed) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(sender, "region.removed", placeholders);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", regionName);
            plugin.getMessageManager().sendMessage(sender, "region.not-found", placeholders);
        }
    }
    
    /**
     * List all allowed mining regions
     * @param sender The command sender
     */
    private void listRegions(CommandSender sender) {
        Set<String> regions = plugin.getWorldGuardManager().getAllowedRegions();
        
        if (regions.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "region.no-regions");
            return;
        }
        
        plugin.getMessageManager().sendMessage(sender, "region.list-header");
        
        for (String region : regions) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("region", region);
            plugin.getMessageManager().sendCustomMessage(sender, "- <gold>{region}</gold>", false, placeholders);
        }
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
            List<String> subCommands = List.of("add", "remove", "list");
            String partial = args[1].toLowerCase();
            
            for (String cmd : subCommands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            // Tab complete with existing regions for the remove command
            String partial = args[2].toLowerCase();
            for (String region : plugin.getWorldGuardManager().getAllowedRegions()) {
                if (region.toLowerCase().startsWith(partial)) {
                    completions.add(region);
                }
            }
        }
        
        return completions;
    }
}