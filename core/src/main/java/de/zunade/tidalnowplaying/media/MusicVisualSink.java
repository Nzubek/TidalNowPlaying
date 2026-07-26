package de.zunade.tidalnowplaying.media;

import java.util.UUID;

/**
 * Keeps local rendering details separate from the deliberately small network status payload.
 */
public interface MusicVisualSink {

  void updateVisual(UUID playerUuid, MediaTrack track);

  void removeVisual(UUID playerUuid);
}
