package de.zunade.tidalnowplaying.nametag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnimatedNametagFormatterTest {

  @Test
  void buildsTitleAndArtistRows() {
    AnimatedNametagFormatter.NametagLines lines =
        AnimatedNametagFormatter.format(status(PlaybackState.PLAYING), preferences(true), 0);

    assertEquals("Numb", lines.primary());
    assertEquals("Linkin Park", lines.secondary());
    assertEquals("▂▅▇", lines.topIndicator());
    assertEquals("▆▃▁", lines.bottomIndicator());
  }

  @Test
  void animatesTheEqualizer() {
    AnimatedNametagFormatter.NametagLines first =
        AnimatedNametagFormatter.format(status(PlaybackState.PLAYING), preferences(true), 0);
    AnimatedNametagFormatter.NametagLines next =
        AnimatedNametagFormatter.format(status(PlaybackState.PLAYING), preferences(true), 1);

    assertNotEquals(first.topIndicator(), next.topIndicator());
    assertNotEquals(first.bottomIndicator(), next.bottomIndicator());
  }

  @Test
  void usesStaticPauseIndicator() {
    AnimatedNametagFormatter.NametagLines lines =
        AnimatedNametagFormatter.format(status(PlaybackState.PAUSED), preferences(true), 7);

    assertTrue(lines.paused());
    assertEquals("▌▌ ", lines.topIndicator());
    assertEquals(lines.topIndicator(), lines.bottomIndicator());
  }

  @Test
  void scrollsLongTextByUnicodeCodePoint() {
    String value = "ABCDEFGHIJKLMN🎵OPQRSTUVWXYZ";

    String first = AnimatedNametagFormatter.marquee(value, 24, 0);
    String moved = AnimatedNametagFormatter.marquee(value, 24, 6);

    assertEquals(24, first.codePointCount(0, first.length()));
    assertEquals(24, moved.codePointCount(0, moved.length()));
    assertNotEquals(first, moved);
  }

  @Test
  void hidesPausedTrackWhenDisabled() {
    DisplayPreferences preferences = new DisplayPreferences(
        true, false, true, true, true, true,
        true, true, 200, DisplayPreferences.DEFAULT_FORMAT, 72
    );

    assertNull(AnimatedNametagFormatter.format(
        status(PlaybackState.PAUSED),
        preferences,
        0
    ));
  }

  private static DisplayPreferences preferences(boolean showPaused) {
    return new DisplayPreferences(
        true, showPaused, true, true, true, true,
        true, true, 200, DisplayPreferences.DEFAULT_FORMAT, 72
    );
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
