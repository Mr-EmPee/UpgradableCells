package ml.empee.upgradableCells.controllers.views;

import com.cryptomorin.xseries.XMaterial;
import lombok.RequiredArgsConstructor;
import ml.empee.ioc.Bean;
import ml.empee.itembuilder.ItemBuilder;
import ml.empee.simplemenu.model.GItem;
import ml.empee.simplemenu.model.menus.ChestMenu;
import ml.empee.upgradableCells.config.LangConfig;
import ml.empee.upgradableCells.model.entities.CellProject;
import ml.empee.upgradableCells.model.entities.OwnedCell;
import ml.empee.upgradableCells.services.CellService;
import ml.empee.upgradableCells.utils.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * GUI from where you can manage a cell
 */

@RequiredArgsConstructor
public class ManageCellMenu implements Bean {

  private static ManageCellMenu instance;

  private final LangConfig langConfig;
  private final CellService cellService;
  private final Economy economy;

  @Override
  public void onStart() {
    instance = this;
  }

  private ChestMenu createMenu(Player player, OwnedCell cell) {
    return new ChestMenu(player, 5, langConfig.translate("menus.manage-cell.title")) {
      @Override
      public void onOpen() {
        populateMenu(this, cell);
      }
    };
  }

  private void populateMenu(ChestMenu menu, OwnedCell cell) {
    menu.top().setItem(2, 1, homeItem());
    menu.top().setItem(2, 3, upgradeItem(cell));
    menu.top().setItem(0, 4, closeItem());
  }

  private GItem homeItem() {
    var item = ItemBuilder.from(XMaterial.IRON_DOOR.parseItem())
        .setName(langConfig.translate("menus.manage-cell.items.home.name"))
        .setLore(langConfig.translateBlock("menus.manage-cell.items.home.lore"))
        .build();

    return GItem.builder()
        .itemstack(item)
        .clickHandler(e -> {
          var player = (Player) e.getWhoClicked();
          var cell = cellService.findCellByOwner(player.getUniqueId()).orElse(null);
          player.closeInventory();

          if (cell == null) {
            Logger.log(player, langConfig.translate("cell.not-bought"));
            return;
          }

          if (cell.getLevel() == 0 && cell.isPasting()) {
            Logger.log(player, langConfig.translate("cell.still-building"));
            return;
          }

          player.teleport(cellService.getSpawnpoint(cell));
        }).build();
  }

  private GItem upgradeItem(OwnedCell cell) {
    CellProject project;
    if (cell.getLevel() != cellService.getMaxLevel()) {
      project = cellService.getCellProject(cell.getLevel() + 1);
    } else {
      project = null;
    }

    var item = ItemBuilder.from(XMaterial.GRASS_BLOCK.parseItem())
        .setName(langConfig.translate("menus.manage-cell.items.upgrade.name"))
        .setLore(
            langConfig.translateBlock(
                "menus.manage-cell.items.upgrade.lore", Map.of(
                    "%cost%", project != null ? project.getCost() : "N/A"
                )
            )
        )
        .build();

    return GItem.builder()
        .itemstack(item)
        .clickHandler(e -> {
          var source = (Player) e.getWhoClicked();
          var targetCell = cellService.findCellByOwner(source.getUniqueId()).orElse(null);
          source.closeInventory();

          if (targetCell == null) {
            Logger.log(source, langConfig.translate("cell.not-bought"));
            return;
          }

          if (project == null) {
            Logger.log(source, langConfig.translate("cell.max-level"));
            return;
          }

          if (targetCell.isPasting()) {
            Logger.log(source, langConfig.translate("cell.still-building"));
            return;
          }

          if (!economy.has(source, project.getCost())) {
            Logger.log(source, langConfig.translate("economy.missing-money"));
            return;
          }

          economy.withdrawPlayer(source, project.getCost());
          cellService.updateCellLevel(targetCell, project.getLevel());
          Logger.log(source, langConfig.translate("cell.bought-upgrade"));
        }).build();
  }

  private GItem closeItem() {
    var item = ItemBuilder.from(XMaterial.WHITE_BED.parseItem())
        .setName(langConfig.translate("menus.manage-cell.items.close.name"))
        .build();

    return GItem.builder()
        .itemstack(item)
        .clickHandler(e -> {
          e.getWhoClicked().closeInventory();
        }).build();
  }

  public static void open(Player player, OwnedCell cell) {
    instance.createMenu(player, cell).open();
  }

}
