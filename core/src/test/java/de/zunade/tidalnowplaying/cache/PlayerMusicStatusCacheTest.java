package de.zunade.tidalnowplaying.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.network.PlayerMusicStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerMusicStatusCacheTest {

  @Test
  void identicalHeartbeatKeepsOriginalImmutableStatus() {
    PlayerMusicStatusCache cache = new PlayerMusicStatusCache(Duration.ofSeconds(20));
    UUID player = UUID.randomUUID();
    PlayerMusicStatus first =
        new PlayerMusicStatus(player, "Artist", "Title", PlaybackState.PLAYING, 10);
    PlayerMusicStatus heartbeat =
        new PlayerMusicStatus(player, "Artist", "Title", PlaybackState.PLAYING, 20);

    assertTrue(cache.putIfChanged(first));
    assertFalse(cache.putIfChanged(heartbeat));
    assertSame(first, cache.getOrNull(player));
  }

  @Test
  void playbackChangesReplaceTheStatus() {
    PlayerMusicStatusCache cache = new PlayerMusicStatusCache(Duration.ofSeconds(20));
    UUID player = UUID.randomUUID();

    assertTrue(cache.putIfChanged(
        new PlayerMusicStatus(player, "Artist", "Title", PlaybackState.PLAYING, 10)
    ));
    assertTrue(cache.putIfChanged(
        new PlayerMusicStatus(player, "Artist", "Title", PlaybackState.PAUSED, 20)
    ));
  }
}
