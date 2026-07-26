package de.zunade.tidalnowplaying.snapshot;

import de.zunade.tidalnowplaying.api.snapshot.MusicPlayerSnapshot;
import de.zunade.tidalnowplaying.nametag.MusicStatusSnapshotBridge;
import de.zunade.tidalnowplaying.nametag.NametagVisual;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.AbstractLabySnapshot;
import net.labymod.api.laby3d.renderer.snapshot.Extras;

public final class DefaultMusicPlayerSnapshot
    extends AbstractLabySnapshot
    implements MusicPlayerSnapshot {

  private final UUID playerUuid;
  private final List<Component> components;
  private final NametagVisual visual;

  public DefaultMusicPlayerSnapshot(Player player, Extras extras) {
    super(extras);
    this.playerUuid = player.getUniqueId();
    this.components = MusicStatusSnapshotBridge.componentsFor(this.playerUuid);
    this.visual = MusicStatusSnapshotBridge.visualFor(this.playerUuid);
  }

  @Override
  public UUID playerUuid() {
    return this.playerUuid;
  }

  @Override
  public List<Component> components() {
    return this.components;
  }

  @Override
  public boolean modernDesign() {
    return this.visual.modernDesign();
  }

  @Override
  public boolean showVisualTile() {
    return this.visual.showTile();
  }

  @Override
  public boolean animateVisualTile() {
    return this.visual.animateTile();
  }

  @Override
  public int animationSpeedMillis() {
    return this.visual.animationSpeedMillis();
  }

  @Override
  public boolean paused() {
    return this.visual.paused();
  }

  @Override
  public String artworkPath() {
    return this.visual.artworkPath();
  }

  @Override
  public String artworkKey() {
    return this.visual.artworkKey();
  }

  @Override
  public double progress() {
    return this.visual.progress();
  }
}
