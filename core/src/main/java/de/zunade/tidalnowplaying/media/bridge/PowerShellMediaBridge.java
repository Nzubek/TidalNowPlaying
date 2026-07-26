package de.zunade.tidalnowplaying.media.bridge;

import de.zunade.tidalnowplaying.media.PlaybackState;
import de.zunade.tidalnowplaying.util.RateLimitedLogger;
import de.zunade.tidalnowplaying.util.ThreadingUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Owns one persistent PowerShell process. One process serves all GSMTC and window-title polls.
 */
public final class PowerShellMediaBridge implements AutoCloseable {

  private static final String SCRIPT_RESOURCE =
      "/assets/tidalnowplaying/bridge/tidal-media-bridge.ps1";
  private static final long RETRY_DELAY_NANOS = Duration.ofSeconds(30).toNanos();

  private final RateLimitedLogger logger;
  private final BlockingQueue<String> responses = new LinkedBlockingQueue<>(16);
  private final boolean windows;

  private Process process;
  private BufferedWriter commandWriter;
  private Path extractedScript;
  private long retryAfterNanos;
  private boolean closed;

  public PowerShellMediaBridge(RateLimitedLogger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.windows = System.getProperty("os.name", "")
        .toLowerCase(Locale.ROOT)
        .startsWith("windows");
  }

  public synchronized BridgeResponse request(String command, Duration timeout) {
    if (this.closed) {
      return BridgeResponse.state(PlaybackState.ERROR, "bridge closed");
    }
    if (!this.windows) {
      return BridgeResponse.state(
          PlaybackState.UNSUPPORTED_PLATFORM,
          "TIDAL detection is supported on Windows only"
      );
    }
    if (!"MEDIA".equals(command) && !"WINDOW".equals(command)) {
      return BridgeResponse.state(PlaybackState.ERROR, "invalid command");
    }
    if (System.nanoTime() < this.retryAfterNanos) {
      return BridgeResponse.state(PlaybackState.ERROR, "bridge restart backoff active");
    }

    try {
      ensureStarted();
      this.commandWriter.write(command);
      this.commandWriter.newLine();
      this.commandWriter.flush();

      String line = this.responses.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (line == null) {
        throw new IOException("PowerShell bridge timed out");
      }
      BridgeResponse response = TidalWireParser.parse(line);
      if (response.state() == PlaybackState.ERROR && !response.diagnostic().isBlank()) {
        this.logger.warn("bridge-response", "TIDAL bridge: " + response.diagnostic(), null);
      }
      return response;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return BridgeResponse.state(PlaybackState.ERROR, "bridge request interrupted");
    } catch (Exception exception) {
      this.logger.warn("bridge-request", "TIDAL PowerShell bridge failed", exception);
      stopProcess();
      this.retryAfterNanos = System.nanoTime() + RETRY_DELAY_NANOS;
      return BridgeResponse.state(PlaybackState.ERROR, exception.getMessage());
    }
  }

  private void ensureStarted() throws IOException {
    if (this.process != null && this.process.isAlive()) {
      return;
    }

    if (this.extractedScript == null || !Files.isRegularFile(this.extractedScript)) {
      this.extractedScript = extractScript();
    }

    ProcessBuilder builder = new ProcessBuilder(
        "powershell.exe",
        "-NoLogo",
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        this.extractedScript.toAbsolutePath().toString()
    );
    builder.redirectErrorStream(false);
    this.process = builder.start();
    this.commandWriter = new BufferedWriter(new OutputStreamWriter(
        this.process.getOutputStream(),
        StandardCharsets.UTF_8
    ));
    this.responses.clear();

    startOutputReader(this.process.getInputStream());
    startErrorReader(this.process.getErrorStream());
    this.logger.debug("Started persistent TIDAL PowerShell bridge");
  }

  private Path extractScript() throws IOException {
    try (InputStream input = PowerShellMediaBridge.class.getResourceAsStream(SCRIPT_RESOURCE)) {
      if (input == null) {
        throw new IOException("Bundled media bridge resource is missing");
      }
      Path script = Files.createTempFile("tidal-now-playing-bridge-", ".ps1");
      Files.copy(input, script, StandardCopyOption.REPLACE_EXISTING);
      script.toFile().deleteOnExit();
      return script;
    }
  }

  private void startOutputReader(InputStream stream) {
    Thread reader = ThreadingUtil.daemonFactory("tidal-bridge-output-").newThread(() -> {
      try (BufferedReader input = new BufferedReader(
          new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = input.readLine()) != null) {
          if (!line.isBlank()) {
            if (line.length() <= TidalWireParser.MAX_LINE_LENGTH) {
              this.responses.offer(line);
            } else {
              this.logger.warn("bridge-line", "Discarded oversized TIDAL bridge response", null);
            }
          }
        }
      } catch (IOException exception) {
        if (!this.closed) {
          this.logger.warn("bridge-output", "TIDAL bridge output stream closed", exception);
        }
      }
    });
    reader.start();
  }

  private void startErrorReader(InputStream stream) {
    Thread reader = ThreadingUtil.daemonFactory("tidal-bridge-error-").newThread(() -> {
      try (BufferedReader input = new BufferedReader(
          new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = input.readLine()) != null) {
          if (!line.isBlank()) {
            this.logger.warn("bridge-stderr", "TIDAL bridge stderr: " + line, null);
          }
        }
      } catch (IOException exception) {
        if (!this.closed) {
          this.logger.warn("bridge-stderr-read", "TIDAL bridge error stream closed", exception);
        }
      }
    });
    reader.start();
  }

  @Override
  public synchronized void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;

    if (this.commandWriter != null && this.process != null && this.process.isAlive()) {
      try {
        this.commandWriter.write("EXIT");
        this.commandWriter.newLine();
        this.commandWriter.flush();
        this.process.waitFor(500, TimeUnit.MILLISECONDS);
      } catch (IOException exception) {
        this.logger.debug("Could not send EXIT to TIDAL bridge: {}", exception.getMessage());
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }
    stopProcess();
    removeExtractedScript();
  }

  /**
   * Releases the helper process while keeping this bridge restartable after a settings change.
   */
  public synchronized void suspend() {
    if (this.closed) {
      return;
    }
    stopProcess();
    removeExtractedScript();
    this.retryAfterNanos = 0;
  }

  private void stopProcess() {
    if (this.commandWriter != null) {
      try {
        this.commandWriter.close();
      } catch (IOException ignored) {
        // The process may already have terminated.
      }
      this.commandWriter = null;
    }
    if (this.process != null) {
      if (this.process.isAlive()) {
        this.process.destroy();
        try {
          if (!this.process.waitFor(250, TimeUnit.MILLISECONDS)) {
            this.process.destroyForcibly();
          }
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          this.process.destroyForcibly();
        }
      }
      this.process = null;
    }
    this.responses.clear();
  }

  private void removeExtractedScript() {
    if (this.extractedScript == null) {
      return;
    }
    try {
      Files.deleteIfExists(this.extractedScript);
    } catch (IOException exception) {
      this.logger.debug("Could not remove temporary TIDAL bridge: {}", exception.getMessage());
    }
    this.extractedScript = null;
  }
}
