package de.zunade.tidalnowplaying.nametag;

import de.zunade.tidalnowplaying.cache.PlayerMusicStatusCache;
import de.zunade.tidalnowplaying.config.TidalAddonConfig;
import de.zunade.tidalnowplaying.media.MediaTrack;
import de.zunade.tidalnowplaying.media.MusicVisualSink;
import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;

/**
 * Converts cached status data to immutable components only when status or settings changed.
 */
public final class MusicNametagService implements MusicVisualSink {

  private final PlayerMusicStatusCache statusCache;
  private final TidalAddonConfig config;
  private final ConcurrentHashMap<UUID, CachedComponents> componentCache =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, TrackVisual> visualCache =
      new ConcurrentHashMap<>();
  private volatile DisplayPreferences preferences;

  public MusicNametagService(PlayerMusicStatusCache statusCache, TidalAddonConfig config) {
    this.statusCache = Objects.requireNonNull(statusCache, "statusCache");
    this.config = Objects.requireNonNull(config, "config");
    this.preferences = DisplayPreferences.from(config);
  }

  public void refreshConfiguration() {
    DisplayPreferences updated = DisplayPreferences.from(this.config);
    if (!updated.equals(this.preferences)) {
      this.preferences = updated;
    }
  }

  public List<Component> componentsFor(UUID playerUuid) {
    PlayerMusicStatus status = this.statusCache.getOrNull(playerUuid);
    if (status == null) {
      this.componentCache.remove(playerUuid);
      return Collections.emptyList();
    }

    DisplayPreferences currentPreferences = this.preferences;
    boolean needsMarquee = status.artist().codePointCount(0, status.artist().length())
        > currentPreferences.maxTextLength()
        || status.title().codePointCount(0, status.title().length())
        > currentPreferences.maxTextLength();
    long animationFrame = currentPreferences.animateNametag() && needsMarquee
        ? System.currentTimeMillis() / currentPreferences.animationSpeedMillis()
        : 0;
    CachedComponents cached = this.componentCache.get(playerUuid);
    if (cached != null
        && cached.status == status
        && cached.preferences == currentPreferences
        && cached.animationFrame == animationFrame) {
      return cached.components;
    }

    List<Component> components = currentPreferences.modernNametag()
        ? this.modernComponents(status, currentPreferences, animationFrame)
        : this.classicComponents(status, currentPreferences);
    this.componentCache.put(
        playerUuid,
        new CachedComponents(status, currentPreferences, animationFrame, components)
    );
    return components;
  }

  private List<Component> modernComponents(
      PlayerMusicStatus status,
      DisplayPreferences preferences,
      long animationFrame
  ) {
    AnimatedNametagFormatter.NametagLines lines =
        AnimatedNametagFormatter.format(status, preferences, animationFrame);
    if (lines == null) {
      return Collections.emptyList();
    }

    Component primary = Component.text(lines.primary(), NamedTextColor.WHITE);
    if (lines.secondary().isBlank()) {
      return List.of(primary);
    }

    Component secondary = Component.text(lines.secondary(), NamedTextColor.GRAY);
    return List.of(primary, secondary);
  }

  private List<Component> classicComponents(
      PlayerMusicStatus status,
      DisplayPreferences preferences
  ) {
    String text = MusicStatusFormatter.format(status, preferences);
    return text == null
        ? Collections.emptyList()
        : List.of(Component.text(text, NamedTextColor.AQUA));
  }

  @Override
  public void updateVisual(UUID playerUuid, MediaTrack track) {
    if (playerUuid == null || track == null) {
      return;
    }
    this.visualCache.put(
        playerUuid,
        new TrackVisual(
            track.artworkPath(),
            track.artworkKey(),
            track.progress(),
            track.playbackState() == PlaybackState.PAUSED
        )
    );
  }

  @Override
  public void removeVisual(UUID playerUuid) {
    if (playerUuid != null) {
      this.visualCache.remove(playerUuid);
    }
  }

  public NametagVisual visualFor(UUID playerUuid) {
    TrackVisual visual = this.visualCache.get(playerUuid);
    DisplayPreferences current = this.preferences;
    if (visual == null) {
      visual = TrackVisual.EMPTY;
    }
    boolean coverEnabled = this.config.displayTrackCover().get();
    return new NametagVisual(
        current.modernNametag(),
        current.showMusicIcon(),
        current.animateNametag(),
        current.animationSpeedMillis(),
        visual.paused,
        coverEnabled ? visual.artworkPath : "",
        coverEnabled ? visual.artworkKey : "",
        visual.progress
    );
  }

  public void invalidate(UUID playerUuid) {
    if (playerUuid != null) {
      this.componentCache.remove(playerUuid);
    }
  }

  public void clear() {
    this.componentCache.clear();
    this.visualCache.clear();
  }

  private record TrackVisual(
      String artworkPath,
      String artworkKey,
      double progress,
      boolean paused
  ) {

    private static final TrackVisual EMPTY = new TrackVisual("", "", 0.0, false);
  }

  private record CachedComponents(
      PlayerMusicStatus status,
      DisplayPreferences preferences,
      long animationFrame,
      List<Component> components
  ) {
  }
}
