package id.rnggagib.commands.subcommands;

import id.rnggagib.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stats command implementation
 */
public class StatsCommand implements SubCommand {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public StatsCommand(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "stats";
    }
    
    @Override
    public String getPermission() {
        return "blockparty.stats";
    }
    
    @Override
    public String[] getAliases() {
        return new String[] { "stat", "statistics" };
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check if sender is a player or if a player is specified
        Player target;
        
        if (args.length >= 2) {
            // Check if viewing another player's stats (requires admin permission)
            if (!sender.hasPermission("blockparty.stats.others")) {
                plugin.getMessageManager().sendMessage(sender, "plugin.no-permission");
                return;
            }
            
            String playerName = args[1];
            target = Bukkit.getPlayerExact(playerName);
            
            if (target == null) {
                plugin.getMessageManager().sendMessage(sender, "command.invalid-player");
                return;
            }
        } else {
            // Viewing own stats, must be a player
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().sendMessage(sender, "plugin.player-only");
                return;
            }
            
            target = (Player) sender;
        }
        
        // Get player data and statistics
        UUID uuid = target.getUniqueId();
        Map<String, String> placeholders = plugin.getPlayerDataManager().getStatisticsPlaceholders(uuid);
        
        // Send statistics message
        plugin.getMessageManager().sendMessage(sender, "command.stats", placeholders);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Tab complete player names if they have permission to view others' stats
        if (args.length == 2 && sender.hasPermission("blockparty.stats.others")) {
            String partialName = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}