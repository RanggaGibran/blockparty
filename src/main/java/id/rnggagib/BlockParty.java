package id.rnggagib;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * blockparty java plugin
 */
public class BlockParty extends JavaPlugin
{
  private static final Logger LOGGER=Logger.getLogger("blockparty");

  public void onEnable()
  {
    LOGGER.info("blockparty enabled");
  }

  public void onDisable()
  {
    LOGGER.info("blockparty disabled");
  }
}
