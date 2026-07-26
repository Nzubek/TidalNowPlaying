package de.zunade.tidalnowplaying;

import de.zunade.tidalnowplaying.cache.PlayerMusicStatusCache;
import de.zunade.tidalnowplaying.config.TidalAddonConfig;
import de.zunade.tidalnowplaying.listener.ClientStateListener;
import de.zunade.tidalnowplaying.media.LocalPlayerTracker;
import de.zunade.tidalnowplaying.media.TidalMediaService;
import de.zunade.tidalnowplaying.media.WindowTitleFallbackProvider;
import de.zunade.tidalnowplaying.media.WindowsMediaSessionProvider;
import de.zunade.tidalnowplaying.media.bridge.PowerShellMediaBridge;
import de.zunade.tidalnowplaying.nametag.MusicNametagRenderer;
import de.zunade.tidalnowplaying.nametag.MusicNametagService;
import de.zunade.tidalnowplaying.nametag.MusicStatusSnapshotBridge;
import de.zunade.tidalnowplaying.network.LocalOnlyNetworkService;
import de.zunade.tidalnowplaying.network.MusicStatusNetworkService;
import de.zunade.tidalnowplaying.util.RateLimitedLogger;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.entity.player.tag.PositionType;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public final class TidalNowPlayingAddon extends LabyAddon<TidalAddonConfig> {

  private static final String TAG_ID = "tidalnowplaying_music_status";

  private final AtomicBoolean servicesClosed = new AtomicBoolean();
  private PlayerMusicStatusCache statusCache;
  private MusicStatusNetworkService networkService;
  private MusicNametagService nametagService;
  private MusicNametagRenderer nametagRenderer;
  private TidalMediaService mediaService;
  private PowerShellMediaBridge mediaBridge;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    RateLimitedLogger safeLogger = new RateLimitedLogger(
        this.logger(),
        () -> this.configuration().debugMode().get(),
        Duration.ofSeconds(30)
    );
    this.statusCache = new PlayerMusicStatusCache(Duration.ofSeconds(20));
    this.networkService = new LocalOnlyNetworkService(this.statusCache);
    this.networkService.start();

    LocalPlayerTracker playerTracker = new LocalPlayerTracker(this.statusCache);
    this.nametagService = new MusicNametagService(this.statusCache, this.configuration());
    MusicStatusSnapshotBridge.bind(this.nametagService);

    this.mediaBridge = new PowerShellMediaBridge(safeLogger);
    this.mediaService = new TidalMediaService(
        this.configuration(),
        playerTracker,
        new WindowsMediaSessionProvider(this.mediaBridge),
        new WindowTitleFallbackProvider(this.mediaBridge),
        this.statusCache,
        this.networkService,
        this.nametagService,
        safeLogger
    );

    this.nametagRenderer = new MusicNametagRenderer(this.configuration());
    Laby.references().tagRegistry().register(
        TAG_ID,
        PositionType.BELOW_NAME,
        this.nametagRenderer
    );
    this.registerListener(new ClientStateListener(
        this,
        playerTracker,
        this.statusCache,
        this.nametagService,
        this.networkService
    ));

    this.mediaService.start();
    this.logger().info("TIDAL Now Playing enabled (local-only mode)");
  }

  public void shutdownServices() {
    if (!this.servicesClosed.compareAndSet(false, true)) {
      return;
    }

    if (this.mediaService != null) {
      this.mediaService.close();
    }
    if (this.mediaBridge != null) {
      this.mediaBridge.close();
    }
    if (this.nametagService != null) {
      MusicStatusSnapshotBridge.unbind(this.nametagService);
      this.nametagService.clear();
    }
    if (this.nametagRenderer != null) {
      this.nametagRenderer.close();
    }
    if (this.statusCache != null) {
      this.statusCache.clear();
    }

    Laby.references().tagRegistry().unregister(TAG_ID);
    this.logger().info("TIDAL Now Playing stopped");
  }

  @Override
  protected Class<? extends TidalAddonConfig> configurationClass() {
    return TidalAddonConfig.class;
  }
}
