package id.rnggagib.commands.subcommands;

import id.rnggagib.BlockParty;
import org.bukkit.command.CommandSender;

/**
 * Help command implementation
 */
public class HelpCommand implements SubCommand {
    private final BlockParty plugin;
    
    /**
     * Constructor
     * @param plugin The BlockParty plugin instance
     */
    public HelpCommand(BlockParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getPermission() {
        return "blockparty.command";
    }
    
    @Override
    public String[] getAliases() {
        return new String[] { "?" };
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessageWithoutPrefix(sender, "command.help", null);
    }
}