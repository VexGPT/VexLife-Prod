# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$ProposedHome = (Join-Path $HOME ".vexlife"),

    [Parameter(Mandatory = $false)]
    [string]$OutputDirectory = "$HOME\Downloads"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$NL = [Environment]::NewLine
$Schema = "vexlife-prod.windows-home-contract-evidence/v1"
$Attempt = "R003"

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

function Write-JsonCheckpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)]$Value
    )
    $path = Join-Path $CheckpointRoot $Name
    Write-Utf8NoBom -Path $path -Text (($Value | ConvertTo-Json -Depth 12) + $NL)
    Write-Host ("checkpoint=" + $path)
}

function Observe-Contract {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string]$CopyName
    )

    $path = Join-Path $homeRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        return [ordered]@{
            relativePath = $RelativePath
            present = $false
            bytes = $null
            sha256 = $null
            jsonParsed = $false
            schemaVersion = $null
            copyName = $null
        }
    }

    $copyPath = Join-Path $ContractRoot $CopyName
    Copy-Item -LiteralPath $path -Destination $copyPath

    $schemaVersion = $null
    $parsed = $false
    try {
        $parsedJson = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
        $parsed = $true
        if ($null -ne $parsedJson.schemaVersion) {
            $schemaVersion = [string]$parsedJson.schemaVersion
        }
    } catch {
        $parsed = $false
    }

    $item = Get-Item -LiteralPath $path
    return [ordered]@{
        relativePath = $RelativePath
        present = $true
        bytes = [long]$item.Length
        sha256 = (Get-LowerSha256 $path)
        jsonParsed = $parsed
        schemaVersion = $schemaVersion
        copyName = $CopyName
    }
}

if ($env:OS -ne "Windows_NT") {
    throw "R003_REQUIRES_WINDOWS_NT"
}

