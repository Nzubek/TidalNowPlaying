package de.zunade.tidalnowplaying.media.bridge;

import de.zunade.tidalnowplaying.media.PlaybackState;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Parses the bounded ASCII protocol emitted by the bundled PowerShell bridge.
 */
public final class TidalWireParser {

  static final int MAX_LINE_LENGTH = 16_384;
  private static final int MAX_BASE64_FIELD_LENGTH = 4_096;

  private TidalWireParser() {
  }

  public static BridgeResponse parse(String line) {
    if (line == null || line.isBlank() || line.length() > MAX_LINE_LENGTH) {
      return BridgeResponse.state(PlaybackState.ERROR, "empty or oversized bridge response");
    }

    String[] fields = line.split("\\|", -1);
    try {
      if (fields.length == 8 && "RESULT".equals(fields[0])) {
        return new BridgeResponse(
            parseState(fields[1]),
            decode(fields[2]),
            decode(fields[3]),
            decode(fields[4]),
            decode(fields[5]),
            decode(fields[6]),
            parseProgress(fields[7]),
            ""
        );
      }
      if (fields.length == 5 && "RESULT".equals(fields[0])) {
        return new BridgeResponse(
            parseState(fields[1]),
            decode(fields[2]),
            decode(fields[3]),
            decode(fields[4]),
            ""
        );
      }
      if (fields.length == 3 && "STATE".equals(fields[0])) {
        return BridgeResponse.state(parseState(fields[1]), decode(fields[2]));
      }
    } catch (IllegalArgumentException ignored) {
      return BridgeResponse.state(PlaybackState.ERROR, "invalid bridge encoding");
    }

    return BridgeResponse.state(PlaybackState.ERROR, "invalid bridge response");
  }

  private static double parseProgress(String value) {
    if (value == null || value.isBlank()) {
      return 0.0;
    }
    double progress = Double.parseDouble(value);
    if (!Double.isFinite(progress)) {
      throw new IllegalArgumentException("Invalid progress");
    }
    return Math.clamp(progress, 0.0, 1.0);
  }

  private static PlaybackState parseState(String value) {
    try {
      return PlaybackState.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return PlaybackState.ERROR;
    }
  }

  private static String decode(String value) {
    if (value.isEmpty()) {
      return "";
    }
    if (value.length() > MAX_BASE64_FIELD_LENGTH) {
      throw new IllegalArgumentException("Field too long");
    }
    byte[] decoded = Base64.getDecoder().decode(value);
    if (decoded.length > 3_072) {
      throw new IllegalArgumentException("Decoded field too long");
    }
    return new String(decoded, StandardCharsets.UTF_8);
  }
}
