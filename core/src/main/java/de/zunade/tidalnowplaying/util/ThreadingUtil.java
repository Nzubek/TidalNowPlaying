package de.zunade.tidalnowplaying.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadingUtil {

  private ThreadingUtil() {
  }

  public static ThreadFactory daemonFactory(String prefix) {
    AtomicInteger sequence = new AtomicInteger();
    return task -> {
      Thread thread = new Thread(task, prefix + sequence.incrementAndGet());
      thread.setDaemon(true);
      thread.setUncaughtExceptionHandler((ignored, error) -> {
        // Scheduled tasks perform their own rate-limited logging. Never crash Minecraft here.
      });
      return thread;
    };
  }
}
