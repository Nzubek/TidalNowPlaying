package de.zunade.tidalnowplaying.media.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.zunade.tidalnowplaying.media.PlaybackState;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TidalWireParserTest {

  @Test
  void parsesUnicodeMetadata() {
    BridgeResponse response = TidalWireParser.parse(
        "RESULT|PLAYING|" + encoded("Björk") + "|" + encoded("Jóga") + "|" + encoded("TIDAL")
    );

    assertEquals(PlaybackState.PLAYING, response.state());
    assertEquals("Björk", response.artist());
    assertEquals("Jóga", response.title());
  }

  @Test
  void rejectsMalformedProtocol() {
    assertEquals(
        PlaybackState.ERROR,
        TidalWireParser.parse("not-a-valid-response").state()
    );
  }

  @Test
  void parsesArtworkAndProgress() {
    BridgeResponse response = TidalWireParser.parse(
        "RESULT|PLAYING|"
            + encoded("Björk") + "|"
            + encoded("Jóga") + "|"
            + encoded("TIDAL") + "|"
            + encoded("C:\\Temp\\tidal-now-playing-cover-12.img") + "|"
            + encoded("abc123") + "|0.625"
    );

    assertEquals("C:\\Temp\\tidal-now-playing-cover-12.img", response.artworkPath());
    assertEquals("abc123", response.artworkKey());
    assertEquals(0.625, response.progress());
  }

  @Test
  void rejectsInvalidProgress() {
    BridgeResponse response = TidalWireParser.parse(
        "RESULT|PLAYING|"
            + encoded("Artist") + "|"
            + encoded("Title") + "|"
            + encoded("TIDAL") + "|"
            + encoded("") + "|"
            + encoded("") + "|NaN"
    );

    assertEquals(PlaybackState.ERROR, response.state());
  }

  private static String encoded(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