$homeRoot = [IO.Path]::GetFullPath($ProposedHome)
if (-not (Test-Path -LiteralPath $homeRoot -PathType Container)) {
    throw ("R003_PROPOSED_HOME_DIRECTORY_NOT_FOUND:" + $homeRoot)
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$attemptId = [DateTimeOffset]::UtcNow.ToString("yyyyMMddTHHmmssZ")
$CheckpointRoot = Join-Path $OutputDirectory ("VEXLIFE-WINDOWS-HOME-EVIDENCE-R003-CHECKPOINTS-" + $attemptId)
$ContractRoot = Join-Path $CheckpointRoot "contracts"
New-Item -ItemType Directory -Path $CheckpointRoot | Out-Null
New-Item -ItemType Directory -Path $ContractRoot | Out-Null

Write-Host ""
Write-Host "VexLife Windows Home evidence R003"
Write-Host ("Home=" + $homeRoot)
Write-Host "Read-only evidence only."
Write-Host "No Home files will be changed."
Write-Host "No conversation, Memory, context body, model weight, runtime or service content will be read."
Write-Host ""

# Stage 1 — shape only.
Write-Host "[1/4] Observe canonical Home coordinate shape"
$expectedDirectories = @(
    "config",
    "devices",
    "runtime",
    "recovery",
    "conversations",
    "context",
    "score",
    "daily-memory-dream"
)
$directoryStates = @()
foreach ($relative in $expectedDirectories) {
    $directoryStates += [ordered]@{
        relativePath = $relative
        present = (Test-Path -LiteralPath (Join-Path $homeRoot $relative) -PathType Container)
    }
}
$layout = [ordered]@{
    schemaVersion = $Schema
    stage = "HOME_LAYOUT_OBSERVED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    homePath = $homeRoot
    directories = $directoryStates
    contentsEnumerated = $false
    personalDataRead = $false
    effectsPerformed = $false
}
Write-JsonCheckpoint -Name "01-layout.json" -Value $layout
Write-Host "PASS: directory presence recorded; contents not enumerated."
Write-Host ""

# Stage 2 — canonical identity coordinates only.
Write-Host "[2/4] Capture Home manifest and its named current-device record"
$manifestObservation = Observe-Contract -RelativePath "config\home.json" -CopyName "config-home.json"
$currentDeviceRef = $null
$deviceObservation = [ordered]@{
    relativePath = $null
    present = $false
    bytes = $null
    sha256 = $null
    jsonParsed = $false
    schemaVersion = $null
    copyName = $null
    reason = "HOME_MANIFEST_NOT_PARSED_OR_CURRENT_DEVICE_REF_ABSENT"
}

if ($manifestObservation.present -and $manifestObservation.jsonParsed) {
    try {
        $manifestJson = Get-Content -LiteralPath (Join-Path $homeRoot "config\home.json") -Raw | ConvertFrom-Json
        if ($null -ne $manifestJson.currentDeviceRef) {
            $candidateDeviceRef = [string]$manifestJson.currentDeviceRef
            if ($candidateDeviceRef -match '^device\.vexlife\.[A-Za-z0-9._-]+$') {
                $currentDeviceRef = $candidateDeviceRef
                $deviceRelative = "devices\" + $currentDeviceRef + ".json"
                $deviceObservation = Observe-Contract -RelativePath $deviceRelative -CopyName "current-device.json"
                $deviceObservation["reason"] = "MANIFEST_NAMED_CURRENT_DEVICE_RECORD"
            } else {
                $deviceObservation["reason"] = "CURRENT_DEVICE_REF_OUTSIDE_SAFE_VEXLIFE_PATTERN"
            }
        }
    } catch {
        $deviceObservation["reason"] = "HOME_MANIFEST_PARSE_FAILED"
    }
}

$identityEvidence = [ordered]@{
    schemaVersion = $Schema
    stage = "IDENTITY_CONTRACT_EVIDENCE_CAPTURED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    manifest = $manifestObservation
    currentDeviceRecord = $deviceObservation
    canonicalValidationPerformed = $false
    rawIdentityPrintedToTerminal = $false
    effectsPerformed = $false
}
Write-JsonCheckpoint -Name "02-identity-contracts.json" -Value $identityEvidence
Write-Host ("PASS: manifest present=" + $manifestObservation.present + ", schema=" + $manifestObservation.schemaVersion)
Write-Host ("PASS: current-device record present=" + $deviceObservation.present + ", schema=" + $deviceObservation.schemaVersion)
Write-Host ""

# Stage 3 — model/binding/bootstrap contracts only.
Write-Host "[3/4] Capture model/binding/bootstrap contract evidence"
$canonicalModel = Observe-Contract -RelativePath "config\model.json" -CopyName "config-model.json"
$productBinding = Observe-Contract -RelativePath "runtime\vexlife-prod\model-binding.json" -CopyName "product-model-binding.json"
$bootstrapReceipt = Observe-Contract -RelativePath "recovery\bootstrap-receipt.json" -CopyName "bootstrap-receipt.json"

$modelEvidence = [ordered]@{
    schemaVersion = $Schema
    stage = "MODEL_AND_BOOTSTRAP_CONTRACT_EVIDENCE_CAPTURED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    canonicalModelConfiguration = $canonicalModel
    productModelBinding = $productBinding
    bootstrapReceipt = $bootstrapReceipt
    modelWeightsRead = $false
    modelRuntimeStarted = $false
    effectsPerformed = $false
}
Write-JsonCheckpoint -Name "03-model-bootstrap-contracts.json" -Value $modelEvidence
Write-Host ("PASS: config/model.json present=" + $canonicalModel.present + ", schema=" + $canonicalModel.schemaVersion)
Write-Host ("PASS: Product model binding present=" + $productBinding.present + ", schema=" + $productBinding.schemaVersion)
Write-Host ("PASS: bootstrap receipt present=" + $bootstrapReceipt.present + ", schema=" + $bootstrapReceipt.schemaVersion)
Write-Host ""

# Stage 4 — bounded summary. Canonical meaning is intentionally deferred to Product/source validation.
Write-Host "[4/4] Form bounded Home evidence summary"
$summaryEvidence = [ordered]@{
    schemaVersion = $Schema
    stage = "HOME_CONTRACT_EVIDENCE_READY"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    homePath = $homeRoot
    homeManifestPresent = $manifestObservation.present
    currentDeviceRecordPresent = $deviceObservation.present
    canonicalModelConfigurationPresent = $canonicalModel.present
    productModelBindingPresent = $productBinding.present
    bootstrapReceiptPresent = $bootstrapReceipt.present
    canonicalClassification = "NOT_PERFORMED_BY_R003"
    nextDecision = "VALIDATE_CAPTURED_CONTRACTS_AGAINST_CURRENT_PRODUCT_SOURCE_BEFORE_RUNTIME_SELECTION"
    conversationsRead = $false
    memoryRead = $false
    contextBodiesRead = $false
    modelWeightsRead = $false
    homeMutationPerformed = $false
    effectsPerformed = $false
}
Write-JsonCheckpoint -Name "04-summary.json" -Value $summaryEvidence

$summaryText = @(
    "VexLife Windows Home evidence R003"
    ""
    "Home manifest present: $($manifestObservation.present)"
    "Home manifest schema: $($manifestObservation.schemaVersion)"
    "Current-device record present: $($deviceObservation.present)"
    "Current-device schema: $($deviceObservation.schemaVersion)"
    "Canonical model config present: $($canonicalModel.present)"
    "Canonical model config schema: $($canonicalModel.schemaVersion)"
    "Product model binding present: $($productBinding.present)"
    "Product model binding schema: $($productBinding.schemaVersion)"
    "Bootstrap receipt present: $($bootstrapReceipt.present)"
    "Bootstrap receipt schema: $($bootstrapReceipt.schemaVersion)"
    ""
    "Canonical classification: NOT PERFORMED BY R003"
    "Effects performed: NONE"
    "Conversation / Memory / context bodies read: NO"
    "Model weights read or loaded: NO"
) -join $NL
Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "RESULT-SUMMARY.txt") -Text ($summaryText + $NL)
Write-Host "PASS: contract evidence captured; canonical meaning deliberately deferred."
Write-Host ""

$inventory = @()
Get-ChildItem -LiteralPath $CheckpointRoot -File -Recurse | Sort-Object FullName | ForEach-Object {
    $relative = $_.FullName.Substring($CheckpointRoot.Length).TrimStart('\')
    $inventory += [ordered]@{
        path = $relative
        bytes = $_.Length
        sha256 = (Get-LowerSha256 $_.FullName)
    }
}
Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "inventory.json") -Text (($inventory | ConvertTo-Json -Depth 6) + $NL)

$tempZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-HOME-EVIDENCE-R003-" + $attemptId + ".forming.zip")
Compress-Archive -Path (Join-Path $CheckpointRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha = Get-LowerSha256 $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-HOME-EVIDENCE-R003--sha256-" + $returnSha + ".zip")
if (Test-Path -LiteralPath $finalZip) {
    Remove-Item -LiteralPath $finalZip -Force
}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host "VEXLIFE_WINDOWS_HOME_EVIDENCE_RETURN"
Write-Host ("path=" + $finalZip)
Write-Host ("sha256=" + $returnSha)
Write-Host "effectsPerformed=false"

# Successful RETURN now carries the raw contract evidence privately back to Victor/ChatGPT.
Remove-Item -LiteralPath $CheckpointRoot -Recurse -Force
