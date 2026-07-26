package de.zunade.tidalnowplaying.media.bridge;

import de.zunade.tidalnowplaying.media.PlaybackState;
import java.util.Objects;

public record BridgeResponse(
    PlaybackState state,
    String artist,
    String title,
    String source,
    String artworkPath,
    String artworkKey,
    double progress,
    String diagnostic
) {

  public BridgeResponse {
    state = Objects.requireNonNull(state, "state");
    artist = Objects.requireNonNullElse(artist, "");
    title = Objects.requireNonNullElse(title, "");
    source = Objects.requireNonNullElse(source, "");
    artworkPath = Objects.requireNonNullElse(artworkPath, "");
    artworkKey = Objects.requireNonNullElse(artworkKey, "");
    progress = Math.clamp(progress, 0.0, 1.0);
    diagnostic = Objects.requireNonNullElse(diagnostic, "");
  }

  public BridgeResponse(
      PlaybackState state,
      String artist,
      String title,
      String source,
      String diagnostic
  ) {
    this(state, artist, title, source, "", "", 0.0, diagnostic);
  }

  public static BridgeResponse state(PlaybackState state, String diagnostic) {
    return new BridgeResponse(state, "", "", "", "", "", 0.0, diagnostic);
  }
}
