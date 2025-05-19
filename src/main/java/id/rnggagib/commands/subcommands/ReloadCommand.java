package id.rnggagib.commands.subcommands;

import id.rnggagib.BlockParty;
import org.bukkit.command.CommandSender;

/**
 * Reload command implementation
 */
public class ReloadCommand implements SubCommand {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public ReloadCommand(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getPermission() {
        return "blockparty.reload";
    }
    
    @Override
    public String[] getAliases() {
        return new String[] { "rl" };
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        // Reload the plugin
        plugin.reload();
        
        // Send confirmation message
        plugin.getMessageManager().sendMessage(sender, "plugin.reload");
    }
}