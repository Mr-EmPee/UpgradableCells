package ml.empee.upgradableCells.model.entities;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A cell
 */

public class OwnedCell {

  @Getter @Setter
  private int id;

  @Getter @Setter
  private UUID owner;

  @Getter @Setter
  private Map<UUID, Rank> members = new HashMap<>();

  @Getter @Setter
  private Integer level;

  @Setter
  private Location origin;

  @Getter @Setter
  private boolean pasting;

  public Location getOrigin() {
    return origin.clone();
  }

  public static OwnedCell of(UUID owner, Integer level, Location origin) {
    OwnedCell cell = new OwnedCell();
    cell.setOwner(owner);
    cell.setLevel(level);
    cell.setOrigin(origin);
    return cell;
  }

  /**
   * Cell ranks
   */
  public enum Rank {
    MEMBER, GUARD, MANAGER
  }

}
