package de.zunade.tidalnowplaying.snapshot;

import de.zunade.tidalnowplaying.api.snapshot.MusicPlayerSnapshot;
import de.zunade.tidalnowplaying.api.snapshot.MusicSnapshotExtraKeys;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshotFactory;
import net.labymod.api.service.annotation.AutoService;

@AutoService(LabySnapshotFactory.class)
public final class MusicPlayerSnapshotFactory
    extends LabySnapshotFactory<Player, MusicPlayerSnapshot> {

  public MusicPlayerSnapshotFactory() {
    super(MusicSnapshotExtraKeys.MUSIC_PLAYER);
  }

  @Override
  protected MusicPlayerSnapshot create(Player player, Extras extras) {
    return new DefaultMusicPlayerSnapshot(player, extras);
  }
}
