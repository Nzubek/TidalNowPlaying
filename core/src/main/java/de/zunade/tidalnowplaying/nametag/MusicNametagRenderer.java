package de.zunade.tidalnowplaying.nametag;

import de.zunade.tidalnowplaying.api.snapshot.MusicPlayerSnapshot;
import de.zunade.tidalnowplaying.api.snapshot.MusicSnapshotExtraKeys;
import de.zunade.tidalnowplaying.config.TidalAddonConfig;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.gfx.pipeline.renderer.text.FontFlags;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import net.labymod.api.laby3d.pipeline.RenderStates;
import net.labymod.api.laby3d.pipeline.material.LevelMaterial;
import net.labymod.api.laby3d.pipeline.material.Material;
import net.labymod.api.laby3d.render.queue.CustomGeometryRenderer;
import net.labymod.api.laby3d.render.queue.SubmissionCollector;
import net.labymod.api.laby3d.render.queue.submissions.IconSubmission.DisplayMode;
import net.labymod.api.loader.MinecraftVersions;
import net.labymod.laby3d.api.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Card layout inspired by LabyMod's open-source Spotify addon.
 *
 * <p>The implementation is purpose-built for the local TIDAL bridge: it uses a dynamic cover when
 * Windows provides one and otherwise renders an animated equalizer tile.</p>
 */
public final class MusicNametagRenderer extends ComponentNameTag implements AutoCloseable {

  private static final boolean INVERSE_DEPTH = MinecraftVersions.V1_20_6.orOlder();
  private static final float BACKGROUND_DEPTH = -0.03F;
  private static final float TILE_DEPTH = 0.03F;
  private static final float FOREGROUND_DEPTH = 0.04F;
  private static final int TILE_BACKGROUND_COLOR = 0xFF102A33;
  private static final int PLAYING_ACCENT_COLOR = 0xFF21D4E2;
  private static final int PAUSED_ACCENT_COLOR = 0xFFFFAA00;
  private static final int PROGRESS_BACKGROUND_COLOR = 0xFF26363B;
  private static final Material CARD_MATERIAL =
      LevelMaterial.builder(RenderStates.GUI).build();
  private static final float[][] BAR_FRAMES = {
      {0.35F, 0.75F, 0.55F},
      {0.70F, 0.40F, 0.85F},
      {0.90F, 0.60F, 0.30F},
      {0.50F, 0.90F, 0.65F}
  };

  private final TidalAddonConfig config;
  private final CoverArtTextureCache coverArtCache = new CoverArtTextureCache();
  private boolean cardLayout;
  private boolean tileVisible;
  private boolean tileAnimated;
  private boolean paused;
  private int animationSpeedMillis = 200;
  private double progress;
  private Icon coverIcon;

  public MusicNametagRenderer(TidalAddonConfig config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    this.cardLayout = false;
    this.tileVisible = false;
    this.tileAnimated = false;
    this.paused = false;
    this.progress = 0.0;
    this.coverIcon = null;
    if (!snapshot.has(MusicSnapshotExtraKeys.MUSIC_PLAYER)) {
      return Collections.emptyList();
    }

    MusicPlayerSnapshot musicSnapshot = snapshot.get(MusicSnapshotExtraKeys.MUSIC_PLAYER);
    this.cardLayout = musicSnapshot.modernDesign();
    this.tileVisible = this.cardLayout && musicSnapshot.showVisualTile();
    this.tileAnimated = musicSnapshot.animateVisualTile();
    this.animationSpeedMillis = musicSnapshot.animationSpeedMillis();
    this.paused = musicSnapshot.paused();
    this.progress = musicSnapshot.progress();
    this.coverIcon = this.tileVisible
        ? this.coverArtCache.resolve(
            musicSnapshot.artworkPath(),
            musicSnapshot.artworkKey()
        )
        : null;
    return musicSnapshot.components();
  }

  @Override
  public void render(
      Stack stack,
      SubmissionCollector submissionCollector,
      EntitySnapshot snapshot
  ) {
    if (!this.cardLayout) {
      super.render(stack, submissionCollector, snapshot);
      return;
    }

    float cardHeight = this.getHeight();
    float cardWidth = this.getWidth();
    int backgroundColor = Laby.labyAPI()
        .minecraft()
        .options()
        .getBackgroundColorWithOpacity(DEFAULT_BACKGROUND_COLOR);
    submitRectangle(
        stack,
        submissionCollector,
        -1.0F,
        -1.0F,
        cardWidth + 1.0F,
        cardHeight + 1.0F,
        BACKGROUND_DEPTH,
        backgroundColor
    );

    if (this.tileVisible) {
      if (this.coverIcon != null) {
        submissionCollector.submitIcon(
            stack,
            this.coverIcon,
            DisplayMode.NORMAL,
            0.0F,
            0.0F,
            cardHeight,
            cardHeight,
            -1
        );
      } else {
        renderEqualizerTile(stack, submissionCollector, cardHeight);
      }
      renderProgress(stack, submissionCollector, cardHeight);
    }

    super.render(stack, submissionCollector, snapshot);
  }

