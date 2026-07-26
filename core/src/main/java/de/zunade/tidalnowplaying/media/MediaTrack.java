package de.zunade.tidalnowplaying.media;

import java.util.Objects;

public record MediaTrack(
    String artist,
    String title,
    PlaybackState playbackState,
    long observedAt,
    String source,
    String artworkPath,
    String artworkKey,
    double progress
) {

  public MediaTrack {
    artist = Objects.requireNonNullElse(artist, "");
    title = Objects.requireNonNullElse(title, "");
    playbackState = Objects.requireNonNull(playbackState, "playbackState");
    source = Objects.requireNonNullElse(source, "");
    artworkPath = Objects.requireNonNullElse(artworkPath, "");
    artworkKey = Objects.requireNonNullElse(artworkKey, "");
    progress = Math.clamp(progress, 0.0, 1.0);
  }

  public MediaTrack(
      String artist,
      String title,
      PlaybackState playbackState,
      long observedAt,
      String source
  ) {
    this(artist, title, playbackState, observedAt, source, "", "", 0.0);
  }

  public static MediaTrack state(PlaybackState state, String source) {
    return new MediaTrack("", "", state, System.currentTimeMillis(), source, "", "", 0.0);
  }

  public boolean hasMetadata() {
    return !this.artist.isBlank() || !this.title.isBlank();
  }

  public boolean sameContent(MediaTrack other) {
    return other != null
        && this.artist.equals(other.artist)
        && this.title.equals(other.title)
        && this.playbackState == other.playbackState
        && this.artworkKey.equals(other.artworkKey);
  }
}
