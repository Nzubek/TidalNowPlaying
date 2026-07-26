package de.zunade.tidalnowplaying.nametag;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import java.util.Objects;

/**
 * Builds the two text rows for the modern nametag without touching render state.
 */
public final class AnimatedNametagFormatter {

  private static final String[][] EQUALIZER_FRAMES = {
      {"▂▅▇", "▆▃▁"},
      {"▅▇▃", "▂▆▅"},
      {"▇▃▆", "▅▁▇"},
      {"▃▆▂", "▇▅▃"}
  };
  private static final String[] PAUSED_INDICATOR = {"▌▌ ", "▌▌ "};

  private AnimatedNametagFormatter() {
  }

  public static NametagLines format(
      PlayerMusicStatus status,
      DisplayPreferences preferences,
      long animationFrame
  ) {
    if (status == null || !preferences.enabled() || !status.playbackState().canBeDisplayed()) {
      return null;
    }
    if (status.playbackState() == PlaybackState.PAUSED && !preferences.showPaused()) {
      return null;
    }

    String title = preferences.showTitle() ? status.title() : "";
    String artist = preferences.showArtist() ? status.artist() : "";
    if (title.isBlank() && artist.isBlank()) {
      return null;
    }

    String primary = title.isBlank() ? artist : title;
    String secondary = title.isBlank() ? "" : artist;
    int textWidth = preferences.maxTextLength();
    primary = marquee(primary, textWidth, animationFrame);
    secondary = marquee(secondary, textWidth, animationFrame + 2);

    String[] indicator = indicator(
        status.playbackState(),
        preferences.showMusicIcon(),
        animationFrame
    );
    return new NametagLines(
        indicator[0],
        indicator[1],
        primary,
        secondary,
        status.playbackState() == PlaybackState.PAUSED
    );
  }

  static String marquee(String text, int width, long animationFrame) {
    Objects.requireNonNull(text, "text");
    int[] codePoints = text.codePoints().toArray();
    if (codePoints.length <= width) {
      return text;
    }

    int travel = codePoints.length - width;
    int startPause = 5;
    int endPause = 4;
    int cycleLength = startPause + travel + endPause + travel;
    int phase = Math.floorMod(animationFrame, cycleLength);

    int offset;
    if (phase < startPause) {
      offset = 0;
    } else if (phase < startPause + travel) {
      offset = phase - startPause + 1;
    } else if (phase < startPause + travel + endPause) {
      offset = travel;
    } else {
      offset = travel - (phase - startPause - travel - endPause) - 1;
    }

    return new String(codePoints, offset, width);
  }

  private static String[] indicator(
      PlaybackState playbackState,
      boolean visible,
      long animationFrame
  ) {
    if (!visible) {
      return new String[]{"", ""};
    }
    if (playbackState == PlaybackState.PAUSED) {
      return PAUSED_INDICATOR;
    }
    int index = Math.floorMod(animationFrame, EQUALIZER_FRAMES.length);
    return EQUALIZER_FRAMES[index];
  }

  public record NametagLines(
      String topIndicator,
      String bottomIndicator,
      String primary,
      String secondary,
      boolean paused
  ) {
  }
}
