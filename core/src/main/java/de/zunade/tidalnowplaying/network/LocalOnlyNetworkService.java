package de.zunade.tidalnowplaying.network;

import de.zunade.tidalnowplaying.cache.PlayerMusicStatusCache;
import java.util.Objects;
import java.util.UUID;

public final class LocalOnlyNetworkService implements MusicStatusNetworkService {

  private final PlayerMusicStatusCache cache;

  public LocalOnlyNetworkService(PlayerMusicStatusCache cache) {
    this.cache = Objects.requireNonNull(cache, "cache");
  }

  @Override
  public void start() {
    // Version 1 intentionally has no socket, packet channel, or server dependency.
  }

  @Override
  public void publish(PlayerMusicStatus status) {
    // The local media service already writes the same immutable status to the shared cache.
  }

  @Override
  public void playerLeft(UUID playerUuid) {
    this.cache.remove(playerUuid);
  }

  @Override
  public boolean supportsRemoteSharing() {
    return false;
  }

  @Override
  public void close() {
    // Nothing external to close in local-only mode.
  }
}
