package ml.empee.upgradableCells.controllers.commands;

import java.util.stream.Collectors;

import ml.empee.upgradableCells.controllers.Controller;
import ml.empee.upgradableCells.model.Member;
import ml.empee.upgradableCells.model.entities.Cell;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.specifier.Greedy;
import lombok.RequiredArgsConstructor;
import ml.empee.upgradableCells.controllers.CellController;
import ml.empee.upgradableCells.config.LangConfig;
import ml.empee.upgradableCells.controllers.views.ClaimCellMenu;
import ml.empee.upgradableCells.controllers.views.ManageCellMenu;
import ml.empee.upgradableCells.controllers.views.SelectCellMenu;
import ml.empee.upgradableCells.controllers.views.TopCellsMenu;
import ml.empee.upgradableCells.services.CellService;
import ml.empee.upgradableCells.utils.Logger;
import mr.empee.lightwire.annotations.Singleton;

/**
 * Controller use to manage cell operations
 */

@Singleton
@RequiredArgsConstructor
public class CellCommand implements Controller {

  private final CellService cellService;
  private final CellController cellController;
  private final LangConfig langConfig;

  @CommandMethod("claim")
  public void claimCell(Player sender) {
    var cell = cellService.findCellByOwner(sender.getUniqueId()).orElse(null);

    if (cell == null) {
      ClaimCellMenu.open(sender);
    } else {
      Logger.log(sender, langConfig.translate("cell.already-bought"));
    }
  }

  /**
   * Open the cell management menu
   */
  @CommandMethod("cell")
  public void openCell(Player sender) {
    var cells = cellService.findCellsByMember(sender.getUniqueId());

    if (cells.isEmpty()) {
      ClaimCellMenu.open(sender);
      return;
    }

    if (cells.size() == 1) {
      ManageCellMenu.open(sender, cells.get(0));
    } else {
      SelectCellMenu.selectCell(sender, cells).thenAccept(
          c -> ManageCellMenu.open(sender, c));
    }
  }

  /**
   * Cell-Top
   */
  @CommandMethod("cell-top")
  public void openCellTopMenu(Player sender) {
    TopCellsMenu.open(sender);
  }

  /**
   * Teleport a player to his cell
   */
  @CommandMethod("home")
  public void teleportToCell(Player sender) {
    var cell = cellService.findCellByOwner(sender.getUniqueId()).orElse(null);

    if (cell == null) {
      Logger.log(sender, langConfig.translate("cell.not-bought"));
      return;
    }

    cellController.teleportToCell(sender, cell);
  }

  @CommandMethod("cell join <target>")
  public void joinCell(Player sender, @Argument OfflinePlayer target) {
    var cell = cellService.findCellByOwner(target.getUniqueId()).orElse(null);

    if (cell == null) {
      Logger.log(sender, langConfig.translate("cell.not-existing"));
      return;
    }

    cellController.joinCell(sender, cell);
  }

  /**
   * Invite a player to a specific cell
   */
  @CommandMethod("cell invite <target>")
  public void inviteToCell(Player sender, @Argument Player target) {
    var cells = cellService.findCellsByMember(sender.getUniqueId()).stream()
        .filter(c -> c.getMember(sender.getUniqueId()).orElseThrow().getRank().hasPermission(Member.Permissions.INVITE))
        .collect(Collectors.toList());

    if (cells.isEmpty()) {
      Logger.log(sender, langConfig.translate("cell.not-bought"));
      return;
    }

    if (cells.size() == 1) {
      cellController.invitePlayer(cells.get(0), sender, target);
    } else {
      SelectCellMenu.selectCell(sender, cells).thenAccept(
          c -> cellController.invitePlayer(c, sender, target));
    }
  }

  /**
   * Leave a cell
   */
  @CommandMethod("cell leave")
  public void leaveCell(Player sender) {
    var cells = cellService.findCellsByMember(sender.getUniqueId()).stream()
      .filter(c -> !c.getOwner().equals(sender.getUniqueId()))
      .collect(Collectors.toList());

    if (cells.isEmpty()) {
      Logger.log(sender, langConfig.translate("cell.not-bought"));
      return;
    }

    if (cells.size() == 1) {
      cellController.leaveCell(sender, cells.get(0));
    } else {
      SelectCellMenu.selectCell(sender, cells).thenAccept(
          c -> cellController.leaveCell(sender, c)
      );
    }
  }

  @CommandMethod("cell name <name>")
  public void setCellName(Player sender, @Argument @Greedy String name) {
    var cell = cellService.findCellByOwner(sender.getUniqueId()).orElse(null);

    if (cell == null) {
      Logger.log(sender, langConfig.translate("cell.not-bought"));
      return;
    }

    cellController.setCellName(sender, cell, name);
  }

  @CommandMethod("cell description <description>")
  public void setCellDescription(Player sender, @Argument @Greedy String description) {
    var cell = cellService.findCellByOwner(sender.getUniqueId()).orElse(null);

    if (cell == null) {
      Logger.log(sender, langConfig.translate("cell.not-bought"));
      return;
    }

    cellController.setCellDescription(sender, cell, description);
  }

  @CommandMethod("cell visit <target>")
  public void visitCell(Player sender, @Argument OfflinePlayer target) {
    Cell cell = cellService.findCellByOwner(target.getUniqueId()).orElse(null);

    if (cell == null) {
      Logger.log(sender, langConfig.translate("cell.not-existing"));
      return;
    }

    cellController.teleportToCell(sender, cell);
  }
}