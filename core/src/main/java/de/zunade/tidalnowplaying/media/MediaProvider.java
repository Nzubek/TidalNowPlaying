package de.zunade.tidalnowplaying.media;

public interface MediaProvider extends AutoCloseable {

  String name();

  MediaTrack query();

  default void suspend() {
  }

  @Override
  default void close() {
  }
}
