# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$OutputDirectory = "$HOME\Downloads"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$NL = [Environment]::NewLine

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

$checkpoint = Get-ChildItem -LiteralPath $OutputDirectory -Directory |
    Where-Object { $_.Name -like "VEXLIFE-WINDOWS-READINESS-R001-CHECKPOINTS-*" } |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1

if ($null -eq $checkpoint) {
    throw "R001_CHECKPOINT_DIRECTORY_NOT_FOUND"
}

$checkpointFiles = @(Get-ChildItem -LiteralPath $checkpoint.FullName -File | Sort-Object Name)
if ($checkpointFiles.Count -eq 0) {
    throw "R001_CHECKPOINT_DIRECTORY_EMPTY"
}

$inventory = @(
    $checkpointFiles |
        ForEach-Object {
            [ordered]@{
                path = $_.Name
                bytes = $_.Length
                sha256 = (Get-LowerSha256 $_.FullName)
            }
        }
)

$recovery = [ordered]@{
    schemaVersion = "vexlife-prod.windows-readiness-host-proof-recovery/v1"
    continuity = "[VXG RealForever]"
    attempt = "R001"
    recoveredAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    checkpointDirectory = $checkpoint.FullName
    recoveredFiles = @($checkpointFiles | ForEach-Object { $_.Name })
    replayPerformed = $false
    homeMutationPerformed = $false
    modelDownloadPerformed = $false
    modelLoadPerformed = $false
    productRuntimeStarted = $false
    productServiceStarted = $false
    installationPerformed = $false
    releaseEffectPerformed = $false
    effectsPerformed = $false
}

Write-Utf8NoBom -Path (Join-Path $checkpoint.FullName "recovery.json") -Text (($recovery | ConvertTo-Json -Depth 8) + $NL)
Write-Utf8NoBom -Path (Join-Path $checkpoint.FullName "recovery-inventory.json") -Text (($inventory | ConvertTo-Json -Depth 6) + $NL)

$tempZip = Join-Path $OutputDirectory "RETURN-VEXLIFE-WINDOWS-READINESS-R001-RECOVERY.forming.zip"
if (Test-Path -LiteralPath $tempZip) {
    Remove-Item -LiteralPath $tempZip -Force
}
Compress-Archive -Path (Join-Path $checkpoint.FullName "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha = Get-LowerSha256 $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-READINESS-R001-RECOVERY--sha256-" + $returnSha + ".zip")
if (Test-Path -LiteralPath $finalZip) {
    Remove-Item -LiteralPath $finalZip -Force
}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host "VEXLIFE_WINDOWS_READINESS_RECOVERY_RETURN"
Write-Host ("checkpointRoot=" + $checkpoint.FullName)
Write-Host ("path=" + $finalZip)
Write-Host ("sha256=" + $returnSha)
Write-Host "replayPerformed=false"
