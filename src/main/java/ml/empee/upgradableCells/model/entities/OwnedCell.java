package ml.empee.upgradableCells.model.entities;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A cell
 */

//TODO Name & Description

@Getter
@Setter
public class OwnedCell {

  private int id;
  private UUID owner;
  private List<Member> members = new ArrayList<>();
  private Integer level;
  private Location origin;
  private boolean pasting;

  public OfflinePlayer getOwnerPlayer() {
    return Bukkit.getOfflinePlayer(owner);
  }

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

  public List<Player> getOnlineMembers() {
    return members.stream()
        .map(m -> Bukkit.getPlayer(m.getUuid()))
        .filter(Objects::nonNull)
        .toList();
  }

  public List<OfflinePlayer> getAllMembers() {
    return members.stream()
        .map(m -> Bukkit.getOfflinePlayer(m.getUuid()))
        .toList();
  }

  public Member getMember(UUID uuid) {
    return members.stream()
        .filter(m -> m.getUuid().equals(uuid))
        .findFirst().orElse(null);
  }

  public void addMember(Member member) {
    members.add(member);
  }

  public void removeMember(UUID uuid) {
    members.removeIf(m -> m.getUuid().equals(uuid));
  }

  public boolean hasMember(UUID uuid) {
    return getMember(uuid) != null;
  }

}
