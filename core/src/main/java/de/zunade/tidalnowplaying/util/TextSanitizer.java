package de.zunade.tidalnowplaying.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextSanitizer {

  private static final Pattern MINECRAFT_FORMATTING =
      Pattern.compile("(?i)(?:§|&)[0-9A-FK-ORX]");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  private TextSanitizer() {
  }

  public static String externalText(String value, int maxCodePoints) {
    if (value == null || value.isBlank() || maxCodePoints <= 0) {
      return "";
    }

    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
    normalized = MINECRAFT_FORMATTING.matcher(normalized).replaceAll("");

    StringBuilder safe = new StringBuilder(Math.min(normalized.length(), maxCodePoints));
    normalized.codePoints().forEach(codePoint -> {
      int type = Character.getType(codePoint);
      if (type == Character.CONTROL
          || type == Character.FORMAT
          || type == Character.PRIVATE_USE
          || type == Character.SURROGATE
          || type == Character.UNASSIGNED
          || isNonCharacter(codePoint)) {
        return;
      }
      safe.appendCodePoint(codePoint);
    });

    String compact = WHITESPACE.matcher(safe.toString()).replaceAll(" ").trim();
    return truncate(compact, maxCodePoints);
  }

  public static String displayTemplate(String value) {
    if (value == null) {
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
    normalized = MINECRAFT_FORMATTING.matcher(normalized).replaceAll("");

    StringBuilder safe = new StringBuilder(Math.min(normalized.length(), 256));
    normalized.codePoints().limit(256).forEach(codePoint -> {
      int type = Character.getType(codePoint);
      if (type != Character.CONTROL
          && type != Character.FORMAT
          && type != Character.PRIVATE_USE
          && !isNonCharacter(codePoint)) {
        safe.appendCodePoint(codePoint);
      }
    });
    return safe.toString().trim();
  }

  public static String truncate(String value, int maxCodePoints) {
    if (value == null || value.isEmpty() || maxCodePoints <= 0) {
      return "";
    }

    int count = value.codePointCount(0, value.length());
    if (count <= maxCodePoints) {
      return value;
    }
    if (maxCodePoints <= 3) {
      return ".".repeat(maxCodePoints);
    }

    int end = value.offsetByCodePoints(0, maxCodePoints - 3);
    return value.substring(0, end).stripTrailing() + "...";
  }

  public static String safeDiagnostic(String value) {
    return externalText(value, 240).toLowerCase(Locale.ROOT);
  }

  private static boolean isNonCharacter(int codePoint) {
    return (codePoint & 0xFFFE) == 0xFFFE || (codePoint >= 0xFDD0 && codePoint <= 0xFDEF);
  }
}
