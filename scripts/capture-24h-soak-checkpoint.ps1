param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("precheck", "start", "12h", "24h")]
    [string]$Phase,

    [string]$AdbPath = "C:\Android\Sdk\platform-tools\adb.exe",

    [string]$OutputDirectory = (
        Join-Path ([Environment]::GetFolderPath("MyDocuments")) "PetRobotEvidence"
    )
)

$ErrorActionPreference = "Stop"
$packageName = "com.taki.petrobot"

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb.exe was not found: $AdbPath"
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputPath = Join-Path $OutputDirectory "petrobot-24h-$Phase-$timestamp.txt"
$report = [System.Collections.Generic.List[string]]::new()

function Add-Section {
    param(
        [string]$Title,
        [string[]]$Lines
    )

    [void]$report.Add("===== $Title =====")
    foreach ($line in $Lines) {
        [void]$report.Add($line)
    }
    [void]$report.Add("")
}

function Invoke-AdbLines {
    param([string[]]$AdbArguments)

    return @(
        & $AdbPath @AdbArguments 2>&1 |
            ForEach-Object { $_.ToString() }
    )
}

$deviceLines = Invoke-AdbLines -AdbArguments @("devices")
if (-not ($deviceLines -match "\sdevice$")) {
    throw "No authorized Android device was found."
}

Add-Section -Title "Checkpoint" -Lines @(
    "phase=$Phase",
    "hostTime=$((Get-Date).ToString("o"))",
    "package=$packageName"
)
Add-Section -Title "ADB devices" -Lines $deviceLines
Add-Section -Title "Device time and uptime" -Lines (
    Invoke-AdbLines -AdbArguments @("shell", "sh", "-c", "date; uptime")
)
Add-Section -Title "Process id" -Lines (
    Invoke-AdbLines -AdbArguments @("shell", "pidof", $packageName)
)
Add-Section -Title "Battery" -Lines (
    Invoke-AdbLines -AdbArguments @("shell", "dumpsys", "battery")
)
Add-Section -Title "Thermal service" -Lines (
    Invoke-AdbLines -AdbArguments @("shell", "dumpsys", "thermalservice")
)
Add-Section -Title "Memory" -Lines (
    Invoke-AdbLines -AdbArguments @("shell", "dumpsys", "meminfo", $packageName)
)
Add-Section -Title "Process exit history" -Lines (
    Invoke-AdbLines -AdbArguments @(
        "shell",
        "dumpsys",
        "activity",
        "exit-info",
        $packageName
    )
)
Add-Section -Title "Database files" -Lines (
    Invoke-AdbLines -AdbArguments @(
        "shell",
        "run-as",
        $packageName,
        "ls",
        "-l",
        "databases"
    )
)
Add-Section -Title "Recent PetRobot diagnostics" -Lines (
    Invoke-AdbLines -AdbArguments @(
        "logcat",
        "-d",
        "-t",
        "1000",
        "-s",
        "PetRobotCamera:I",
        "PetRobotAudio:I",
        "PetRobotThermal:I",
        "PetRobotStorage:I",
        "PetRobotSummary:I",
        "*:S"
    )
)

$report | Set-Content -LiteralPath $outputPath -Encoding utf8
Write-Host "Checkpoint saved: $outputPath"
