# TIDAL Now Playing für LabyMod 4

Ein lokales LabyMod-4-Addon für Windows, das den aktuell in der TIDAL-Desktop-App
abgespielten Titel als animierte Zwei-Zeilen-Anzeige unter dem eigenen Spielernamen anzeigt:

```text
zunaDE
┌──────┬────────────────┐
│Cover │ Numb           │
│  ━━━ │ Linkin Park    │
└──────┴────────────────┘
```

Version 1 überträgt keinerlei Daten. Der lokale Spieler wird trotzdem bereits über seine UUID
verwaltet, sodass eine spätere, ausdrücklich aktivierbare Version 2 ohne Umbau des Renderpfads
ergänzt werden kann.

## Technische Lösung

1. `TidalMediaService` läuft auf einem einzelnen geplanten Daemon-Thread. Es gibt keine Windows-
   oder Prozessabfrage im Render-Thread und keine Abfrage pro Frame.
2. Ein einziger langlebiger PowerShell-Prozess verwendet unter Windows die
   `GlobalSystemMediaTransportControlsSessionManager`-API (GSMTC). Die Bridge sucht gezielt nach
   einer Media-Session, deren `SourceAppUserModelId` TIDAL enthält, und liest Titel, Interpret und
   Wiedergabestatus.
3. Ist GSMTC nicht verfügbar, fragt derselbe Prozess den TIDAL-Hauptfenstertitel als
   Best-Effort-Fallback ab. Dieser Fallback kann Pausieren nicht zuverlässig erkennen und liefert
   deshalb den Zustand `UNKNOWN`.
4. Externe Texte werden normalisiert, Minecraft-Formatcodes, Steuerzeichen, Bidi-Overrides,
   Private-Use- und ungültige Unicode-Zeichen werden entfernt und Längenlimits werden erzwungen.
5. Nur semantische Änderungen werden als neuer `PlayerMusicStatus` in den threadsicheren
   UUID-Cache übernommen. Unveränderte Polls erneuern lediglich den internen Heartbeat.
6. Die offizielle LabyMod-API `ComponentNameTag` mit `PositionType.BELOW_NAME` rendert vorbereitete,
   unveränderliche Entity-Snapshots. Der moderne Stil verwendet eine gemeinsame dunkle Karte,
   links das TIDAL-Cover mit Fortschrittslinie und rechts zwei Textzeilen. Fehlt ein Cover, erscheint
   ein gedrosselt animierter Equalizer. Lange Texte verwenden eine Unicode-sichere Laufschrift.
   Dadurch gelten normale Nametag-Sichtbarkeit und Distanz; das Addon fügt keinen eigenen
   World-Render-Hook hinzu.
