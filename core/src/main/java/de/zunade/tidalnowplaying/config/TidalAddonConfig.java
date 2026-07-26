package de.zunade.tidalnowplaying.config;

import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;

@ConfigName("settings")
public final class TidalAddonConfig extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @SettingSection("display")
  @SwitchSetting
  private final ConfigProperty<Boolean> showMusicStatus = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showOwnStatus = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showWhenPaused = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showPauseSymbol = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showMusicIcon = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showArtist = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> showTitle = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> modernNametag = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> animateNametag = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> displayTrackCover = new ConfigProperty<>(true);

  @SliderSetting(min = 100, max = 500, steps = 25)
  private final ConfigProperty<Integer> animationSpeedMillis = new ConfigProperty<>(200);

  @SliderSetting(min = 60, max = 140, steps = 5)
  private final ConfigProperty<Integer> nametagScalePercent = new ConfigProperty<>(100);

  @TextFieldSetting(maxLength = 160)
  private final ConfigProperty<String> displayFormat =
      new ConfigProperty<>("{icon} {artist} {separator} {title}");

  @SliderSetting(min = 24, max = 160, steps = 1)
  private final ConfigProperty<Integer> maxTextLength = new ConfigProperty<>(72);

  @SettingSection("detection")
  @SliderSetting(min = 1000, max = 5000, steps = 250)
  private final ConfigProperty<Integer> updateIntervalMillis = new ConfigProperty<>(1500);

  @SettingSection("privacy")
  @SwitchSetting
  private final ConfigProperty<Boolean> shareWithOthers = new ConfigProperty<>(false);

  @SettingSection("diagnostics")
  @SwitchSetting
  private final ConfigProperty<Boolean> debugMode = new ConfigProperty<>(false);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Boolean> showMusicStatus() {
    return this.showMusicStatus;
  }

  public ConfigProperty<Boolean> showOwnStatus() {
    return this.showOwnStatus;
  }

  public ConfigProperty<Boolean> showWhenPaused() {
    return this.showWhenPaused;
  }

  public ConfigProperty<Boolean> showPauseSymbol() {
    return this.showPauseSymbol;
  }

  public ConfigProperty<Boolean> showMusicIcon() {
    return this.showMusicIcon;
  }

  public ConfigProperty<Boolean> showArtist() {
    return this.showArtist;
  }

  public ConfigProperty<Boolean> showTitle() {
    return this.showTitle;
  }

  public ConfigProperty<Boolean> modernNametag() {
    return this.modernNametag;
  }

  public ConfigProperty<Boolean> animateNametag() {
    return this.animateNametag;
  }

  public ConfigProperty<Boolean> displayTrackCover() {
    return this.displayTrackCover;
  }

  public ConfigProperty<Integer> animationSpeedMillis() {
    return this.animationSpeedMillis;
  }

  public ConfigProperty<Integer> nametagScalePercent() {
    return this.nametagScalePercent;
  }

  public ConfigProperty<String> displayFormat() {
    return this.displayFormat;
  }

  public ConfigProperty<Integer> maxTextLength() {
    return this.maxTextLength;
  }

  public ConfigProperty<Integer> updateIntervalMillis() {
    return this.updateIntervalMillis;
  }

  public ConfigProperty<Boolean> shareWithOthers() {
    return this.shareWithOthers;
  }

  public ConfigProperty<Boolean> debugMode() {
    return this.debugMode;
  }
}
