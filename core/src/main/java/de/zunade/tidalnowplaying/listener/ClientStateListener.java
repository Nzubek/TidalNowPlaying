package de.zunade.tidalnowplaying.listener;

import de.zunade.tidalnowplaying.TidalNowPlayingAddon;
import de.zunade.tidalnowplaying.cache.PlayerMusicStatusCache;
import de.zunade.tidalnowplaying.media.LocalPlayerTracker;
import de.zunade.tidalnowplaying.nametag.MusicNametagService;
import de.zunade.tidalnowplaying.network.MusicStatusNetworkService;
import java.util.UUID;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.addon.lifecycle.AddonUnloadEvent;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.lifecycle.ShutdownEvent;
import net.labymod.api.event.client.network.playerinfo.PlayerInfoRemoveEvent;

@SuppressWarnings("deprecation")
public final class ClientStateListener {

  private final TidalNowPlayingAddon addon;
  private final LocalPlayerTracker playerTracker;
  private final PlayerMusicStatusCache cache;
  private final MusicNametagService nametagService;
  private final MusicStatusNetworkService networkService;
  private int ticks;

  public ClientStateListener(
      TidalNowPlayingAddon addon,
      LocalPlayerTracker playerTracker,
      PlayerMusicStatusCache cache,
      MusicNametagService nametagService,
      MusicStatusNetworkService networkService
  ) {
    this.addon = addon;
    this.playerTracker = playerTracker;
    this.cache = cache;
    this.nametagService = nametagService;
    this.networkService = networkService;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (event.phase() != Phase.PRE) {
      return;
    }

    ClientPlayer player = this.addon.labyAPI().minecraft().clientPlayer();
    this.playerTracker.update(player == null ? null : player.getUniqueId());

    this.ticks++;
    if (this.ticks % 20 == 0) {
      this.nametagService.refreshConfiguration();
    }
    if (this.ticks % 100 == 0) {
      this.cache.removeExpired();
    }
  }

  @Subscribe
  public void onPlayerInfoRemove(PlayerInfoRemoveEvent event) {
    UUID playerUuid = event.playerInfo().profile().getUniqueId();
    this.networkService.playerLeft(playerUuid);
    this.nametagService.invalidate(playerUuid);
  }

  @Subscribe
  public void onGameShutdown(ShutdownEvent event) {
    this.addon.shutdownServices();
  }

  @Subscribe
  public void onAddonUnload(AddonUnloadEvent event) {
    this.addon.shutdownServices();
  }
}
