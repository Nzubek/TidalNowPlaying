$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# This bridge exposes public media-session metadata and, when available, its public thumbnail.
# It never reads TIDAL credentials, cookies, tokens, playlists, history, or user files.

function Convert-ToWireField {
    param([AllowNull()][string] $Value)

    if ([string]::IsNullOrEmpty($Value)) {
        return ""
    }

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Value)
    return [System.Convert]::ToBase64String($bytes)
}

function Write-State {
    param(
        [string] $State,
        [AllowNull()][string] $Diagnostic
    )

    $line = "STATE|{0}|{1}" -f @(
        $State,
        (Convert-ToWireField $Diagnostic)
    )
    [Console]::Out.WriteLine($line)
    [Console]::Out.Flush()
}

function Write-Result {
    param(
        [string] $State,
        [AllowNull()][string] $Artist,
        [AllowNull()][string] $Title,
        [AllowNull()][string] $Source,
        [AllowNull()][string] $ArtworkPath,
        [AllowNull()][string] $ArtworkKey,
        [AllowNull()][string] $Progress
    )

    if ([string]::IsNullOrWhiteSpace($Progress)) {
        $Progress = "0"
    }

    $line = "RESULT|{0}|{1}|{2}|{3}|{4}|{5}|{6}" -f @(
        $State,
        (Convert-ToWireField $Artist),
        (Convert-ToWireField $Title),
        (Convert-ToWireField $Source),
        (Convert-ToWireField $ArtworkPath),
        (Convert-ToWireField $ArtworkKey),
        $Progress
    )
    [Console]::Out.WriteLine($line)
    [Console]::Out.Flush()
}

function Test-TidalRunning {
    return $null -ne (Get-Process -Name "TIDAL" -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Get-ConciseExceptionMessage {
    param([Parameter(Mandatory = $true)] $Exception)

    $messages = New-Object System.Collections.Generic.List[string]
    $current = $Exception
    while ($null -ne $current -and $messages.Count -lt 4) {
        if (-not [string]::IsNullOrWhiteSpace($current.Message)) {
            $messages.Add($current.Message)
        }
        $current = $current.InnerException
    }

    $message = $messages -join " -> "
    if ($message.Length -gt 800) {
        return $message.Substring(0, 800)
    }
    return $message
}

$script:AsTaskMethod = $null
$script:SessionManager = $null
$script:WinRtInitializationError = $null
$script:ArtworkIdentity = ""
$script:ArtworkKey = ""
$script:ArtworkPath = Join-Path ([System.IO.Path]::GetTempPath()) (
    "tidal-now-playing-cover-{0}.img" -f $PID
)

function Initialize-WinRt {
    if ($null -ne $script:AsTaskMethod) {
        return
    }

    [void][System.Reflection.Assembly]::LoadWithPartialName("System.Runtime.WindowsRuntime")
    [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
    [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]

    $script:AsTaskMethod = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object {
            $_.Name -eq "AsTask" -and
            $_.IsGenericMethod -and
            $_.GetParameters().Count -eq 1
        } |
        Select-Object -First 1

    if ($null -eq $script:AsTaskMethod) {
        throw "System.WindowsRuntimeSystemExtensions.AsTask was not found"
    }
}

function Wait-WinRtOperation {
    param(
        [Parameter(Mandatory = $true)] $Operation,
        [Parameter(Mandatory = $true)] [Type] $ResultType,
        [int] $TimeoutMillis = 2500
    )

    $method = $script:AsTaskMethod.MakeGenericMethod($ResultType)
    $task = $method.Invoke($null, @($Operation))
    if (-not $task.Wait($TimeoutMillis)) {
        throw "Windows Runtime media request timed out"
    }
    return $task.Result
}

function Get-SessionManager {
    if ($null -eq $script:SessionManager) {
        Initialize-WinRt
        $operation = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()
        $managerType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
        $script:SessionManager = Wait-WinRtOperation $operation $managerType
    }
    return $script:SessionManager
}

function Convert-PlaybackState {
    param([AllowNull()] $PlaybackInfo)

    if ($null -eq $PlaybackInfo) {
        return "NO_TRACK"
    }

    switch ($PlaybackInfo.PlaybackStatus.ToString()) {
        "Playing" { return "PLAYING" }
        "Paused" { return "PAUSED" }
        "Stopped" { return "STOPPED" }
        "Closed" { return "NO_TRACK" }
        default { return "UNKNOWN" }
    }
}

function Get-ArtworkKey {
    param([Parameter(Mandatory = $true)][string] $Identity)

    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Identity)
        $hash = $sha256.ComputeHash($bytes)
        return ([System.BitConverter]::ToString($hash)).Replace("-", "").ToLowerInvariant()
    }
    finally {
        $sha256.Dispose()
    }
}

function Clear-CachedArtwork {
    $script:ArtworkIdentity = ""
    $script:ArtworkKey = ""
    Remove-Item -LiteralPath $script:ArtworkPath -Force -ErrorAction SilentlyContinue
}

function Get-TidalArtwork {
    param(
        [Parameter(Mandatory = $true)] $Properties,
        [Parameter(Mandatory = $true)][string] $Identity
    )

    if ($null -eq $Properties.Thumbnail) {
        Clear-CachedArtwork
        return @{ Path = ""; Key = "" }
    }

    if (
        $script:ArtworkIdentity -eq $Identity -and
        -not [string]::IsNullOrWhiteSpace($script:ArtworkKey) -and
        (Test-Path -LiteralPath $script:ArtworkPath -PathType Leaf)
    ) {
        return @{ Path = $script:ArtworkPath; Key = $script:ArtworkKey }
    }

    $randomAccessStream = $null
    $managedStream = $null
    $output = $null
    try {
        [void][Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
        $streamOperation = $Properties.Thumbnail.OpenReadAsync()
        $streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType]
        $randomAccessStream = Wait-WinRtOperation $streamOperation $streamType 1500
        if ($null -eq $randomAccessStream -or $randomAccessStream.Size -le 0) {
            Clear-CachedArtwork
            return @{ Path = ""; Key = "" }
        }
        if ($randomAccessStream.Size -gt (5 * 1024 * 1024)) {
            Clear-CachedArtwork
            return @{ Path = ""; Key = "" }
        }

        $managedStream = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead(
            $randomAccessStream
        )
        $output = [System.IO.File]::Open(
            $script:ArtworkPath,
            [System.IO.FileMode]::Create,
            [System.IO.FileAccess]::Write,
            [System.IO.FileShare]::None
        )
        $managedStream.CopyTo($output)
        $output.Flush()

        $script:ArtworkIdentity = $Identity
        $script:ArtworkKey = Get-ArtworkKey $Identity
        return @{ Path = $script:ArtworkPath; Key = $script:ArtworkKey }
    }
    catch {
        Clear-CachedArtwork
        return @{ Path = ""; Key = "" }
    }
    finally {
        if ($null -ne $output) {
            $output.Dispose()
        }
        if ($null -ne $managedStream) {
            $managedStream.Dispose()
        }
        if ($null -ne $randomAccessStream) {
            $randomAccessStream.Dispose()
        }
    }
}

function Get-TimelineProgress {
    param([Parameter(Mandatory = $true)] $Session)

    try {
        $timeline = $Session.GetTimelineProperties()
        if ($null -eq $timeline) {
            return "0"
        }
        $duration = ($timeline.EndTime - $timeline.StartTime).TotalMilliseconds
        $position = ($timeline.Position - $timeline.StartTime).TotalMilliseconds
        if ($duration -le 0 -or $position -le 0) {
            return "0"
        }
        $progress = [System.Math]::Max(0.0, [System.Math]::Min(1.0, $position / $duration))
        return $progress.ToString(
            "0.######",
            [System.Globalization.CultureInfo]::InvariantCulture
        )
    }
    catch {
        return "0"
    }
}

function Get-TidalMediaSession {
    try {
        $manager = Get-SessionManager
        $selectedSession = $null
        $selectedState = "NO_TRACK"

        foreach ($session in $manager.GetSessions()) {
            $source = [string]$session.SourceAppUserModelId
            if ($source -notmatch "(?i)tidal") {
                continue
            }

            $state = Convert-PlaybackState $session.GetPlaybackInfo()
            if ($null -eq $selectedSession -or $state -eq "PLAYING") {
                $selectedSession = $session
                $selectedState = $state
            }
            if ($state -eq "PLAYING") {
                break
            }
        }

        if ($null -eq $selectedSession) {
            if (Test-TidalRunning) {
                Write-State "SESSION_UNAVAILABLE" "TIDAL is running without a matching GSMTC session"
            }
            else {
                Write-State "TIDAL_NOT_RUNNING" ""
            }
            return
        }

        $propertiesOperation = $selectedSession.TryGetMediaPropertiesAsync()
        $propertiesType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]
        $properties = Wait-WinRtOperation $propertiesOperation $propertiesType
        $artist = [string]$properties.Artist
        $title = [string]$properties.Title
        $source = [string]$selectedSession.SourceAppUserModelId

        if ([string]::IsNullOrWhiteSpace($artist) -and [string]::IsNullOrWhiteSpace($title)) {
            Write-State "NO_TRACK" "TIDAL GSMTC session has no track metadata"
            return
        }

        $artwork = Get-TidalArtwork $properties ($artist + [char]0 + $title)
        $progress = Get-TimelineProgress $selectedSession
        Write-Result $selectedState $artist $title $source $artwork.Path $artwork.Key $progress
    }
    catch {
        $script:SessionManager = $null
        Write-State "SESSION_UNAVAILABLE" (Get-ConciseExceptionMessage $_.Exception)
    }
}

