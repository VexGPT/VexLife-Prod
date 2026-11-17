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
    Where-Object { $_.Name -like "VEXLIFE-WINDOWS-HOME-EVIDENCE-R003-CHECKPOINTS-*" } |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1

if ($null -eq $checkpoint) {
    throw "R003_CHECKPOINT_DIRECTORY_NOT_FOUND"
}

$files = @(Get-ChildItem -LiteralPath $checkpoint.FullName -File -Recurse | Sort-Object FullName)
if ($files.Count -eq 0) {
    throw "R003_CHECKPOINT_DIRECTORY_EMPTY"
}

$inventory = @(
    $files | ForEach-Object {
        [ordered]@{
            path = $_.FullName.Substring($checkpoint.FullName.Length).TrimStart('\')
            bytes = $_.Length
            sha256 = (Get-LowerSha256 $_.FullName)
        }
    }
)

$recoveryEvidence = [ordered]@{
    schemaVersion = "vexlife-prod.windows-home-contract-evidence-recovery/v1"
    continuity = "[VXG RealForever]"
    attempt = "R003"
    recoveredAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    checkpointDirectory = $checkpoint.FullName
    recoveredFiles = @($files | ForEach-Object { $_.FullName.Substring($checkpoint.FullName.Length).TrimStart('\') })
    replayPerformed = $false
    homeMutationPerformed = $false
    conversationBodiesRead = $false
    memoryBodiesRead = $false
    contextBodiesRead = $false
    modelWeightsRead = $false
    modelRuntimeStarted = $false
    productServiceStarted = $false
    effectsPerformed = $false
}

Write-Utf8NoBom -Path (Join-Path $checkpoint.FullName "recovery.json") -Text (($recoveryEvidence | ConvertTo-Json -Depth 8) + $NL)
Write-Utf8NoBom -Path (Join-Path $checkpoint.FullName "recovery-inventory.json") -Text (($inventory | ConvertTo-Json -Depth 6) + $NL)

$tempZip = Join-Path $OutputDirectory "RETURN-VEXLIFE-WINDOWS-HOME-EVIDENCE-R003-RECOVERY.forming.zip"
if (Test-Path -LiteralPath $tempZip) { Remove-Item -LiteralPath $tempZip -Force }
Compress-Archive -Path (Join-Path $checkpoint.FullName "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha = Get-LowerSha256 $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-HOME-EVIDENCE-R003-RECOVERY--sha256-" + $returnSha + ".zip")
if (Test-Path -LiteralPath $finalZip) { Remove-Item -LiteralPath $finalZip -Force }
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host "VEXLIFE_WINDOWS_HOME_EVIDENCE_RECOVERY_RETURN"
Write-Host ("path=" + $finalZip)
Write-Host ("sha256=" + $returnSha)
Write-Host "replayPerformed=false"
