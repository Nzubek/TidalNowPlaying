package de.zunade.tidalnowplaying.media;

import de.zunade.tidalnowplaying.media.bridge.BridgeResponse;
import de.zunade.tidalnowplaying.media.bridge.PowerShellMediaBridge;
import java.time.Duration;
import java.util.Objects;

/**
 * Best-effort fallback. Window titles do not expose a reliable paused/playing distinction.
 */
public final class WindowTitleFallbackProvider implements MediaProvider {

  private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(3);
  private final PowerShellMediaBridge bridge;

  public WindowTitleFallbackProvider(PowerShellMediaBridge bridge) {
    this.bridge = Objects.requireNonNull(bridge, "bridge");
  }

  @Override
  public String name() {
    return "TIDAL window title fallback";
  }

  @Override
  public MediaTrack query() {
    BridgeResponse response = this.bridge.request("WINDOW", QUERY_TIMEOUT);
    return new MediaTrack(
        response.artist(),
        response.title(),
        response.state(),
        System.currentTimeMillis(),
        name()
    );
  }

  @Override
  public void suspend() {
    this.bridge.suspend();
  }
}
