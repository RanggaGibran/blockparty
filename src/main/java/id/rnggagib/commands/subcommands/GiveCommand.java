package id.rnggagib.commands.subcommands;

import id.rnggagib.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Give command implementation
 */
public class GiveCommand implements SubCommand {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public GiveCommand(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "give";
    }
    
    @Override
    public String getPermission() {
        return "blockparty.give";
    }
    
    @Override
    public String[] getAliases() {
        return new String[] { "g" };
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check if enough arguments
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "command.invalid");
            return;
        }
        
        // Get player
        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);
        
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "command.invalid-player");
            return;
        }
        
        // Get amount (default 1)
        int amount = 1;
        
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    throw new NumberFormatException("Amount must be positive");
                }
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(sender, "command.invalid-number");
                return;
            }
        }
        
        // Give access item
        plugin.getAccessManager().giveAccessItem(target, amount);
        
        // Send messages
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("player", target.getName());
        
        plugin.getMessageManager().sendMessage(sender, "access.given", placeholders);
        
        if (sender != target) {
            plugin.getMessageManager().sendMessage(target, "access.received", placeholders);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Tab complete player names
        if (args.length == 2) {
            String partialName = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }
        
        // Tab complete amount suggestions
        if (args.length == 3) {
            for (String suggestion : new String[] { "1", "5", "10", "64" }) {
                if (suggestion.startsWith(args[2])) {
                    completions.add(suggestion);
                }
            }
        }
        
        return completions;
    }
}