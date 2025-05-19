package id.rnggagib.commands;

import id.rnggagib.BlockParty;
import id.rnggagib.commands.subcommands.GiveCommand;
import id.rnggagib.commands.subcommands.HelpCommand;
import id.rnggagib.commands.subcommands.ReloadCommand;
import id.rnggagib.commands.subcommands.StatsCommand;
import id.rnggagib.commands.subcommands.SubCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Main command manager for the BlockParty plugin
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    private final BlockParty plugin;
    private final Map<String, SubCommand> commands;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public CommandManager(BlockParty plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        
        // Register subcommands
        registerSubCommand(new HelpCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
        registerSubCommand(new GiveCommand(plugin));
        registerSubCommand(new StatsCommand(plugin));
    }
    
    /**
     * Register a subcommand
     * @param subCommand The subcommand to register
     */
    private void registerSubCommand(SubCommand subCommand) {
        commands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            commands.put(alias.toLowerCase(), subCommand);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // No arguments, show help
            commands.get("help").execute(sender, args);
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = commands.get(subCommandName);
        
        if (subCommand == null) {
            // Unknown subcommand, show invalid message
            plugin.getMessageManager().sendMessage(sender, "command.invalid");
            return true;
        }
        
        // Check permission
        if (!subCommand.hasPermission(sender)) {
            plugin.getMessageManager().sendMessage(sender, "plugin.no-permission");
            return true;
        }
        
        // Execute the subcommand
        subCommand.execute(sender, args);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // First argument - show available commands the sender has permission to use
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            for (SubCommand subCommand : getUniqueSubCommands()) {
                if (subCommand.hasPermission(sender) && 
                    subCommand.getName().toLowerCase().startsWith(partialCommand)) {
                    completions.add(subCommand.getName());
                }
            }
            return completions;
        }
        
        // Further arguments - delegate to subcommand
        if (args.length >= 2) {
            SubCommand subCommand = commands.get(args[0].toLowerCase());
            if (subCommand != null && subCommand.hasPermission(sender)) {
                return subCommand.onTabComplete(sender, args);
            }
        }
        
        return completions;
    }
    
    /**
     * Get unique subcommands (no duplicates from aliases)
     * @return List of unique subcommands
     */
    private List<SubCommand> getUniqueSubCommands() {
        List<SubCommand> uniqueCommands = new ArrayList<>();
        for (SubCommand cmd : commands.values()) {
            if (!uniqueCommands.contains(cmd)) {
                uniqueCommands.add(cmd);
            }
        }
        return uniqueCommands;
    }
}