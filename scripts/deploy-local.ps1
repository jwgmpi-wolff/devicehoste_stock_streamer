[CmdletBinding()]
param(
    [string]$DeviceId,
    [string]$AdbPath
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot 'gradlew.bat'

if (-not $AdbPath) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCommand) {
        $AdbPath = $adbCommand.Source
    } elseif ($env:ANDROID_HOME) {
        $AdbPath = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    }
}

if (-not $AdbPath -or -not (Test-Path $AdbPath)) {
    throw 'adb was not found. Install Android SDK Platform-Tools, add adb to PATH, or pass -AdbPath.'
}

$devices = & $AdbPath devices | Select-String '\sdevice$' | ForEach-Object {
    ($_ -split '\s+')[0]
}
if (-not $devices) {
    throw 'No authorized Android device or emulator is connected.'
}
if (-not $DeviceId) {
    if ($devices.Count -gt 1) {
        throw 'Multiple devices are connected. Pass -DeviceId with the desired adb serial.'
    }
    $DeviceId = $devices[0]
}
if ($DeviceId -notin $devices) {
    throw "Device '$DeviceId' is not connected or authorized."
}

Push-Location $projectRoot
try {
    & $gradleWrapper :app:installDebug --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle installDebug failed with exit code $LASTEXITCODE."
    }

    & $AdbPath -s $DeviceId shell am start -n 'com.wolffentp.android_stock_tracker.debug/com.wolffentp.stockstreamlocal.MainActivity'
    if ($LASTEXITCODE -ne 0) {
        throw "Android activity launch failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Write-Host "StockStream Local was installed and launched on $DeviceId."