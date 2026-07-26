package de.zunade.tidalnowplaying.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zunade.tidalnowplaying.media.PlaybackState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MusicStatusPayloadValidatorTest {

  @Test
  void sanitizesAndAcceptsBoundedPayload() {
    MusicStatusPayloadValidator validator = new MusicStatusPayloadValidator();
    UUID player = UUID.randomUUID();
    long now = System.currentTimeMillis();

    PlayerMusicStatus accepted = validator.validate(
        player,
        "§cArtist",
        "Song\u202E",
        PlaybackState.PLAYING,
        now,
        100,
        now
    ).orElseThrow();

    assertEquals("Artist", accepted.artist());
    assertEquals("Song", accepted.title());
  }

  @Test
  void rejectsOversizedAndRateLimitedPayloads() {
    MusicStatusPayloadValidator validator = new MusicStatusPayloadValidator();
    UUID player = UUID.randomUUID();
    long now = System.currentTimeMillis();

    assertTrue(validator.validate(
        player,
        "Artist",
        "Song",
        PlaybackState.PLAYING,
        now,
        PlayerMusicStatus.MAX_NETWORK_PAYLOAD_BYTES + 1,
        now
    ).isEmpty());

    validator.validate(
        player,
        "Artist",
        "Song",
        PlaybackState.PLAYING,
        now,
        100,
        now
    ).orElseThrow();
    assertTrue(validator.validate(
        player,
        "Artist",
        "Next",
        PlaybackState.PLAYING,
        now + 100,
        100,
        now + 100
    ).isEmpty());
  }
}
