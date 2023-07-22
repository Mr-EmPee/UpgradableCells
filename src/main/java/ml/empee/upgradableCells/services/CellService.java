package ml.empee.upgradableCells.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ml.empee.ioc.Bean;
import ml.empee.upgradableCells.model.content.PlayerCache;
import ml.empee.upgradableCells.model.entities.CellProject;
import ml.empee.upgradableCells.model.entities.OwnedCell;
import ml.empee.upgradableCells.repositories.CellRepository;
import ml.empee.upgradableCells.utils.Logger;
import ml.empee.upgradableCells.model.content.Schematic;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handle cell upgrades
 */

@RequiredArgsConstructor
public class CellService implements Bean {

  private final JavaPlugin plugin;
  private final CellRepository cellRepository;
  private final WorldService worldService;

  private final List<CellProject> cellUpgrades = new ArrayList<>();
  private final PlayerCache<Optional<OwnedCell>> ownedCellsCache = new PlayerCache<>(true) {
    @Override
    @SneakyThrows
    public Optional<OwnedCell> fetchValue(UUID key) {
      return cellRepository.findByOwner(key).get();
    }
  };

  private File schematicFolder;

  @Override
  public void onStart() {
    schematicFolder = new File(plugin.getDataFolder(), "levels");
    loadCellUpgrades();
  }

  /**
   * Load cell levels from the schematic folder
   */
  public void loadCellUpgrades() {
    schematicFolder.mkdir();
    cellUpgrades.clear();

    Logger.info("Loading cell upgrades...");

    int level = 0;
    for (File cellFile : findCellUpgradeSchematics()) {
      var cellLevel = new CellProject(level, new Schematic(cellFile));
      cellUpgrades.add(cellLevel);
      level += 1;
    }

    Logger.info("Loaded %s cells", cellUpgrades.size());
  }

  public void reload() {
    ownedCellsCache.reload();
  }

  private List<File> findCellUpgradeSchematics() {
    return Arrays.stream(schematicFolder.listFiles())
        .filter(File::isFile)
        .toList();
  }

  public int getAvailableLevels() {
    return cellUpgrades.size();
  }

  public CellProject getCellProject(int level) {
    return cellUpgrades.get(level);
  }

  @SneakyThrows
  public Optional<OwnedCell> findCellByOwner(UUID owner) {
    return ownedCellsCache.get(owner);
  }

  /**
   * @return the cell within the location
   */
  public Optional<OwnedCell> findCellByLocation(Location location) {
    var position = location.toVector();
    var margin = worldService.getMargin();

    return ownedCellsCache.values().stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(c -> {
          var origin = c.getOrigin();
          if (!origin.getWorld().equals(location.getWorld())) {
            return false;
          }

          return position.isInAABB(origin.toVector(), origin.add(margin, margin, margin).toVector());
        }).findAny();
  }

  public Location getSpawnpoint(OwnedCell cell) {
    return cell.getOrigin().add(getCellProject(cell.getLevel()).getSpawnpoint());
  }

  public void updateCellLevel(OwnedCell cell, int level) {
    CellProject cellProject = cellUpgrades.get(level);
    cell.setLevel(level);
    cellProject.paste(cell);

    saveCell(cell);
  }

  private void saveCell(OwnedCell cell) {
    ownedCellsCache.put(cell.getOwner(), Optional.of(cell));
    cellRepository.save(cell);
  }

}
