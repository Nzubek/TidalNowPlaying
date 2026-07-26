package de.zunade.tidalnowplaying.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TextSanitizerTest {

  @Test
  void removesMinecraftCodesControlsAndBidiOverrides() {
    String sanitized = TextSanitizer.externalText(
        "§cLinkin\u0000 Park \u202E - &aNumb",
        128
    );

    assertEquals("Linkin Park - Numb", sanitized);
    assertFalse(sanitized.contains("§"));
  }

  @Test
  void truncatesByCodePointWithoutSplittingEmoji() {
    assertEquals("A😀B...", TextSanitizer.truncate("A😀BCDEFG", 6));
  }

  @Test
  void keepsPlaceholdersButRemovesFormatting() {
    assertEquals(
        "{icon} {artist}",
        TextSanitizer.displayTemplate("§c{icon} \u202E{artist}")
    );
  }
}
