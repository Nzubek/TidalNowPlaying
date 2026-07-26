package de.zunade.tidalnowplaying.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.labymod.api.util.logging.Logging;

public final class RateLimitedLogger {

  private final Logging logging;
  private final BooleanSupplier debugEnabled;
  private final long minimumIntervalNanos;
  private final ConcurrentHashMap<String, Long> lastWarnings = new ConcurrentHashMap<>();

  public RateLimitedLogger(Logging logging, BooleanSupplier debugEnabled, Duration minimumInterval) {
    this.logging = Objects.requireNonNull(logging, "logging");
    this.debugEnabled = Objects.requireNonNull(debugEnabled, "debugEnabled");
    this.minimumIntervalNanos = Objects.requireNonNull(minimumInterval, "minimumInterval").toNanos();
  }

  public void warn(String key, String message, Throwable error) {
    long now = System.nanoTime();
    Long previous = this.lastWarnings.putIfAbsent(key, now);
    if (previous != null) {
      if (now - previous < this.minimumIntervalNanos
          || !this.lastWarnings.replace(key, previous, now)) {
        return;
      }
    }

    if (error == null) {
      this.logging.warn(message);
    } else if (this.debugEnabled.getAsBoolean()) {
      this.logging.warn(message, error);
    } else {
      this.logging.warn("{} ({}: {})", message, error.getClass().getSimpleName(), error.getMessage());
    }
  }

  public void debug(String message, Object... arguments) {
    if (this.debugEnabled.getAsBoolean()) {
      this.logging.debug(message, arguments);
    }
  }
}
