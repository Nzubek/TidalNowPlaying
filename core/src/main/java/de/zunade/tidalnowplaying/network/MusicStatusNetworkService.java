package de.zunade.tidalnowplaying.network;

import java.util.UUID;

/**
 * Transport boundary for version 2. Version 1 binds the local-only implementation.
 */
public interface MusicStatusNetworkService extends AutoCloseable {

  void start();

  void publish(PlayerMusicStatus status);

  void playerLeft(UUID playerUuid);

  boolean supportsRemoteSharing();

  @Override
  void close();
}
