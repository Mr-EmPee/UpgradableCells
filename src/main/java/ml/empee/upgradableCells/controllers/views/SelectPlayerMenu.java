package ml.empee.upgradableCells.controllers.views;

import lombok.RequiredArgsConstructor;
import ml.empee.ioc.Bean;
import ml.empee.ioc.RegisteredListener;
import ml.empee.itembuilder.ItemBuilder;
import ml.empee.simplemenu.model.GItem;
import ml.empee.simplemenu.model.menus.ChestMenu;
import ml.empee.simplemenu.model.pane.ScrollPane;
import ml.empee.upgradableCells.config.LangConfig;
import ml.empee.upgradableCells.model.entities.OwnedCell;
import ml.empee.upgradableCells.model.events.CellMemberJoinEvent;
import ml.empee.upgradableCells.model.events.CellMemberLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Menu to claim a cell
 */

@RequiredArgsConstructor
public class SelectPlayerMenu implements Bean, RegisteredListener {

  private static SelectPlayerMenu instance;
  private final List<Menu> openedMenus = new ArrayList<>();
  private final LangConfig langConfig;

  public static CompletableFuture<OfflinePlayer> selectPlayer(Player player, OwnedCell cell, List<OfflinePlayer> players) {
    var action = new CompletableFuture<OfflinePlayer>();
    instance.create(player, cell, players, action).open();
    return action;
  }

  @Override
  public void onStart() {
    instance = this;
  }

  @EventHandler
  public void onCellMemberLeave(CellMemberLeaveEvent event) {
    for (Menu menu : openedMenus) {
      if (menu.cell.equals(event.getCell())) {
        menu.removeSelectablePlayer(event.getMember().getUuid());
      }
    }
  }

  @EventHandler
  public void onCellMemberJoin(CellMemberJoinEvent event) {
    for (Menu menu : openedMenus) {
      if (menu.cell.equals(event.getCell())) {
        menu.addSelectablePlayer(event.getMember().getUuid());
      }
    }
  }

  private Menu create(Player player, OwnedCell cell, List<OfflinePlayer> players, CompletableFuture<OfflinePlayer> future) {
    return new Menu(player, cell, players, future);
  }

  private class Menu extends ChestMenu {

    private final OwnedCell cell;
    private final CompletableFuture<OfflinePlayer> action;
    private final ScrollPane pane = new ScrollPane(7, 4);

    private final List<OfflinePlayer> players;

    public Menu(Player viewer, OwnedCell cell, List<OfflinePlayer> players, CompletableFuture<OfflinePlayer> action) {
      super(viewer, 6, langConfig.translate("menus.select-player.title"));

      this.cell = cell;
      this.action = action;
      this.players = new ArrayList<>(players);
    }

    public void removeSelectablePlayer(UUID uuid) {
      players.removeIf(p -> p.getUniqueId().equals(uuid));
      pane.setCols(
          players.stream()
              .map(this::playerItem)
              .toList()
      );

      refresh();
    }

    public void addSelectablePlayer(UUID uuid) {
      players.add(Bukkit.getOfflinePlayer(uuid));
      pane.setCols(
          players.stream()
              .map(this::playerItem)
              .toList()
      );

      refresh();
    }

    @Override
    public void onOpen() {
      openedMenus.add(this);

      pane.setCols(
          players.stream()
              .map(this::playerItem)
              .toList()
      );

      top().addPane(1, 1, pane);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
      openedMenus.remove(this);
    }

    private GItem playerItem(OfflinePlayer player) {
      var member = cell.getMember(player.getUniqueId());
      var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      var item = ItemBuilder.skull()
          .setName("&e" + player.getName())
          .setLore(
              langConfig.translateBlock(
                  "menus.select-player.player-lore",
                  member.getRank().name(), member.getMemberSince().format(formatter)
              )
          )
          .owner(player)
          .build();

      return GItem.builder()
          .itemstack(item)
          .clickHandler(e -> action.complete(player))
          .build();
    }
  }

}

