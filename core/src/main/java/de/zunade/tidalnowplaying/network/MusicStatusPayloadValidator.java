package de.zunade.tidalnowplaying.network;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.util.TextSanitizer;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reusable inbound boundary for version 2. It is intentionally not wired in local-only version 1.
 */
public final class MusicStatusPayloadValidator {

  public static final long MIN_UPDATE_INTERVAL_MILLIS = 750;
  private static final long MAX_TIMESTAMP_SKEW_MILLIS = Duration.ofMinutes(2).toMillis();

  private final ConcurrentHashMap<UUID, Long> lastAcceptedAt = new ConcurrentHashMap<>();

  public Optional<PlayerMusicStatus> validate(
      UUID playerUuid,
      String artist,
      String title,
      PlaybackState state,
      long updatedAt,
      int encodedPayloadBytes,
      long receivedAt
  ) {
    if (playerUuid == null
        || state == null
        || encodedPayloadBytes < 0
        || encodedPayloadBytes > PlayerMusicStatus.MAX_NETWORK_PAYLOAD_BYTES
        || Math.abs(receivedAt - updatedAt) > MAX_TIMESTAMP_SKEW_MILLIS
        || !isShareableState(state)) {
      return Optional.empty();
    }

    Long previous = this.lastAcceptedAt.get(playerUuid);
    if (previous != null && receivedAt - previous < MIN_UPDATE_INTERVAL_MILLIS) {
      return Optional.empty();
    }

    String safeArtist = TextSanitizer.externalText(
        artist,
        PlayerMusicStatus.MAX_ARTIST_CODE_POINTS
    );
    String safeTitle = TextSanitizer.externalText(
        title,
        PlayerMusicStatus.MAX_TITLE_CODE_POINTS
    );
    if (state.canBeDisplayed() && safeArtist.isBlank() && safeTitle.isBlank()) {
      return Optional.empty();
    }

    this.lastAcceptedAt.put(playerUuid, receivedAt);
    return Optional.of(new PlayerMusicStatus(
        playerUuid,
        safeArtist,
        safeTitle,
        state,
        updatedAt
    ));
  }

  public void playerLeft(UUID playerUuid) {
    if (playerUuid != null) {
      this.lastAcceptedAt.remove(playerUuid);
    }
  }

  private boolean isShareableState(PlaybackState state) {
    return state == PlaybackState.PLAYING
        || state == PlaybackState.PAUSED
        || state == PlaybackState.STOPPED
        || state == PlaybackState.NO_TRACK
        || state == PlaybackState.UNKNOWN;
  }
}