  private void renderEqualizerTile(
      Stack stack,
      SubmissionCollector submissionCollector,
      float size
  ) {
    submitRectangle(
        stack,
        submissionCollector,
        0.0F,
        0.0F,
        size,
        size,
        TILE_DEPTH,
        TILE_BACKGROUND_COLOR
    );

    int accentColor = this.paused ? PAUSED_ACCENT_COLOR : PLAYING_ACCENT_COLOR;
    if (this.paused) {
      float barWidth = Math.max(1.0F, size * 0.16F);
      float gap = size * 0.13F;
      float left = (size - barWidth * 2.0F - gap) / 2.0F;
      submitRectangle(
          stack, submissionCollector,
          left, size * 0.25F,
          left + barWidth, size * 0.75F,
          FOREGROUND_DEPTH, accentColor
      );
      submitRectangle(
          stack, submissionCollector,
          left + barWidth + gap, size * 0.25F,
          left + barWidth * 2.0F + gap, size * 0.75F,
          FOREGROUND_DEPTH, accentColor
      );
      return;
    }

    long frame = this.tileAnimated
        ? System.currentTimeMillis() / this.animationSpeedMillis
        : 0L;
    float[] heights = BAR_FRAMES[Math.floorMod(frame, BAR_FRAMES.length)];
    float gap = Math.max(1.0F, size * 0.08F);
    float horizontalPadding = size * 0.18F;
    float barWidth = (size - horizontalPadding * 2.0F - gap * 2.0F) / 3.0F;
    float bottom = size * 0.82F;
    float usableHeight = size * 0.62F;
    for (int index = 0; index < heights.length; index++) {
      float left = horizontalPadding + index * (barWidth + gap);
      float top = bottom - usableHeight * heights[index];
      submitRectangle(
          stack, submissionCollector,
          left, top,
          left + barWidth, bottom,
          FOREGROUND_DEPTH, accentColor
      );
    }
  }

  private void renderProgress(
      Stack stack,
      SubmissionCollector submissionCollector,
      float size
  ) {
    if (this.progress <= 0.0) {
      return;
    }
    float top = size - 1.0F;
    submitRectangle(
        stack, submissionCollector,
        0.0F, top,
        size, size,
        TILE_DEPTH, PROGRESS_BACKGROUND_COLOR
    );
    submitRectangle(
        stack, submissionCollector,
        0.0F, top,
        (float) (size * this.progress), size,
        FOREGROUND_DEPTH, PLAYING_ACCENT_COLOR
    );
  }

  private static void submitRectangle(
      Stack stack,
      SubmissionCollector submissionCollector,
      float left,
      float top,
      float right,
      float bottom,
      float depth,
      int color
  ) {
    submissionCollector.submitCustomGeometry(
        stack,
        CARD_MATERIAL,
        new CardRectangle(
            left,
            top,
            right,
            bottom,
            INVERSE_DEPTH ? -depth : depth,
            color
        )
    );
  }

  @Override
  protected void submitText(
      Stack stack,
      SubmissionCollector submissionCollector,
      EntitySnapshot snapshot,
      Component component,
      float xOffset,
      float yOffset
  ) {
    if (!this.cardLayout) {
      super.submitText(
          stack,
          submissionCollector,
          snapshot,
          component,
          xOffset,
          yOffset
      );
      return;
    }

    float left = this.tileVisible ? this.getHeight() + 2.0F : 1.0F;
    submissionCollector.order(3).submitComponent(
        stack,
        component,
        left,
        yOffset,
        DEFAULT_TEXT_COLOR,
        snapshot.lightCoords(),
        0,
        FontFlags.DISPLAY_MODE_NORMAL
    );
  }

  @Override
  protected int getBackgroundColor(EntitySnapshot snapshot) {
    return this.cardLayout ? 0 : super.getBackgroundColor(snapshot);
  }

  @Override
  public float getScale() {
    int scalePercent = Math.clamp(this.config.nametagScalePercent().get(), 60, 140);
    float baseScale = this.config.modernNametag().get() ? 0.5F : 1.0F;
    return baseScale * scalePercent / 100.0F;
  }

  @Override
  public float getWidth() {
    float width = super.getWidth();
    return this.cardLayout && this.tileVisible
        ? width + this.getHeight() + 2.0F
        : width + (this.cardLayout ? 2.0F : 0.0F);
  }

  @Override
  public void close() {
    this.coverArtCache.close();
  }

  private record CardRectangle(
      float left,
      float top,
      float right,
      float bottom,
      float depth,
      int color
  ) implements CustomGeometryRenderer {

    @Override
    public void render(Matrix4f pose, VertexConsumer consumer) {
      consumer.addVertex(pose, this.left, this.top, this.depth)
          .setBlankUv()
          .setColor(this.color);
      consumer.addVertex(pose, this.left, this.bottom, this.depth)
          .setBlankUv()
          .setColor(this.color);
      consumer.addVertex(pose, this.right, this.bottom, this.depth)
          .setBlankUv()
          .setColor(this.color);
      consumer.addVertex(pose, this.right, this.top, this.depth)
          .setBlankUv()
          .setColor(this.color);
    }
  }
}
