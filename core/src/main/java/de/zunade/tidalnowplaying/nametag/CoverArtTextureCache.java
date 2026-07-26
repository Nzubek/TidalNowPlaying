package de.zunade.tidalnowplaying.nametag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.resources.texture.TextureDetails;

/**
 * Registers only bridge-owned, size-bounded files as dynamic textures.
 */
final class CoverArtTextureCache implements AutoCloseable {

  private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
  private static final String FILE_PREFIX = "tidal-now-playing-cover-";

  private String attemptedToken = "";
  private ResourceLocation resource;
  private Icon icon;

  Icon resolve(String artworkPath, String artworkKey) {
    if (artworkPath == null || artworkPath.isBlank()
        || artworkKey == null || artworkKey.isBlank()) {
      clear();
      return null;
    }

    String token = artworkPath + '\0' + artworkKey;
    if (token.equals(this.attemptedToken)) {
      return this.icon;
    }
    this.attemptedToken = token;
    releaseTexture();

    try {
      Path image = trustedImagePath(artworkPath);
      long size = Files.size(image);
      if (size < 32 || size > MAX_IMAGE_BYTES) {
        return null;
      }

      byte[] imageData = Files.readAllBytes(image);
      String textureKey = artworkKey
          .toLowerCase(Locale.ROOT)
          .replaceAll("[^a-z0-9_-]", "");
      if (textureKey.isBlank()) {
        textureKey = Integer.toUnsignedString(token.hashCode(), 16);
      }
      if (textureKey.length() > 64) {
        textureKey = textureKey.substring(0, 64);
      }
      textureKey = textureKey + "-" + Integer.toUnsignedString(artworkPath.hashCode(), 16);

      this.resource = ResourceLocation.create(
          "tidalnowplaying",
          "dynamic/cover/" + textureKey
      );
      TextureDetails details = TextureDetails.builder(this.resource)
          .withImageData(imageData)
          .build();
      Laby.references().textureRepository().getOrRegisterTexture(details);
      this.icon = Icon.texture(this.resource);
      return this.icon;
    } catch (IOException | InvalidPathException | SecurityException ignored) {
      this.resource = null;
      this.icon = null;
      return null;
    }
  }

  private static Path trustedImagePath(String artworkPath) throws IOException {
    Path image = Path.of(artworkPath).toAbsolutePath().normalize();
    Path tempDirectory = Path.of(
        Objects.requireNonNullElse(System.getProperty("java.io.tmpdir"), "")
    ).toAbsolutePath().normalize();
    Path fileName = image.getFileName();
    if (!image.startsWith(tempDirectory)
        || fileName == null
        || !fileName.toString().startsWith(FILE_PREFIX)
        || !Files.isRegularFile(image)) {
      throw new IOException("Untrusted cover-art path");
    }
    return image;
  }

  private void clear() {
    if (this.attemptedToken.isEmpty()) {
      return;
    }
    this.attemptedToken = "";
    releaseTexture();
  }

  private void releaseTexture() {
    if (this.resource != null) {
      Laby.references().textureRepository().queueTextureRelease(this.resource);
    }
    this.resource = null;
    this.icon = null;
  }

  @Override
  public void close() {
    this.attemptedToken = "";
    releaseTexture();
  }
}
