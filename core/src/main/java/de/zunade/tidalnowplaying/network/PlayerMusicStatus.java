package de.zunade.tidalnowplaying.network;

import de.zunade.tidalnowplaying.media.PlaybackState;
import java.util.Objects;
import java.util.UUID;

/**
 * The complete privacy-bounded payload planned for version 2.
 *
 * <p>No account identifier, token, playlist, history, file path, or device information belongs in
 * this model.</p>
 */
public record PlayerMusicStatus(
    UUID playerUuid,
    String artist,
    String title,
    PlaybackState playbackState,
    long updatedAt
) {

  public static final int MAX_ARTIST_CODE_POINTS = 128;
  public static final int MAX_TITLE_CODE_POINTS = 256;
  public static final int MAX_NETWORK_PAYLOAD_BYTES = 1024;

  public PlayerMusicStatus {
    playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
    artist = Objects.requireNonNullElse(artist, "");
    title = Objects.requireNonNullElse(title, "");
    playbackState = Objects.requireNonNull(playbackState, "playbackState");

    if (artist.codePointCount(0, artist.length()) > MAX_ARTIST_CODE_POINTS) {
      throw new IllegalArgumentException("Artist exceeds the privacy limit");
    }
    if (title.codePointCount(0, title.length()) > MAX_TITLE_CODE_POINTS) {
      throw new IllegalArgumentException("Title exceeds the privacy limit");
    }
  }

  public boolean sameContent(PlayerMusicStatus other) {
    return other != null
        && this.playerUuid.equals(other.playerUuid)
        && this.artist.equals(other.artist)
        && this.title.equals(other.title)
        && this.playbackState == other.playbackState;
  }
}
