package de.zunade.tidalnowplaying.media;

import de.zunade.tidalnowplaying.cache.PlayerMusicStatusCache;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class LocalPlayerTracker {

  private final AtomicReference<UUID> currentPlayer = new AtomicReference<>();
  private final PlayerMusicStatusCache cache;

  public LocalPlayerTracker(PlayerMusicStatusCache cache) {
    this.cache = Objects.requireNonNull(cache, "cache");
  }

  public UUID currentPlayerUuid() {
    return this.currentPlayer.get();
  }

  public void update(UUID playerUuid) {
    UUID previous = this.currentPlayer.getAndSet(playerUuid);
    if (previous != null && !previous.equals(playerUuid)) {
      this.cache.remove(previous);
    }
  }

  public void clear() {
    update(null);
  }
}
