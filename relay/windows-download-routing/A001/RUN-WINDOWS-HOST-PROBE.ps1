# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$InputZip,
    [Parameter(Mandatory = $true)][string]$ExpectedSha256,
    [Parameter(Mandatory = $false)][string]$OutputDirectory = "$HOME\Downloads"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Get-LowerSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Text
    )
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Text, $utf8)
}

$started = [DateTimeOffset]::UtcNow
$expected = $ExpectedSha256.ToLowerInvariant()
$observedInput = Get-LowerSha256 -Path $InputZip
if ($observedInput -ne $expected) {
    throw "INPUT_ZIP_SHA256_MISMATCH expected=$expected observed=$observedInput"
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$manifestPath = Join-Path $root "package-manifest.json"
$probePath = Join-Path $root "probe\vexlife-windows-host-probe.ps1"
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "PACKAGE_MANIFEST_MISSING"
}
if (-not (Test-Path -LiteralPath $probePath -PathType Leaf)) {
    throw "PROBE_SOURCE_MISSING"
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$expectedProbe = [string]$manifest.probe.sha256
$observedProbe = Get-LowerSha256 -Path $probePath
if ($observedProbe -ne $expectedProbe.ToLowerInvariant()) {
    throw "PROBE_SHA256_MISMATCH expected=$expectedProbe observed=$observedProbe"
}

$returnRoot = Join-Path $env:TEMP ("vexlife-windows-host-probe-return-" + $expected.Substring(0, 12))
if (Test-Path -LiteralPath $returnRoot) {
    Remove-Item -LiteralPath $returnRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $returnRoot | Out-Null
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$disposition = "SAFE_FAILURE"
$errorMessage = $null
$probeJson = $null
$probeObject = $null

try {
    $probeJson = (& powershell.exe -NoProfile -NonInteractive -File $probePath | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($probeJson)) {
        throw "PROBE_RETURNED_NO_JSON"
    }
    $probeObject = $probeJson | ConvertFrom-Json
    if ([string]$probeObject.mutationPerformed -ne "False") {
        throw "PROBE_REPORTED_MUTATION"
    }
    $disposition = "PASS"
} catch {
    $errorMessage = $_.Exception.Message
}

$finished = [DateTimeOffset]::UtcNow

$result = [ordered]@{
    schemaVersion = "vexlife-prod.windows-host-probe-return/v1"
    continuity = "[VXG RealForever]"
    disposition = $disposition
    sourceRepository = "VexGPT/VexLife-Prod"
    sourcePullRequest = 215
    sourceCandidateHead = "1430e2749646a7be4daf873ed758b0fe1180506c"
    inputZipSha256 = $observedInput
    probeSha256 = $observedProbe
    startedAtUtc = $started.ToString("o")
    finishedAtUtc = $finished.ToString("o")
    mutationPerformed = $false
    firstFailure = $errorMessage
    hostObservation = $probeObject
}

$resultJson = $result | ConvertTo-Json -Depth 8
Write-Utf8NoBom -Path (Join-Path $returnRoot "result.json") -Text ($resultJson + "`n")

$summary =
    if ($disposition -eq "PASS") {
        "PASS`nWindows target: $($probeObject.targetPlatform) / $($probeObject.targetArchitecture)`nRoute: $($probeObject.routeRef)`nNo mutation performed.`n"
    } else {
        "SAFE_FAILURE`n$errorMessage`nNo mutation performed.`n"
    }
Write-Utf8NoBom -Path (Join-Path $returnRoot "RESULT-SUMMARY.txt") -Text $summary

$log = @(
    "startedAtUtc=$($started.ToString("o"))"
    "inputZipSha256=$observedInput"
    "probeSha256=$observedProbe"
    "disposition=$disposition"
    "targetArchitecture=$($probeObject.targetArchitecture)"
    "routeRef=$($probeObject.routeRef)"
    "finishedAtUtc=$($finished.ToString("o"))"
) -join "`n"
Write-Utf8NoBom -Path (Join-Path $returnRoot "execution.log") -Text ($log + "`n")

Copy-Item -LiteralPath $manifestPath -Destination (Join-Path $returnRoot "input-package.json")

$inventory = @()
Get-ChildItem -LiteralPath $returnRoot -File | Sort-Object Name | ForEach-Object {
    $inventory += [ordered]@{
        path = $_.Name
        bytes = $_.Length
        sha256 = (Get-LowerSha256 -Path $_.FullName)
    }
}
Write-Utf8NoBom -Path (Join-Path $returnRoot "inventory.json") -Text (($inventory | ConvertTo-Json -Depth 5) + "`n")

$tempZip = Join-Path $OutputDirectory "RETURN-VEXLIFE-WINDOWS-HOST-PROBE-A001.forming.zip"
if (Test-Path -LiteralPath $tempZip) {
    Remove-Item -LiteralPath $tempZip -Force
}
Compress-Archive -Path (Join-Path $returnRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha = Get-LowerSha256 -Path $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-HOST-PROBE-A001--sha256-" + $returnSha + ".zip")
if (Test-Path -LiteralPath $finalZip) {
    Remove-Item -LiteralPath $finalZip -Force
}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host ""
Write-Host "VEXLIFE_WINDOWS_HOST_PROBE_RETURN"
Write-Host ("path=" + $finalZip)
Write-Host ("sha256=" + $returnSha)
Write-Host ("disposition=" + $disposition)
if ($disposition -eq "PASS") {
    Write-Host ("target=" + $probeObject.targetPlatform + "/" + $probeObject.targetArchitecture)
    Write-Host ("routeRef=" + $probeObject.routeRef)
}
try {
    Start-Process explorer.exe -ArgumentList "/select,`"$finalZip`"" | Out-Null
} catch {
}

if ($disposition -ne "PASS") {
    exit 2
}