function Get-TidalWindowTitle {
    try {
        $tidalProcess = Get-Process -Name "TIDAL" -ErrorAction SilentlyContinue |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_.MainWindowTitle) } |
            Select-Object -First 1

        if ($null -eq $tidalProcess) {
            if (Test-TidalRunning) {
                Write-State "NO_TRACK" "TIDAL has no readable main-window title"
            }
            else {
                Write-State "TIDAL_NOT_RUNNING" ""
            }
            return
        }

        $windowTitle = ([string]$tidalProcess.MainWindowTitle).Trim()
        if ($windowTitle -match "^(?i:TIDAL)$") {
            Write-State "NO_TRACK" "TIDAL window title contains no track"
            return
        }

        $body = $windowTitle -replace "\s+[-\u2013\u2014|]\s+(?i:TIDAL)$", ""
        $parts = $body -split "\s+[-\u2013\u2014]\s+", 2
        if ($parts.Count -eq 2) {
            # TIDAL's title-bar format is not a public contract. This is a best-effort heuristic.
            Write-Result "UNKNOWN" $parts[0].Trim() $parts[1].Trim() (
                "TIDAL window title"
            ) "" "" "0"
        }
        else {
            Write-Result "UNKNOWN" "" $body.Trim() "TIDAL window title" "" "" "0"
        }
    }
    catch {
        Write-State "ERROR" (Get-ConciseExceptionMessage $_.Exception)
    }
}

while ($true) {
    $command = [Console]::In.ReadLine()
    if ($null -eq $command -or $command -eq "EXIT") {
        break
    }

    switch ($command) {
        "MEDIA" { Get-TidalMediaSession }
        "WINDOW" { Get-TidalWindowTitle }
        default { Write-State "ERROR" "Unknown bridge command" }
    }
}

Clear-CachedArtwork
