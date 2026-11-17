# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$OutputDirectory = "$HOME\Downloads"
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

$checkpoint = Get-ChildItem -LiteralPath $OutputDirectory -Directory |
    Where-Object { $_.Name -like "VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R007-CHECKPOINTS-*" } |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1

if ($null -eq $checkpoint) {
    throw "WINDOWS_4B_PATIENT_ZERO_R007_CHECKPOINT_DIRECTORY_NOT_FOUND"
}

$files = @(Get-ChildItem -LiteralPath $checkpoint.FullName -File -Recurse | Sort-Object FullName)
if ($files.Count -eq 0) {
    throw "WINDOWS_4B_PATIENT_ZERO_R007_CHECKPOINT_DIRECTORY_EMPTY"
}

$recoveryEvidence = [ordered]@{
    schemaVersion = "vexlife-prod.windows-4b-patient-zero-r007-recovery/v1"
    continuity = "[VXG RealForever]"
    recoveredAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    checkpointDirectory = $checkpoint.FullName
    recoveredFiles = @(
        $files | ForEach-Object {
            $_.FullName.Substring($checkpoint.FullName.Length).TrimStart('\')
        }
    )
    replayPerformed = $false
    githubActionsUsed = $false
    realHomeMutationPerformedByRecovery = $false
    modelDownloadPerformedByRecovery = $false
    runtimeStartPerformedByRecovery = $false
    productServiceStartPerformedByRecovery = $false
}

Write-Utf8NoBom -Path (Join-Path $checkpoint.FullName "recovery.json") -Text (($recoveryEvidence | ConvertTo-Json -Depth 8) + [Environment]::NewLine)

$forming = Join-Path $OutputDirectory "RETURN-VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R007-RECOVERY.forming.zip"
if (Test-Path -LiteralPath $forming) {
    Remove-Item -LiteralPath $forming -Force
}
Compress-Archive -Path (Join-Path $checkpoint.FullName "*") -DestinationPath $forming -CompressionLevel Optimal
$sha = Get-LowerSha256 $forming
$final = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R007-RECOVERY--sha256-" + $sha + ".zip")
if (Test-Path -LiteralPath $final) {
    Remove-Item -LiteralPath $final -Force
}
Move-Item -LiteralPath $forming -Destination $final

Write-Host "VEXLIFE_WINDOWS_4B_PATIENT_ZERO_R007_RECOVERY_RETURN"
Write-Host ("path=" + $final)
Write-Host ("sha256=" + $sha)
Write-Host "replayPerformed=false"
