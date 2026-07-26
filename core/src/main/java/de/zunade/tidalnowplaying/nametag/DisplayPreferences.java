package de.zunade.tidalnowplaying.nametag;

import de.zunade.tidalnowplaying.config.TidalAddonConfig;
import de.zunade.tidalnowplaying.util.TextSanitizer;
import java.util.Objects;

public record DisplayPreferences(
    boolean enabled,
    boolean showPaused,
    boolean showPauseSymbol,
    boolean showMusicIcon,
    boolean showArtist,
    boolean showTitle,
    boolean modernNametag,
    boolean animateNametag,
    int animationSpeedMillis,
    String format,
    int maxTextLength
) {

  public static final String DEFAULT_FORMAT = "{icon} {artist} {separator} {title}";

  public DisplayPreferences {
    format = Objects.requireNonNullElse(format, DEFAULT_FORMAT);
    animationSpeedMillis = Math.clamp(animationSpeedMillis, 100, 500);
    maxTextLength = Math.clamp(maxTextLength, 24, 160);
  }

  public static DisplayPreferences from(TidalAddonConfig config) {
    String configuredFormat = TextSanitizer.displayTemplate(config.displayFormat().get());
    if (configuredFormat.isBlank()) {
      configuredFormat = DEFAULT_FORMAT;
    }

    return new DisplayPreferences(
        config.enabled().get() && config.showMusicStatus().get(),
        config.showWhenPaused().get(),
        config.showPauseSymbol().get(),
        config.showMusicIcon().get(),
        config.showArtist().get(),
        config.showTitle().get(),
        config.modernNametag().get(),
        config.animateNametag().get(),
        config.animationSpeedMillis().get(),
        configuredFormat,
        config.maxTextLength().get()
    );
  }
}