7. Das Kartenlayout orientiert sich am offenen
   [LabyMod Spotify Addon](https://github.com/labymod-addons/spotify), insbesondere an dessen
   `SpotifySharedTrack`-Renderer. Die TIDAL-Implementierung verwendet einen eigenen lokalen
   Cover- und Animationspfad.

Die GSMTC-API ist ab Windows 10 Version 1809 verfügbar. Auf anderen Betriebssystemen bleibt die
Anzeige leer und das Addon schreibt nur bei aktiviertem Debug-Modus einen verständlichen Hinweis.

## Projektstruktur

```text
.
├── api/
│   └── src/main/java/de/zunade/tidalnowplaying/api/snapshot/
│       ├── MusicPlayerSnapshot.java
│       └── MusicSnapshotExtraKeys.java
├── core/
│   ├── src/main/java/de/zunade/tidalnowplaying/
│   │   ├── TidalNowPlayingAddon.java
│   │   ├── cache/
│   │   │   └── PlayerMusicStatusCache.java
│   │   ├── config/
│   │   │   └── TidalAddonConfig.java
│   │   ├── listener/
│   │   │   └── ClientStateListener.java
│   │   ├── media/
│   │   │   ├── LocalPlayerTracker.java
│   │   │   ├── MediaProvider.java
│   │   │   ├── MediaTrack.java
│   │   │   ├── PlaybackState.java
│   │   │   ├── TidalMediaService.java
│   │   │   ├── WindowsMediaSessionProvider.java
│   │   │   ├── WindowTitleFallbackProvider.java
│   │   │   └── bridge/
│   │   │       ├── BridgeResponse.java
│   │   │       ├── PowerShellMediaBridge.java
│   │   │       └── TidalWireParser.java
│   │   ├── nametag/
│   │   │   ├── DisplayPreferences.java
│   │   │   ├── MusicNametagRenderer.java
│   │   │   ├── MusicNametagService.java
│   │   │   ├── MusicStatusFormatter.java
│   │   │   └── MusicStatusSnapshotBridge.java
│   │   ├── network/
│   │   │   ├── LocalOnlyNetworkService.java
│   │   │   ├── MusicStatusNetworkService.java
│   │   │   ├── MusicStatusPayloadValidator.java
│   │   │   └── PlayerMusicStatus.java
│   │   ├── snapshot/
│   │   │   ├── DefaultMusicPlayerSnapshot.java
│   │   │   ├── MusicPlayerSnapshotFactory.java
│   │   │   └── MusicPlayerSnapshotProcessor.java
│   │   └── util/
│   │       ├── RateLimitedLogger.java
│   │       ├── TextSanitizer.java
│   │       └── ThreadingUtil.java
│   ├── src/main/resources/assets/tidalnowplaying/
│   │   ├── bridge/tidal-media-bridge.ps1
│   │   └── i18n/{de_de,en_us}.json
│   └── src/test/java/...
├── game-runner/
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

`addon.json` wird entsprechend der offiziellen LabyMod-Vorlage aus `@AddonMain` und dem
`addonInfo`-Block generiert. Die Release-Datei enthält API- und Core-Modul, Service-Metadaten,
Übersetzungen und die Bridge.

## Konfiguration

Die Einstellungsseite enthält:

- Addon aktivieren/deaktivieren
- Musikstatus und eigene Anzeige aktivieren
- pausierte Musik anzeigen/verbergen
- Pausen- und Musiksymbol ein-/ausschalten
- Interpret und Titel ein-/ausschalten
- modernes Zwei-Zeilen-Design oder klassisches Ein-Zeilen-Design
- TIDAL-Cover oder animierte Equalizer-Kachel
- Equalizer- und Laufschrift-Animationen samt Geschwindigkeit
- Nametag-Größe von 60 bis 140 Prozent
- frei konfigurierbares Format für das klassische Design
- maximale Textlänge beziehungsweise Laufschriftbreite
- Poll-Intervall von 1.000 bis 5.000 ms
- für Version 2 reservierte Freigabe, standardmäßig deaktiviert
- rate-limitierten Debug-Modus

Verfügbare Formatplatzhalter:

```text
{icon} {artist} {title} {separator} {status}
```

Standard im klassischen Design: `{icon} {artist} {separator} {title}`.

## Build

Voraussetzungen für die Entwicklung:

- Windows 10/11
- JDK 21
- PowerShell 5.1 oder neuer
- Internetzugriff beim ersten Gradle-Lauf

Im Projektstamm:

```powershell
.\gradlew.bat build
.\gradlew.bat createReleaseJar
```

Tests separat:

```powershell
.\gradlew.bat :core:test
```

Das installierbare Artefakt ist ausschließlich:

```text
build/libs/tidal-now-playing-release.jar
```

Die anderen JARs sind Zwischenprodukte des Multi-Modul-Builds.

## Installation

1. LabyMod 4 und Minecraft vollständig schließen.
2. `build/libs/tidal-now-playing-release.jar` nach
   `%APPDATA%\.minecraft\labymod-neo\addons` kopieren.
3. LabyMod 4 starten.
4. In den LabyMod-Einstellungen **TIDAL Now Playing** öffnen.
5. TIDAL Desktop starten und einen Titel abspielen.
6. Die eigene Figur in der Third-Person-Ansicht prüfen.

In der normalen First-Person-Ansicht rendert Minecraft die eigene Figur und ihr World-Nametag
nicht. Third Person und andere Ansichten, in denen die lokale Spieler-Entity regulär gerendert
wird, verwenden die zusätzliche Zeile.

## Zustände und Fehlerverhalten

Intern werden `PLAYING`, `PAUSED`, `STOPPED`, `NO_TRACK`, `TIDAL_NOT_RUNNING`,
`SESSION_UNAVAILABLE`, `UNSUPPORTED_PLATFORM`, `UNKNOWN` und `ERROR` unterschieden.
Nur spielbare Zustände mit bereinigten Metadaten erzeugen eine Nametag-Zeile.

Die Bridge wird beim Beenden geschlossen und die temporär extrahierte Skriptdatei entfernt.
Auch das Deaktivieren der Erkennung oder das Verlassen einer Welt suspendiert den Hilfsprozess;
beim erneuten Aktivieren wird er bedarfsgesteuert neu gestartet.
Timeouts beenden einen festgefahrenen Hilfsprozess; Neustartversuche erhalten einen Backoff.
Wiederholte Warnungen werden standardmäßig höchstens einmal alle 30 Sekunden protokolliert.
Keine Bridge-Exception wird bis zum Render- oder Minecraft-Thread weitergereicht.

## Datenschutz

Version 1 öffnet keinen Port und sendet keine Pakete. Die Bridge liest ausschließlich:

- öffentliche Windows-Media-Session-Metadaten,
- den öffentlichen Wiedergabestatus,
- das öffentliche Media-Session-Cover und den Wiedergabefortschritt,
- als Fallback den Titel des TIDAL-Hauptfensters.

Passwörter, E-Mail-Adressen, Cookies, Tokens, Playlists, Hörverlauf, beliebige Benutzerdateien und
Geräteinformationen werden weder gelesen noch gespeichert. Das Cover wird höchstens 5 MB groß in
einer Bridge-eigenen temporären Datei gehalten, ausschließlich lokal als Textur geladen und beim
Beenden entfernt. Dieser interne temporäre Pfad und der Fortschritt sind kein Bestandteil von
`PlayerMusicStatus` und werden nicht freigegeben.

## Vorbereitung für Version 2

`MusicStatusNetworkService` trennt den späteren Transport von Erkennung, Cache und Rendering.
`PlayerMusicStatus` enthält ausschließlich UUID, Interpret, Titel, Wiedergabestatus und Zeitstempel.
`MusicStatusPayloadValidator` ist bereits für eingehende Daten vorbereitet und erzwingt:

- maximal 1.024 Byte pro Nutzlast,
- maximal 128 Codepoints für Interpreten und 256 für Titel,
- Textbereinigung,
- plausible Zeitstempel,
- mindestens 750 ms zwischen Updates eines Spielers,
- ausschließlich freigegebene Wiedergabezustände.

Eine Version 2 sollte einen authentifizierten LabyMod-Addon-Kanal oder einen kleinen Relay-Dienst
implementieren. Der Transport validiert eingehende Pakete, schreibt sie über die Spieler-UUID in
`PlayerMusicStatusCache` und ruft bei `PlayerInfoRemoveEvent` `playerLeft(UUID)` auf. Der bestehende
20-Sekunden-TTL entfernt zusätzlich veraltete Einträge. Das Rendering bleibt unverändert und liest
für jede Entity nur deren eigenen UUID-Eintrag. Die Freigabe bleibt Opt-in.

## Verifizierte API-Grundlage

Das Projekt basiert auf der offiziellen LabyMod-Addon-Vorlage mit LabyGradle 0.8.1, Java 21 und
kompiliert aktuell gegen LabyMod API 4.6.10. Verwendete Referenzen:

- [Offizielle Addon-Vorlage](https://github.com/LabyMod/addon-template)
- [LabyMod Entity Tags](https://dev.labymod.net/pages/addon/rendering/entity-tags/)
- [LabyMod Entity Snapshots](https://dev.labymod.net/pages/addon/rendering/entity-snapshots/)
- [LabyMod-Konfiguration](https://dev.labymod.net/pages/addon/features/config/)
- [Release-Build und Installation](https://dev.labymod.net/pages/addon/publishing/testing/)
- [Microsoft GSMTC Session Manager](https://learn.microsoft.com/en-us/uwp/api/windows.media.control.globalsystemmediatransportcontrolssessionmanager)
