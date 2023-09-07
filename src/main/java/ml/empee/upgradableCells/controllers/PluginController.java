package ml.empee.upgradableCells.controllers;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import ml.empee.upgradableCells.config.CommandsConfig;
import ml.empee.upgradableCells.config.LangConfig;
import ml.empee.upgradableCells.constants.Permissions;
import ml.empee.upgradableCells.services.CellService;
import ml.empee.upgradableCells.utils.Logger;
import mr.empee.lightwire.annotations.Singleton;
import org.bukkit.command.CommandSender;

/**
 * Plugin related commands
 */

@Singleton
public class PluginController {

  private final LangConfig langConfig;
  private final CellService cellService;

  public PluginController(
      LangConfig langConfig, CommandsConfig commandsConfig, CellService cellService
  ) {
    this.langConfig = langConfig;
    this.cellService = cellService;

    commandsConfig.register(this);
  }

  @CommandMethod("cell reload")
  @CommandPermission(Permissions.ADMIN)
  public void reload(CommandSender sender) {
    langConfig.reload();
    cellService.reload();

    Logger.log(sender, "&7The plugin has been reloaded");
  }

}
