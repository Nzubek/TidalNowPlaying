package de.zunade.tidalnowplaying.nametag;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import de.zunade.tidalnowplaying.util.TextSanitizer;
import java.util.regex.Pattern;

public final class MusicStatusFormatter {

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private MusicStatusFormatter() {
  }

  public static String format(PlayerMusicStatus status, DisplayPreferences preferences) {
    if (status == null || !preferences.enabled() || !status.playbackState().canBeDisplayed()) {
      return null;
    }
    if (status.playbackState() == PlaybackState.PAUSED && !preferences.showPaused()) {
      return null;
    }

    String artist = preferences.showArtist() ? status.artist() : "";
    String title = preferences.showTitle() ? status.title() : "";
    if (artist.isBlank() && title.isBlank()) {
      return null;
    }

    String icon = "";
    if (status.playbackState() == PlaybackState.PAUSED && preferences.showPauseSymbol()) {
      icon = "⏸";
    } else if (preferences.showMusicIcon()) {
      icon = "♫";
    }

    String separator = !artist.isBlank() && !title.isBlank() ? "–" : "";
    String state = switch (status.playbackState()) {
      case PLAYING -> "playing";
      case PAUSED -> "paused";
      case UNKNOWN -> "unknown";
      default -> "";
    };

    String rendered = preferences.format()
        .replace("{icon}", icon)
        .replace("{artist}", artist)
        .replace("{title}", title)
        .replace("{separator}", separator)
        .replace("{status}", state);
    rendered = WHITESPACE.matcher(rendered).replaceAll(" ").trim();
    if (rendered.isBlank()) {
      return null;
    }
    return TextSanitizer.truncate(rendered, preferences.maxTextLength());
  }
}
