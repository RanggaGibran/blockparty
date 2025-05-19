package id.rnggagib.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

/**
 * Interface for all BlockParty subcommands
 */
public interface SubCommand {
    /**
     * Get the name of the subcommand
     * @return The subcommand name
     */
    String getName();
    
    /**
     * Get the permission required to use this command
     * @return The permission node
     */
    String getPermission();
    
    /**
     * Get the aliases for this command
     * @return Array of aliases
     */
    String[] getAliases();
    
    /**
     * Check if a sender has permission to use this command
     * @param sender The command sender
     * @return True if they have permission
     */
    boolean hasPermission(CommandSender sender);
    
    /**
     * Execute the command
     * @param sender The command sender
     * @param args Command arguments
     */
    void execute(CommandSender sender, String[] args);
    
    /**
     * Tab completion for this command
     * @param sender The command sender
     * @param args Command arguments
     * @return List of tab completions
     */
    default List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}