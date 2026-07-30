[CmdletBinding()]
param(
    [string]$DeviceId,
    [string]$AdbPath,
    [string]$ApkArchivePath = (Join-Path $env:USERPROFILE 'OneDrive\Apps')
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

$devices = @(& $AdbPath devices | Select-String '\sdevice$' | ForEach-Object {
    ($_ -split '\s+')[0]
})
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

    $apkOutputPath = Join-Path $projectRoot 'app\build\outputs\apk\debug'
    $outputMetadataPath = Join-Path $apkOutputPath 'output-metadata.json'
    if (-not (Test-Path $outputMetadataPath)) {
        throw "Android build metadata was not found at '$outputMetadataPath'."
    }

    $outputMetadata = Get-Content $outputMetadataPath -Raw | ConvertFrom-Json
    $apkElement = $outputMetadata.elements | Select-Object -First 1
    $builtApkPath = Join-Path $apkOutputPath $apkElement.outputFile
    if (-not $apkElement -or -not (Test-Path $builtApkPath)) {
        throw "The generated debug APK could not be resolved from '$outputMetadataPath'."
    }

    New-Item -ItemType Directory -Path $ApkArchivePath -Force | Out-Null
    $versionedApkPath = Join-Path $ApkArchivePath "StockStreamLocal-$($apkElement.versionName)-debug.apk"
    $latestApkPath = Join-Path $ApkArchivePath 'StockStreamLocal-debug.apk'
    Copy-Item $builtApkPath $versionedApkPath -Force
    Copy-Item $builtApkPath $latestApkPath -Force

    & $AdbPath -s $DeviceId shell am start -n 'com.wolffentp.android_stock_tracker.debug/com.wolffentp.stockstreamlocal.MainActivity'
    if ($LASTEXITCODE -ne 0) {
        throw "Android activity launch failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Write-Host "StockStream Local was installed and launched on $DeviceId."
Write-Host "APK archived to '$versionedApkPath' and '$latestApkPath'."