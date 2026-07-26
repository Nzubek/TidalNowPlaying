package de.zunade.tidalnowplaying.api.snapshot;

import net.labymod.api.laby3d.renderer.snapshot.ExtraKey;

public final class MusicSnapshotExtraKeys {

  public static final ExtraKey<MusicPlayerSnapshot> MUSIC_PLAYER =
      ExtraKey.of("music_player", MusicPlayerSnapshot.class);

  private MusicSnapshotExtraKeys() {
  }
}
