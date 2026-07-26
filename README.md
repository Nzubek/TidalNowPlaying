# TIDAL Now Playing für LabyMod 4

Dieses Addon zeigt den aktuell laufenden TIDAL-Song unter deinem
Minecraft-Namen an – mit Cover, Titel, Interpret und Fortschrittsleiste.

```text
┌────────┬────────────────────┐
│ Cover  │ Numb               │
│  ━━━━  │ Linkin Park        │
└────────┴────────────────────┘
```

Wenn Windows kein Cover bereitstellt, erscheint stattdessen ein animierter Equalizer.

## Voraussetzungen

- Windows 10 oder 11
- LabyMod 4
- TIDAL Desktop

Das Addon funktioniert mit den von LabyMod 4 unterstützten Minecraft-Versionen.

## Installation

1. Lade die JAR unter
   [Releases](https://github.com/Nzubek/TidalNowPlaying/releases/latest) herunter.
2. Schließe Minecraft und LabyMod vollständig.
3. Kopiere die JAR nach:

   ```text
   %APPDATA%\.minecraft\labymod-neo\addons
   ```

4. Starte TIDAL Desktop und spiele einen Song ab.
5. Starte anschließend Minecraft mit LabyMod 4.

## Benutzung

Öffne in LabyMod die Einstellungen von **TIDAL Now Playing**. Dort kannst du unter
anderem Folgendes ändern:

- Cover oder Equalizer anzeigen
- Animationen ein- oder ausschalten
- Größe des Nametags ändern
- Titel und Interpret ein- oder ausblenden
- Anzeige bei pausierter Musik ändern

Das eigene Nametag siehst du normalerweise nur in der Third-Person-Ansicht
(`F5`). Die aktuelle Version zeigt nur deinen eigenen TIDAL-Status an.

## Wenn nichts angezeigt wird

Prüfe diese Punkte:

1. Läuft der Song in **TIDAL Desktop** und nicht nur im Browser?
2. Ist das Addon in den LabyMod-Einstellungen aktiviert?
3. Testest du dein eigenes Nametag mit `F5`?
4. Starte TIDAL und LabyMod einmal neu.
5. Aktiviere bei Bedarf den Debug-Modus in den Addon-Einstellungen.

Ohne verfügbare Windows-Media-Session nutzt das Addon den Fenstertitel als
Fallback. Dabei können Cover, Fortschritt und der Pausenstatus fehlen.

## Datenschutz

Die aktuelle Version arbeitet lokal auf deinem Computer:

- keine Anmeldung bei TIDAL über das Addon
- keine Passwörter, Cookies oder Tokens
- keine Übertragung des gehörten Songs
- temporäre Cover-Dateien werden beim Beenden entfernt

## Für Entwickler

Benötigt werden JDK 21 und PowerShell 5.1 oder neuer.

```powershell
.\gradlew.bat :core:test
.\gradlew.bat createReleaseJar
```

Die fertige Datei liegt unter:

```text
build/libs/tidal-now-playing-release.jar
```

## Credits

Das kompakte Nametag-Layout wurde vom offenen
[LabyMod Spotify Addon](https://github.com/labymod-addons/spotify) inspiriert.
Weitere Angaben stehen in den
[Hinweisen zu Drittanbietern](core/src/main/resources/META-INF/THIRD-PARTY-NOTICES.txt).
