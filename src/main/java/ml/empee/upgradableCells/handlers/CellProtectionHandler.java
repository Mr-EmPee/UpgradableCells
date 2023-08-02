package ml.empee.upgradableCells.handlers;

import lombok.RequiredArgsConstructor;
import ml.empee.ioc.Bean;
import ml.empee.ioc.RegisteredListener;
import ml.empee.upgradableCells.constants.Permissions;
import ml.empee.upgradableCells.model.entities.OwnedCell;
import ml.empee.upgradableCells.services.CellService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handle actions inside a cell
 */

@RequiredArgsConstructor
public class CellProtectionHandler implements Bean, RegisteredListener {

  private final CellService cellService;

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
      event.setCancelled(true);
    }
  }

  private boolean canBuild(Player player, Location target) {
    if (player.hasPermission(Permissions.ADMIN)) {
      return true;
    }

    var cell = cellService.findCellByLocation(target).orElse(null);
    if (cell == null) {
      return true;
    }

    OwnedCell.Rank member = cell.getMembers().get(player.getUniqueId());
    if (member == null || member.canBuild()) {
      return false;
    }

    var project = cellService.getCellProject(cell.getLevel());
    return project.canBuild(cell, target);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();
    if (player.hasPermission(Permissions.ADMIN)) {
      return;
    }

    var clickedBlock = event.getClickedBlock();
    var cell = cellService.findCellByLocation(clickedBlock.getLocation()).orElse(null);
    if (cell == null) {
      return;
    }

    OwnedCell.Rank member = cell.getMembers().get(player.getUniqueId());
    if (member == null) {
      event.setCancelled(true);
      return;
    }

    if (clickedBlock.getType() == Material.CHEST && !member.canAccessChests()) {
      event.setCancelled(true);
    }
  }

}
