package de.zunade.tidalnowplaying.cache;

import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe UUID cache shared by local rendering and the future network transport.
 *
 * <p>The externally visible {@code updatedAt} changes only when the song or playback state changes.
 * An internal last-seen timestamp is refreshed on identical polls so unchanged local tracks do not
 * expire or trigger rendering updates.</p>
 */
public final class PlayerMusicStatusCache {

  private final ConcurrentHashMap<UUID, CacheEntry> entries = new ConcurrentHashMap<>();
  private final long timeToLiveMillis;

  public PlayerMusicStatusCache(Duration timeToLive) {
    long requested = Objects.requireNonNull(timeToLive, "timeToLive").toMillis();
    if (requested < 1_000) {
      throw new IllegalArgumentException("timeToLive must be at least one second");
    }
    this.timeToLiveMillis = requested;
  }

  public boolean putIfChanged(PlayerMusicStatus status) {
    Objects.requireNonNull(status, "status");
    long now = System.currentTimeMillis();
    ChangeFlag result = new ChangeFlag();

    this.entries.compute(status.playerUuid(), (ignored, current) -> {
      if (current != null && current.status.sameContent(status)) {
        current.lastSeenAt = now;
        return current;
      }
      result.changed = true;
      return new CacheEntry(status, now);
    });
    return result.changed;
  }

  public Optional<PlayerMusicStatus> get(UUID playerUuid) {
    return Optional.ofNullable(this.getOrNull(playerUuid));
  }

  /**
   * Allocation-free hot-path accessor used while entity snapshots are captured.
   */
  public PlayerMusicStatus getOrNull(UUID playerUuid) {
    if (playerUuid == null) {
      return null;
    }

    CacheEntry entry = this.entries.get(playerUuid);
    if (entry == null) {
      return null;
    }
    if (isExpired(entry, System.currentTimeMillis())) {
      this.entries.remove(playerUuid, entry);
      return null;
    }
    return entry.status;
  }

  public void remove(UUID playerUuid) {
    if (playerUuid != null) {
      this.entries.remove(playerUuid);
    }
  }

  public int removeExpired() {
    long now = System.currentTimeMillis();
    int before = this.entries.size();
    this.entries.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    return before - this.entries.size();
  }

  public void clear() {
    this.entries.clear();
  }

  public int size() {
    return this.entries.size();
  }

  private boolean isExpired(CacheEntry entry, long now) {
    return now - entry.lastSeenAt > this.timeToLiveMillis;
  }

  private static final class CacheEntry {

    private final PlayerMusicStatus status;
    private volatile long lastSeenAt;

    private CacheEntry(PlayerMusicStatus status, long lastSeenAt) {
      this.status = status;
      this.lastSeenAt = lastSeenAt;
    }
  }

  private static final class ChangeFlag {

    private boolean changed;
  }
}
