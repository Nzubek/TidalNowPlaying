package de.zunade.tidalnowplaying.api.snapshot;

import java.util.List;
import java.util.UUID;
import net.labymod.api.client.component.Component;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshot;

/**
 * Immutable rendering data for one player.
 *
 * <p>The list is prepared while the entity snapshot is captured. Rendering therefore performs no
 * Windows calls, string formatting, or component allocation.</p>
 */
public interface MusicPlayerSnapshot extends LabySnapshot {

  UUID playerUuid();

  List<Component> components();

  boolean modernDesign();

  boolean showVisualTile();

  boolean animateVisualTile();

  int animationSpeedMillis();

  boolean paused();

  String artworkPath();

  String artworkKey();

  double progress();
}
