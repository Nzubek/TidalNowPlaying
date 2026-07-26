package de.zunade.tidalnowplaying.media;

import de.zunade.tidalnowplaying.media.bridge.BridgeResponse;
import de.zunade.tidalnowplaying.media.bridge.PowerShellMediaBridge;
import java.time.Duration;
import java.util.Objects;

public final class WindowsMediaSessionProvider implements MediaProvider {

  private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(8);
  private final PowerShellMediaBridge bridge;

  public WindowsMediaSessionProvider(PowerShellMediaBridge bridge) {
    this.bridge = Objects.requireNonNull(bridge, "bridge");
  }

  @Override
  public String name() {
    return "Windows GSMTC";
  }

  @Override
  public MediaTrack query() {
    BridgeResponse response = this.bridge.request("MEDIA", QUERY_TIMEOUT);
    return new MediaTrack(
        response.artist(),
        response.title(),
        response.state(),
        System.currentTimeMillis(),
        response.source().isBlank() ? name() : response.source(),
        response.artworkPath(),
        response.artworkKey(),
        response.progress()
    );
  }

  @Override
  public void suspend() {
    this.bridge.suspend();
  }
}
