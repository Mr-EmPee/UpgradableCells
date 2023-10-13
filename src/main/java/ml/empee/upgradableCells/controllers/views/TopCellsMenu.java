package ml.empee.upgradableCells.controllers.views;

import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.cryptomorin.xseries.XMaterial;

import lombok.RequiredArgsConstructor;
import ml.empee.itembuilder.ItemBuilder;
import ml.empee.simplemenu.model.GItem;
import ml.empee.simplemenu.model.menus.ChestMenu;
import ml.empee.simplemenu.model.panes.ScrollPane;
import ml.empee.upgradableCells.api.CellAPI;
import ml.empee.upgradableCells.config.LangConfig;
import ml.empee.upgradableCells.model.entities.OwnedCell;
import mr.empee.lightwire.annotations.Instance;
import mr.empee.lightwire.annotations.Singleton;

/**
 * Menu to manage banned players
 */

@Singleton
@RequiredArgsConstructor
public class TopCellsMenu implements Listener {

  @Instance
  private static TopCellsMenu instance;
  private final LangConfig langConfig;
  private final CellAPI cellAPI;

  public static void open(Player player) {
    instance.create(player).open();
  }

  private Menu create(Player player) {
    return new Menu(player);
  }

  private class Menu extends ChestMenu {
    private final ScrollPane pane = ScrollPane.horizontal(7, 3, 3);

    public Menu(Player viewer) {
      super(viewer, 5, langConfig.translate("menus.top-cells.title"));
    }

    @Override
    public void onOpen() {
      top().setItem(0, 4, closeItem());

      pane.addAll(
          cellAPI.findTopCells(21).stream()
              .map(this::cellItem)
              .collect(Collectors.toList())
      );

      top().addPane(1, 1, pane);
    }

    private GItem cellItem(OwnedCell cell) {
      OfflinePlayer owner = Bukkit.getOfflinePlayer(cell.getOwner());
      var item = ItemBuilder.skull()
          .setName("&e" + owner.getName())
          .setLore(langConfig.translateBlock("menus.top-cells.cell-lore", cell.getAllMembers().size()))
          .owner(owner)
          .build();

      return GItem.builder()
          .itemstack(item)
          .clickHandler(e -> {
            var player = (Player) e.getWhoClicked();
            cellAPI.teleportToCell(player, cell);
          }).build();
    }

    private GItem closeItem() {
      var item = ItemBuilder.from(XMaterial.WHITE_BED.parseItem())
          .setName(langConfig.translate("menus.top-cells.items.close.name"))
          .build();

      return GItem.builder()
          .itemstack(item)
          .clickHandler(e -> {
            e.getWhoClicked().closeInventory();
          }).build();
    }
  }

}
