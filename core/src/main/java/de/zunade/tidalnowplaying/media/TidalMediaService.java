package de.zunade.tidalnowplaying.media;

import de.zunade.tidalnowplaying.cache.PlayerMusicStatusCache;
import de.zunade.tidalnowplaying.config.TidalAddonConfig;
import de.zunade.tidalnowplaying.network.MusicStatusNetworkService;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import de.zunade.tidalnowplaying.util.RateLimitedLogger;
import de.zunade.tidalnowplaying.util.TextSanitizer;
import de.zunade.tidalnowplaying.util.ThreadingUtil;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polls providers off-thread and publishes immutable, sanitized status changes to the UUID cache.
 */
public final class TidalMediaService implements AutoCloseable {

  private static final long SCHEDULER_RESOLUTION_MILLIS = 250;

  private final TidalAddonConfig config;
  private final LocalPlayerTracker playerTracker;
  private final MediaProvider primaryProvider;
  private final MediaProvider fallbackProvider;
  private final PlayerMusicStatusCache cache;
  private final MusicStatusNetworkService networkService;
  private final MusicVisualSink visualSink;
  private final RateLimitedLogger logger;
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadingUtil.daemonFactory("tidal-media-poller-")
      );
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicReference<MediaTrack> latestTrack = new AtomicReference<>(
      MediaTrack.state(PlaybackState.NO_TRACK, "not polled")
  );

  private volatile long nextPollNanos;
  private boolean providersSuspended = true;

  public TidalMediaService(
      TidalAddonConfig config,
      LocalPlayerTracker playerTracker,
      MediaProvider primaryProvider,
      MediaProvider fallbackProvider,
      PlayerMusicStatusCache cache,
      MusicStatusNetworkService networkService,
      MusicVisualSink visualSink,
      RateLimitedLogger logger
  ) {
    this.config = Objects.requireNonNull(config, "config");
    this.playerTracker = Objects.requireNonNull(playerTracker, "playerTracker");
    this.primaryProvider = Objects.requireNonNull(primaryProvider, "primaryProvider");
    this.fallbackProvider = Objects.requireNonNull(fallbackProvider, "fallbackProvider");
    this.cache = Objects.requireNonNull(cache, "cache");
    this.networkService = Objects.requireNonNull(networkService, "networkService");
    this.visualSink = Objects.requireNonNull(visualSink, "visualSink");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public void start() {
    if (!this.started.compareAndSet(false, true)) {
      return;
    }
    this.executor.scheduleWithFixedDelay(
        this::pollIfDue,
        0,
        SCHEDULER_RESOLUTION_MILLIS,
        TimeUnit.MILLISECONDS
    );
  }

  public MediaTrack latestTrack() {
    return this.latestTrack.get();
  }

  private void pollIfDue() {
    if (this.closed.get()) {
      return;
    }
    long now = System.nanoTime();
    if (now < this.nextPollNanos) {
      return;
    }

    int intervalMillis = Math.clamp(this.config.updateIntervalMillis().get(), 1000, 5000);
    this.nextPollNanos = now + TimeUnit.MILLISECONDS.toNanos(intervalMillis);
    try {
      poll();
    } catch (Throwable error) {
      // Linkage errors from optional platform APIs must not terminate the scheduled executor.
      this.logger.warn("media-poll", "TIDAL media poll failed safely", error);
    }
  }

  private void poll() {
    UUID playerUuid = this.playerTracker.currentPlayerUuid();
    if (playerUuid == null) {
      suspendProviders();
      return;
    }

    if (!this.config.enabled().get()
        || !this.config.showMusicStatus().get()
        || !this.config.showOwnStatus().get()) {
      this.cache.remove(playerUuid);
      this.visualSink.removeVisual(playerUuid);
      suspendProviders();
      return;
    }

    this.providersSuspended = false;
    MediaTrack track = queryProviders();
    track = new MediaTrack(
        TextSanitizer.externalText(
            track.artist(),
            PlayerMusicStatus.MAX_ARTIST_CODE_POINTS
        ),
        TextSanitizer.externalText(
            track.title(),
            PlayerMusicStatus.MAX_TITLE_CODE_POINTS
        ),
        track.playbackState(),
        track.observedAt(),
        TextSanitizer.externalText(track.source(), 128),
        track.artworkPath(),
        TextSanitizer.externalText(track.artworkKey(), 128),
        track.progress()
    );
    updateLatestTrack(track);

    if (!track.playbackState().canBeDisplayed() || !track.hasMetadata()) {
      this.cache.remove(playerUuid);
      this.visualSink.removeVisual(playerUuid);
      return;
    }

    this.visualSink.updateVisual(playerUuid, track);

    PlayerMusicStatus status = new PlayerMusicStatus(
        playerUuid,
        track.artist(),
        track.title(),
        track.playbackState(),
        System.currentTimeMillis()
    );
    boolean changed = this.cache.putIfChanged(status);
    if (changed) {
      this.logger.debug(
          "TIDAL status changed: {} - {} ({}, source: {})",
          status.artist(),
          status.title(),
          status.playbackState(),
          track.source()
      );
      if (this.config.shareWithOthers().get()) {
        this.networkService.publish(status);
      }
    }
  }

  private MediaTrack queryProviders() {
    MediaTrack primary;
    try {
      primary = this.primaryProvider.query();
    } catch (Throwable error) {
      this.logger.warn("primary-provider", "Windows media-session query failed", error);
      primary = MediaTrack.state(PlaybackState.ERROR, this.primaryProvider.name());
    }

    if (primary.playbackState() != PlaybackState.SESSION_UNAVAILABLE
        && primary.playbackState() != PlaybackState.ERROR) {
      return primary;
    }

    try {
      MediaTrack fallback = this.fallbackProvider.query();
      if (fallback.hasMetadata()
          || fallback.playbackState() != PlaybackState.ERROR) {
        return fallback;
      }
    } catch (Throwable error) {
      this.logger.warn("fallback-provider", "TIDAL window-title fallback failed", error);
    }
    return primary;
  }

  private void updateLatestTrack(MediaTrack track) {
    MediaTrack previous = this.latestTrack.get();
    if (!track.sameContent(previous)) {
      this.latestTrack.set(track);
    } else if (track.progress() != previous.progress()) {
      this.latestTrack.set(track);
    }
  }

  @Override
  public void close() {
    if (!this.closed.compareAndSet(false, true)) {
      return;
    }

    this.executor.shutdownNow();
    try {
      this.executor.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }

    closeProvider(this.primaryProvider);
    closeProvider(this.fallbackProvider);
    this.networkService.close();
    UUID playerUuid = this.playerTracker.currentPlayerUuid();
    if (playerUuid != null) {
      this.cache.remove(playerUuid);
      this.visualSink.removeVisual(playerUuid);
    }
  }

  private void closeProvider(MediaProvider provider) {
    try {
      provider.close();
    } catch (Exception error) {
      this.logger.warn("provider-close-" + provider.name(), "Could not close " + provider.name(), error);
    }
  }

  private void suspendProviders() {
    if (this.providersSuspended) {
      return;
    }
    this.providersSuspended = true;
    try {
      this.primaryProvider.suspend();
    } catch (Throwable error) {
      this.logger.warn("primary-provider-suspend", "Could not suspend media detection", error);
    }
    try {
      this.fallbackProvider.suspend();
    } catch (Throwable error) {
      this.logger.warn("fallback-provider-suspend", "Could not suspend fallback detection", error);
    }
  }
}
