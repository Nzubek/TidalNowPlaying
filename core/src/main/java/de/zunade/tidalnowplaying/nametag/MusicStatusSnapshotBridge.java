package de.zunade.tidalnowplaying.nametag;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.labymod.api.client.component.Component;

/**
 * Narrow lifecycle-bound bridge required because LabyMod instantiates snapshot services itself.
 */
public final class MusicStatusSnapshotBridge {

  private static final AtomicReference<MusicNametagService> ACTIVE_SERVICE =
      new AtomicReference<>();

  private MusicStatusSnapshotBridge() {
  }

  public static void bind(MusicNametagService service) {
    ACTIVE_SERVICE.set(service);
  }

  public static void unbind(MusicNametagService service) {
    ACTIVE_SERVICE.compareAndSet(service, null);
  }

  public static List<Component> componentsFor(UUID playerUuid) {
    MusicNametagService service = ACTIVE_SERVICE.get();
    return service == null ? Collections.emptyList() : service.componentsFor(playerUuid);
  }

  public static NametagVisual visualFor(UUID playerUuid) {
    MusicNametagService service = ACTIVE_SERVICE.get();
    return service == null ? NametagVisual.EMPTY : service.visualFor(playerUuid);
  }
}
