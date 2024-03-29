package ml.empee.upgradableCells.utils.helpers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import lombok.SneakyThrows;
import ml.empee.upgradableCells.utils.Logger;
import ml.empee.upgradableCells.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A WorldEdit schematic
 */

public class Schematic {

  private static final int DELAY_BETWEEN_PASTING = 20; //ticks
  private final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(Schematic.class);
  private final File file;
  private final Vector origin;

  private final List<Map<Vector, BlockData>> sections;

  @SneakyThrows
  private static ClipboardReader getReader(File file) {
    ClipboardFormat format = ClipboardFormats.findByFile(file);
    if (format == null) {
      throw new IllegalArgumentException("The file " + file.getName() + " isn't a valid schematic");
    }

    return format.getReader(new FileInputStream(file));
  }

  public Schematic(File file) {
    this.file = file;

    try (ClipboardReader reader = getReader(file)) {
      Clipboard clipboard = reader.read();
      origin = readOrigin(clipboard);
      sections = readSections(clipboard);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public BlockData getBlock(Vector vector) {
    for (Map<Vector, BlockData> section : sections) {
      var data = section.get(vector);
      if (data != null) {
        return data;
      }
    }

    return null;
  }

  private List<Map<Vector, BlockData>> readSections(Clipboard clipboard) {
    List<Map<Vector, BlockData>> sections = new ArrayList<>();

    var min = clipboard.getMinimumPoint();
    var max = clipboard.getMaximumPoint();

    for (int x = min.getX(); x <= max.getX(); x++) {
      Map<Vector, BlockData> section = new HashMap<>();

      for (int y = min.getY(); y <= max.getY(); y++) {
        for (int z = min.getZ(); z <= max.getZ(); z++) {
          BaseBlock block = clipboard.getFullBlock(BlockVector3.at(x, y, z));
          section.put(new Vector(x - min.getX(), y - min.getY(), z - min.getZ()), BukkitAdapter.adapt(block));
        }
      }

      sections.add(section);
    }

    return sections;
  }

  private Vector readOrigin(Clipboard clipboard) {
    BlockVector3 origin = clipboard.getOrigin();

    return new Vector(
        origin.getX() - clipboard.getMinimumPoint().getBlockX(),
        origin.getY() - clipboard.getMinimumPoint().getBlockY(),
        origin.getZ() - clipboard.getMinimumPoint().getBlockZ()
    );
  }

  public Vector getOrigin() {
    return origin.clone();
  }

  /**
   * Paste the schematic
   * @param mask a function that returns true if the block <b>can't</b> be pasted
   */
  public CompletableFuture<Void> paste(Location origin, Function<Vector, Boolean> mask) {
    Logger.debug("Starting pasting of schematic " + file.getName());
    return pasteRecursively(origin, mask, 0, 5);
  }

  private CompletableFuture<Void> pasteRecursively(Location origin, Function<Vector, Boolean> mask, int sectionIndex, int chunkSize) {
    CompletableFuture<Void> stage = new CompletableFuture<>();
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      for (int i = sectionIndex; i < sectionIndex + chunkSize; i++) {
        if (i >= sections.size()) {
          Logger.debug("Finished pasting schematic " + file.getName());
          stage.complete(null);
          return;
        }

        var section = sections.get(i);
        section.forEach((position, block) -> {
          if (mask.apply(position.clone())) {
            return;
          }

          NmsUtils.setBlockFast(origin.clone().add(position), block);
        });
      }

      pasteRecursively(origin, mask, sectionIndex + chunkSize, chunkSize).thenRun(() -> stage.complete(null));
    }, DELAY_BETWEEN_PASTING);

    return stage;
  }

}
