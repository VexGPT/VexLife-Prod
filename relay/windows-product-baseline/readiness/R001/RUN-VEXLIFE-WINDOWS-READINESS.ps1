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

$Schema = "vexlife-prod.windows-readiness-host-proof/v1"
$Attempt = "R001"
$CandidatePr = 223
$CandidateHead = "9ba98244269897a04535a9d27996b5cafc50e758"
$CandidateTree = "65e56fd706d28efcd1ac77191b2161483726b634"
$CandidateBase = "262c208b2e65e828dd867719c29ecd286ae58a80"

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
    return $path
}

function Normalize-WindowsArchitecture {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return "UNKNOWN" }
    switch ($Value.Trim().ToUpperInvariant()) {
        "AMD64" { return "X86_64" }
        "X64" { return "X86_64" }
        "X86_64" { return "X86_64" }
        "ARM64" { return "ARM64" }
        "AARCH64" { return "ARM64" }
        default { return "UNKNOWN:" + $Value }
    }
}

if ($env:OS -ne "Windows_NT") {
    throw "WINDOWS_READINESS_REQUIRES_WINDOWS_NT"
}

$proposedHomeFull = [IO.Path]::GetFullPath($ProposedHome)
if (-not [IO.Path]::IsPathRooted($proposedHomeFull)) {
    throw "PROPOSED_HOME_MUST_BE_ABSOLUTE"
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$attemptId = [DateTimeOffset]::UtcNow.ToString("yyyyMMddTHHmmssZ")
$CheckpointRoot = Join-Path $OutputDirectory ("VEXLIFE-WINDOWS-READINESS-R001-CHECKPOINTS-" + $attemptId)
New-Item -ItemType Directory -Path $CheckpointRoot | Out-Null

Write-Host ""
Write-Host "VexLife Windows readiness R001"
Write-Host ("checkpointRoot=" + $CheckpointRoot)
Write-Host "No model will be downloaded or loaded."
Write-Host "No Vex runtime or Product service will be started."
Write-Host "No Vex Home will be created or modified."
Write-Host ""

# Stage 1 — actual host resources.
Write-Host "[1/4] Observe this Windows machine"
$os = Get-CimInstance Win32_OperatingSystem
$cpuRows = @(
    Get-CimInstance Win32_Processor |
        ForEach-Object {
            [ordered]@{
                name = [string]$_.Name
                cores = [int]$_.NumberOfCores
                logicalProcessors = [int]$_.NumberOfLogicalProcessors
                maxClockMHz = [int]$_.MaxClockSpeed
            }
        }
)

$nativeArchitectureRaw =
    if (-not [string]::IsNullOrWhiteSpace($env:PROCESSOR_ARCHITEW6432)) {
        $env:PROCESSOR_ARCHITEW6432
    } else {
        $env:PROCESSOR_ARCHITECTURE
    }

$totalMemoryBytes = [long]$os.TotalVisibleMemorySize * 1024L
$freeMemoryBytes = [long]$os.FreePhysicalMemory * 1024L
$host = [ordered]@{
    schemaVersion = $Schema
    stage = "HOST_OBSERVED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    osCaption = [string]$os.Caption
    osVersion = [string]$os.Version
    nativeArchitectureRaw = $nativeArchitectureRaw
    nativeArchitecture = Normalize-WindowsArchitecture $nativeArchitectureRaw
    processArchitectureRaw = $env:PROCESSOR_ARCHITECTURE
    processArchitecture = Normalize-WindowsArchitecture $env:PROCESSOR_ARCHITECTURE
    processorArchitectureW6432 = $env:PROCESSOR_ARCHITEW6432
    totalMemoryBytes = $totalMemoryBytes
    freeMemoryBytes = $freeMemoryBytes
    usedMemoryBytes = $totalMemoryBytes - $freeMemoryBytes
    processors = $cpuRows
    powershellVersion = $PSVersionTable.PSVersion.ToString()
    effectsPerformed = $false
}
[void](Write-JsonCheckpoint -Name "01-host.json" -Value $host)
Write-Host ("PASS: " + $host.nativeArchitecture + ", RAM=" + [math]::Round($totalMemoryBytes / 1GB, 1) + " GiB")
Write-Host ""

# Stage 2 — proposed Home coordinate and its volume. This is path observation only.
Write-Host "[2/4] Observe proposed Vex Home coordinate and storage"
$homeExists = Test-Path -LiteralPath $proposedHomeFull
$homeKind =
    if (-not $homeExists) {
        "ABSENT_PROPOSED_COORDINATE"
    } elseif (Test-Path -LiteralPath $proposedHomeFull -PathType Container) {
        "EXISTING_DIRECTORY_NOT_CANONICALLY_CLASSIFIED_BY_PROBE"
    } else {
        "EXISTING_NON_DIRECTORY"
    }

$driveRoot = [IO.Path]::GetPathRoot($proposedHomeFull)
$drive = New-Object System.IO.DriveInfo($driveRoot)
$home = [ordered]@{
    schemaVersion = $Schema
    stage = "HOME_COORDINATE_OBSERVED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    proposedHome = $proposedHomeFull
    pathState = $homeKind
    canonicalHomeClassification = "NOT_EXECUTED_BY_HOST_PROBE"
    driveRoot = $driveRoot
    driveFormat = $drive.DriveFormat
    driveTotalBytes = [long]$drive.TotalSize
    driveFreeBytes = [long]$drive.AvailableFreeSpace
    effectsPerformed = $false
}
[void](Write-JsonCheckpoint -Name "02-home-storage.json" -Value $home)
Write-Host ("PASS: Home path=" + $home.pathState + ", free storage=" + [math]::Round($home.driveFreeBytes / 1GB, 1) + " GiB")
Write-Host ""

# Stage 3 — accelerator evidence only. No driver/runtime installation or model effect.
Write-Host "[3/4] Observe graphics / accelerator evidence"
$video = @(
    Get-CimInstance Win32_VideoController |
        ForEach-Object {
            [ordered]@{
                name = [string]$_.Name
                adapterRamBytes = if ($null -eq $_.AdapterRAM) { $null } else { [long]$_.AdapterRAM }
                driverVersion = [string]$_.DriverVersion
            }
        }
)

$nvidiaCommand = Get-Command nvidia-smi.exe -ErrorAction SilentlyContinue
$nvidiaRows = @()
if ($null -ne $nvidiaCommand) {
    try {
        $rawRows = @(
            & $nvidiaCommand.Source --query-gpu=name,memory.total,driver_version --format=csv,noheader,nounits 2>$null
        )
        foreach ($row in $rawRows) {
            $parts = @($row -split "," | ForEach-Object { $_.Trim() })
            if ($parts.Count -ge 3) {
                $nvidiaRows += [ordered]@{
                    name = $parts[0]
                    memoryTotalMiB = [int]$parts[1]
                    driverVersion = $parts[2]
                }
            }
        }
    } catch {
        $nvidiaRows = @()
    }
}

$accelerator = [ordered]@{
    schemaVersion = $Schema
    stage = "ACCELERATOR_EVIDENCE_OBSERVED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    videoControllers = $video
    nvidiaSmiPresent = ($null -ne $nvidiaCommand)
    nvidiaGpus = $nvidiaRows
    inferenceRuntimeSelected = $false
    modelSelected = $false
    effectsPerformed = $false
}
[void](Write-JsonCheckpoint -Name "03-accelerator.json" -Value $accelerator)
Write-Host ("PASS: video controllers=" + $video.Count + ", nvidia-smi=" + $accelerator.nvidiaSmiPresent)
Write-Host ""

# Stage 4 — combine observed host facts with the exact current source candidate boundary.
Write-Host "[4/4] Form no-effect Windows readiness summary"
$readiness = [ordered]@{
    schemaVersion = $Schema
    stage = "WINDOWS_READINESS_OBSERVED"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    candidate = [ordered]@{
        pullRequest = $CandidatePr
        head = $CandidateHead
        tree = $CandidateTree
        baseMain = $CandidateBase
        hostedWindowsSourceProof = "PASS"
        hostedWindowsActionsRun = 44053924414
    }
    host = $host
    home = $home
    accelerator = $accelerator
    realization = [ordered]@{
        runtimeState = "WINDOWS_RUNTIME_REALIZATION_UNAVAILABLE"
        serviceState = "WINDOWS_PRODUCT_SERVICE_UNAVAILABLE"
        nativeSetupState = "WINDOWS_NATIVE_SETUP_UNAVAILABLE"
        packageState = "WINDOWS_PACKAGE_UNAVAILABLE"
    }
    nextDecision = "SELECT_SMALLEST_RIGHTFUL_WINDOWS_COMPATIBLE_MODEL_RUNTIME_AFTER_HARDWARE_REVIEW"
    modelDownloadPerformed = $false
    modelLoadPerformed = $false
    productRuntimeStarted = $false
    productServiceStarted = $false
    homeMutationPerformed = $false
    installationPerformed = $false
    releaseEffectPerformed = $false
    effectsPerformed = $false
}
[void](Write-JsonCheckpoint -Name "04-readiness.json" -Value $readiness)

$summary = @(
    "VexLife Windows readiness R001"
    ""
    "Host"
    "  target: WINDOWS / $($host.nativeArchitecture)"
    "  RAM: $([math]::Round($host.totalMemoryBytes / 1GB, 1)) GiB total"
    "  RAM currently free: $([math]::Round($host.freeMemoryBytes / 1GB, 1)) GiB"
    ""
    "Proposed Home"
    "  path: $($home.proposedHome)"
    "  path state: $($home.pathState)"
    "  canonical Home classification: NOT EXECUTED BY THIS PROBE"
    "  free storage: $([math]::Round($home.driveFreeBytes / 1GB, 1)) GiB"
    ""
    "Accelerator"
    "  video controllers: $($video.Count)"
    "  nvidia-smi present: $($accelerator.nvidiaSmiPresent)"
    "  NVIDIA GPUs observed: $($nvidiaRows.Count)"
    ""
    "Windows Product realization"
    "  runtime: $($readiness.realization.runtimeState)"
    "  service: $($readiness.realization.serviceState)"
    "  native setup: $($readiness.realization.nativeSetupState)"
    "  package: $($readiness.realization.packageState)"
    ""
    "Effects performed: NONE"
    "No model was downloaded or loaded."
    "No Vex runtime or Product service was started."
) -join $NL
Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "RESULT-SUMMARY.txt") -Text ($summary + $NL)
Write-Host "PASS: readiness summary formed"
Write-Host ""

$inventory = @()
Get-ChildItem -LiteralPath $CheckpointRoot -File | Sort-Object Name | ForEach-Object {
    $inventory += [ordered]@{
        path = $_.Name
        bytes = $_.Length
        sha256 = (Get-LowerSha256 $_.FullName)
    }
}
Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "inventory.json") -Text (($inventory | ConvertTo-Json -Depth 6) + $NL)

$tempZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-READINESS-R001-" + $attemptId + ".forming.zip")
Compress-Archive -Path (Join-Path $CheckpointRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha = Get-LowerSha256 $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-READINESS-R001--sha256-" + $returnSha + ".zip")
if (Test-Path -LiteralPath $finalZip) {
    Remove-Item -LiteralPath $finalZip -Force
}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host "VEXLIFE_WINDOWS_READINESS_RETURN"
Write-Host ("path=" + $finalZip)
Write-Host ("sha256=" + $returnSha)
Write-Host "effectsPerformed=false"

# Successful RETURN now carries the checkpoints. Cancellation before this point intentionally leaves
# CheckpointRoot in Downloads for recovery.
Remove-Item -LiteralPath $CheckpointRoot -Recurse -Force
