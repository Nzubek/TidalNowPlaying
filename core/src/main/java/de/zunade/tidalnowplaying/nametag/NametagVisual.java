package de.zunade.tidalnowplaying.nametag;

import java.util.Objects;

public record NametagVisual(
    boolean modernDesign,
    boolean showTile,
    boolean animateTile,
    int animationSpeedMillis,
    boolean paused,
    String artworkPath,
    String artworkKey,
    double progress
) {

  public static final NametagVisual EMPTY =
      new NametagVisual(false, false, false, 200, false, "", "", 0.0);

  public NametagVisual {
    animationSpeedMillis = Math.clamp(animationSpeedMillis, 100, 500);
    artworkPath = Objects.requireNonNullElse(artworkPath, "");
    artworkKey = Objects.requireNonNullElse(artworkKey, "");
    progress = Math.clamp(progress, 0.0, 1.0);
  }
}
