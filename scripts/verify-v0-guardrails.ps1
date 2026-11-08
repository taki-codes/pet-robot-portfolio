$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$apkPath = Join-Path $repositoryRoot "app/build/outputs/apk/debug/app-debug.apk"
$sensingSourceRoot = Join-Path $repositoryRoot "sensing/src/main"

if (-not (Test-Path $apkPath)) {
    throw "Debug APK not found. Run assembleDebug before this guardrail check."
}

$sdkRoot = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdkRoot) -or -not (Test-Path $sdkRoot)) {
    $localPropertiesPath = Join-Path $repositoryRoot "local.properties"
    if (-not (Test-Path $localPropertiesPath)) {
        throw "ANDROID_HOME is not set and local.properties was not found."
    }

    $sdkEntry = Get-Content $localPropertiesPath |
        Where-Object { $_ -match "^sdk\.dir=" } |
        Select-Object -First 1

    if ([string]::IsNullOrWhiteSpace($sdkEntry)) {
        throw "sdk.dir was not found in local.properties."
    }

    $sdkRoot = $sdkEntry.Substring("sdk.dir=".Length)
    $sdkRoot = $sdkRoot -replace "\\\\", "\"
    $sdkRoot = $sdkRoot -replace "\\:", ":"
}

$aapt2Name = if ($IsWindows -or $env:OS -eq "Windows_NT") { "aapt2.exe" } else { "aapt2" }
$buildToolsRoot = Join-Path $sdkRoot "build-tools"
$aapt2Path = Get-ChildItem $buildToolsRoot -Directory |
    ForEach-Object { Join-Path $_.FullName $aapt2Name } |
    Where-Object { Test-Path $_ } |
    Sort-Object |
    Select-Object -Last 1

if ([string]::IsNullOrWhiteSpace($aapt2Path)) {
    throw "aapt2 was not found under $buildToolsRoot."
}

$permissionOutput = & $aapt2Path dump permissions $apkPath 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "aapt2 could not inspect the debug APK: $permissionOutput"
}

if ($permissionOutput -match "android\.permission\.INTERNET") {
    throw "v0 guardrail violation: the merged debug APK requests android.permission.INTERNET."
}

$forbiddenPersistencePatterns = @(
    "java\.io\.(File|FileOutputStream)",
    "android\.provider\.MediaStore",
    "openOutputStream\s*\("
)

$sourceFiles = Get-ChildItem $sensingSourceRoot -Recurse -File |
    Where-Object { $_.Extension -in ".kt", ".java" }

foreach ($pattern in $forbiddenPersistencePatterns) {
    $matches = $sourceFiles | Select-String -Pattern $pattern
    if ($matches) {
        $locations = $matches |
            ForEach-Object { "$($_.Path):$($_.LineNumber)" } |
            Select-Object -First 5
        throw "v0 guardrail violation: sensing main source contains forbidden persistence API pattern '$pattern' at $($locations -join ', ')."
    }
}

Write-Host "v0 guardrails passed: no INTERNET permission and no forbidden persistence API in sensing main source."
