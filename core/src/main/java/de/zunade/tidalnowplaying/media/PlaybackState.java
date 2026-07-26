package de.zunade.tidalnowplaying.media;

public enum PlaybackState {
  PLAYING,
  PAUSED,
  STOPPED,
  NO_TRACK,
  TIDAL_NOT_RUNNING,
  SESSION_UNAVAILABLE,
  UNSUPPORTED_PLATFORM,
  UNKNOWN,
  ERROR;

  public boolean canBeDisplayed() {
    return this == PLAYING || this == PAUSED || this == UNKNOWN;
  }
}
