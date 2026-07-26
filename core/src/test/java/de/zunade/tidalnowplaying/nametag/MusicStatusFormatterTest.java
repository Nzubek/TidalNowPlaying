package de.zunade.tidalnowplaying.nametag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MusicStatusFormatterTest {

  @Test
  void formatsPlayingTrack() {
    PlayerMusicStatus status = status(PlaybackState.PLAYING);
    DisplayPreferences preferences =
        new DisplayPreferences(true, true, true, true, true, true,
            false, false, 200, DisplayPreferences.DEFAULT_FORMAT, 72);

    assertEquals("♫ Linkin Park – Numb", MusicStatusFormatter.format(status, preferences));
  }

  @Test
  void formatsPausedTrackWithPauseIcon() {
    PlayerMusicStatus status = status(PlaybackState.PAUSED);
    DisplayPreferences preferences =
        new DisplayPreferences(true, true, true, true, true, true,
            false, false, 200, DisplayPreferences.DEFAULT_FORMAT, 72);

    assertEquals("⏸ Linkin Park – Numb", MusicStatusFormatter.format(status, preferences));
  }

  @Test
  void hidesPausedTrackWhenConfigured() {
    PlayerMusicStatus status = status(PlaybackState.PAUSED);
    DisplayPreferences preferences =
        new DisplayPreferences(true, false, true, true, true, true,
            false, false, 200, DisplayPreferences.DEFAULT_FORMAT, 72);

    assertNull(MusicStatusFormatter.format(status, preferences));
  }

  @Test
  void omitsSeparatorWhenArtistIsHidden() {
    PlayerMusicStatus status = status(PlaybackState.PLAYING);
    DisplayPreferences preferences =
        new DisplayPreferences(true, true, true, true, false, true,
            false, false, 200, DisplayPreferences.DEFAULT_FORMAT, 72);

    assertEquals("♫ Numb", MusicStatusFormatter.format(status, preferences));
  }

  private static PlayerMusicStatus status(PlaybackState state) {
    return new PlayerMusicStatus(
        UUID.randomUUID(),
        "Linkin Park",
        "Numb",
        state,
        1
    );
  }
}
