# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$OutputDirectory = "$HOME\Downloads"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Attempt = "R001"
$Repository = "VexGPT/VexLife-Prod"
$CarrierBranch = "VXG-111726-chatgpt-windows-4b-patient-zero-a001"
$CarrierWorkflow = "windows-4b-patient-zero-proof.yml"
$BuildSourceSha = $null
$ActionsRunId = $null
$ArtifactName = $null

$ModelUrl = "https://huggingface.co/yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-GGUF/resolve/c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61/model-Q4_K_M.gguf?download=true"
$ModelSha256 = "7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159"
$ModelBytes = 2790000800L

$LlamaServerSha256 = "af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7"

$JdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"
$JdkSha256 = "a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9"

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

function Write-Checkpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)]$Value
    )
    $path = Join-Path $CheckpointRoot $Name
    Write-Utf8NoBom -Path $path -Text (($Value | ConvertTo-Json -Depth 12) + [Environment]::NewLine)
    Write-Host ("checkpoint=" + $path)
}

function Resolve-Gh {
    $command = Get-Command gh.exe -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $known = "C:\Program Files\GitHub CLI\gh.exe"
    if (Test-Path -LiteralPath $known -PathType Leaf) {
        return $known
    }

    throw "GITHUB_CLI_NOT_FOUND"
}

function Form-Return {
    param(
        [Parameter(Mandatory = $true)][string]$Disposition,
        [string]$Failure
    )

    $summary = @(
        "VexLife Windows 4B patient-zero R001"
        "appSource=$BuildSourceSha"
        "actionsRun=$ActionsRunId"
        "disposition=$Disposition"
        "failure=$Failure"
        "realHomeTouched=false"
        "technicalHome=$TechnicalHome"
        ""
        "This is a temporary lived carrier for the current Product's existing heretic-4b choice."
    ) -join [Environment]::NewLine
    Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "RESULT-SUMMARY.txt") -Text ($summary + [Environment]::NewLine)

    $inventory = @()
    Get-ChildItem -LiteralPath $CheckpointRoot -File -Recurse | Sort-Object FullName | ForEach-Object {
        $relative = $_.FullName.Substring($CheckpointRoot.Length).TrimStart('\')
        $inventory += [ordered]@{
            path = $relative
            bytes = $_.Length
            sha256 = (Get-LowerSha256 $_.FullName)
        }
    }
    Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "inventory.json") -Text (($inventory | ConvertTo-Json -Depth 6) + [Environment]::NewLine)

    $forming = Join-Path $OutputDirectory "RETURN-VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R001.forming.zip"
    if (Test-Path -LiteralPath $forming) {
        Remove-Item -LiteralPath $forming -Force
    }
    Compress-Archive -Path (Join-Path $CheckpointRoot "*") -DestinationPath $forming -CompressionLevel Optimal
    $returnSha = Get-LowerSha256 $forming
    $final = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R001--sha256-" + $returnSha + ".zip")
    if (Test-Path -LiteralPath $final) {
        Remove-Item -LiteralPath $final -Force
    }
    Move-Item -LiteralPath $forming -Destination $final

    Write-Host ""
    Write-Host "VEXLIFE_WINDOWS_4B_PATIENT_ZERO_RETURN"
    Write-Host ("path=" + $final)
    Write-Host ("sha256=" + $returnSha)
    Write-Host ("disposition=" + $Disposition)
    return $final
}

if ($env:OS -ne "Windows_NT") {
    throw "WINDOWS_4B_PATIENT_ZERO_REQUIRES_WINDOWS"
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$Timestamp = [DateTimeOffset]::UtcNow.ToString("yyyyMMddTHHmmssZ")
$CheckpointRoot = Join-Path $OutputDirectory ("VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R001-CHECKPOINTS-" + $Timestamp)
New-Item -ItemType Directory -Path $CheckpointRoot | Out-Null

$CacheRoot = Join-Path $OutputDirectory "VEXLIFE-WINDOWS-4B-PATIENT-ZERO-CACHE"
New-Item -ItemType Directory -Path $CacheRoot -Force | Out-Null
$TechnicalHome = Join-Path $OutputDirectory "VEXLIFE-WINDOWS-4B-PATIENT-ZERO-HOME"

$failure = $null
$disposition = "SAFE_FAILURE"

try {
    Write-Host ""
    Write-Host "VexLife Windows 4B patient-zero R001"
    Write-Host "This run uses a disposable technical Home."
    Write-Host ("Your real Home remains untouched: " + (Join-Path $HOME ".vexlife"))
    Write-Host "Model: current Product heretic-4b only."
    Write-Host ""

    Write-Host "[1/6] Verify latest successful hosted VexLife patient-zero build"
    $gh = Resolve-Gh
    $runsJson = & $gh run list --repo $Repository --branch $CarrierBranch --workflow $CarrierWorkflow --status success --limit 20 --json databaseId,headSha,conclusion,createdAt 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "GITHUB_ACTIONS_RUN_LIST_FAILED"
    }
    $successfulRuns = @($runsJson | ConvertFrom-Json)
    $run = $successfulRuns | Select-Object -First 1
    if ($null -eq $run -or $run.conclusion -ne "success" -or [string]::IsNullOrWhiteSpace([string]$run.headSha)) {
        throw "NO_SUCCESSFUL_PATIENT_ZERO_BUILD_AVAILABLE"
    }

    $ActionsRunId = [string]$run.databaseId
    $BuildSourceSha = [string]$run.headSha
    $ArtifactName = "windows-4b-patient-zero-" + $BuildSourceSha

    $ArtifactRoot = Join-Path $env:TEMP ("vexlife-win4b-app-" + $BuildSourceSha.Substring(0,12))
    if (Test-Path -LiteralPath $ArtifactRoot) {
        Remove-Item -LiteralPath $ArtifactRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path $ArtifactRoot | Out-Null

    & $gh run download $ActionsRunId --repo $Repository -n $ArtifactName -D $ArtifactRoot
    if ($LASTEXITCODE -ne 0) {
        throw "GITHUB_ACTIONS_ARTIFACT_DOWNLOAD_FAILED"
    }

    $bundleZip = Get-ChildItem -LiteralPath $ArtifactRoot -Filter "VEXLIFE-WINDOWS-4B-PATIENT-ZERO-BUNDLE.zip" -File | Select-Object -First 1
    $bundleReceipt = Get-ChildItem -LiteralPath $ArtifactRoot -Filter "bundle-receipt.txt" -File | Select-Object -First 1
    if ($null -eq $bundleZip -or $null -eq $bundleReceipt) {
        throw "HOSTED_BUNDLE_ARTIFACT_INCOMPLETE"
    }

    $receiptLines = Get-Content -LiteralPath $bundleReceipt.FullName
    $receiptMap = @{}
    foreach ($line in $receiptLines) {
        if ($line -match '^([^=]+)=(.*)$') {
            $receiptMap[$Matches[1]] = $Matches[2]
        }
    }
    $bundleSha = Get-LowerSha256 $bundleZip.FullName
    if ($receiptMap["sourceSha"] -ne $BuildSourceSha -or $receiptMap["bundleSha256"] -ne $bundleSha) {
        throw "HOSTED_BUNDLE_RECEIPT_MISMATCH"
    }

    $AppRoot = Join-Path $ArtifactRoot "app"
    New-Item -ItemType Directory -Path $AppRoot | Out-Null
    Expand-Archive -LiteralPath $bundleZip.FullName -DestinationPath $AppRoot

    Write-Checkpoint -Name "01-app-build.json" -Value ([ordered]@{
        stage = "HOSTED_APP_BUILD_VERIFIED"
        sourceSha = $BuildSourceSha
        actionsRun = $ActionsRunId
        bundleSha256 = $bundleSha
        effectsPerformed = $false
    })
    Write-Host "PASS: hosted VexLife carrier verified"
    Write-Host ""

    Write-Host "[2/6] Verify reusable Windows llama.cpp runtime from existing Home"
    $realModelConfig = Join-Path $HOME ".vexlife\config\model.json"
    if (-not (Test-Path -LiteralPath $realModelConfig -PathType Leaf)) {
        throw "PREDECESSOR_RUNTIME_EVIDENCE_NOT_AVAILABLE"
    }
    $historical = Get-Content -LiteralPath $realModelConfig -Raw | ConvertFrom-Json
    $llamaServer = [string]$historical.runtimeExecutablePath
    if ([string]::IsNullOrWhiteSpace($llamaServer) -or -not (Test-Path -LiteralPath $llamaServer -PathType Leaf)) {
        throw "EXISTING_LLAMA_SERVER_NOT_PRESENT"
    }
    $observedLlamaSha = Get-LowerSha256 $llamaServer
    if ($observedLlamaSha -ne $LlamaServerSha256) {
        throw ("EXISTING_LLAMA_SERVER_HASH_MISMATCH observed=" + $observedLlamaSha)
    }
    Write-Checkpoint -Name "02-runtime-dependency.json" -Value ([ordered]@{
        stage = "EXISTING_LLAMA_CPP_REUSED_EXACT"
        sha256 = $observedLlamaSha
        predecessorModelAuthorityUsed = $false
        runtimeBytesReusedOnly = $true
        effectsPerformed = $false
    })
    Write-Host "PASS: existing exact llama-server.exe is reusable machinery"
    Write-Host ""

    Write-Host "[3/6] Materialize current Product heretic-4b Windows GGUF"
    $ModelPath = Join-Path $CacheRoot "model-Q4_K_M.gguf"
    $reuse = $false
    if (Test-Path -LiteralPath $ModelPath -PathType Leaf) {
        $item = Get-Item -LiteralPath $ModelPath
        if ($item.Length -eq $ModelBytes -and (Get-LowerSha256 $ModelPath) -eq $ModelSha256) {
            $reuse = $true
        } else {
            throw "EXISTING_4B_CACHE_IS_NOT_EXACT"
        }
    }

    if (-not $reuse) {
        $partial = $ModelPath + ".partial"
        if (Test-Path -LiteralPath $partial) {
            Remove-Item -LiteralPath $partial -Force
        }
        Write-Host "Downloading ~2.8 GB current heretic-4b GGUF. Progress below:"
        & curl.exe -L --fail --progress-bar -o $partial $ModelUrl
        if ($LASTEXITCODE -ne 0) {
            throw "CURRENT_4B_GGUF_DOWNLOAD_FAILED"
        }
        $item = Get-Item -LiteralPath $partial
        if ($item.Length -ne $ModelBytes) {
            throw ("CURRENT_4B_GGUF_SIZE_MISMATCH observed=" + $item.Length)
        }
        $modelObservedSha = Get-LowerSha256 $partial
        if ($modelObservedSha -ne $ModelSha256) {
            throw ("CURRENT_4B_GGUF_HASH_MISMATCH observed=" + $modelObservedSha)
        }
        Move-Item -LiteralPath $partial -Destination $ModelPath
    }

    Write-Checkpoint -Name "03-model-artifact.json" -Value ([ordered]@{
        stage = "CURRENT_HERETIC_4B_WINDOWS_GGUF_EXACT"
        bytes = (Get-Item -LiteralPath $ModelPath).Length
        sha256 = (Get-LowerSha256 $ModelPath)
        reused = $reuse
        visionProjectorMaterialized = $false
        effectsPerformed = (-not $reuse)
    })
    Write-Host "PASS: exact current heretic-4b GGUF ready"
    Write-Host ""

    Write-Host "[4/6] Materialize attempt-local Java 21 runtime"
    $JdkZip = Join-Path $CacheRoot "OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"
    if (-not (Test-Path -LiteralPath $JdkZip -PathType Leaf)) {
        $jdkPartial = $JdkZip + ".partial"
        if (Test-Path -LiteralPath $jdkPartial) { Remove-Item -LiteralPath $jdkPartial -Force }
        Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $jdkPartial
        if ((Get-LowerSha256 $jdkPartial) -ne $JdkSha256) {
            throw "JDK_SHA256_MISMATCH"
        }
        Move-Item -LiteralPath $jdkPartial -Destination $JdkZip
    } elseif ((Get-LowerSha256 $JdkZip) -ne $JdkSha256) {
        throw "CACHED_JDK_SHA256_MISMATCH"
    }

    $JdkRoot = Join-Path $env:TEMP "vexlife-win4b-jdk-21.0.13"
    if (Test-Path -LiteralPath $JdkRoot) {
        Remove-Item -LiteralPath $JdkRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path $JdkRoot | Out-Null
    Expand-Archive -LiteralPath $JdkZip -DestinationPath $JdkRoot
    $jdkDirs = @(Get-ChildItem -LiteralPath $JdkRoot -Directory)
    if ($jdkDirs.Count -ne 1) {
        throw "JDK_ARCHIVE_ROOT_AMBIGUOUS"
    }
    $java = Join-Path $jdkDirs[0].FullName "bin\java.exe"
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        throw "JDK_JAVA_MISSING"
    }
    Write-Checkpoint -Name "04-java.json" -Value ([ordered]@{
        stage = "ATTEMPT_LOCAL_JAVA_READY"
        jdkSha256 = $JdkSha256
        persistentGlobalInstall = $false
    })
    Write-Host "PASS: attempt-local Java ready"
    Write-Host ""

    Write-Host "[5/6] Run actual VexLife-Prod 4B service/Conversation/restart lifecycle"
    Write-Host ("Technical Home: " + $TechnicalHome)
    Write-Host "Your real Home will not be modified."

    $Classpath = (Join-Path $AppRoot "vexlife-prod.jar") + ";" + (Join-Path $AppRoot "lib\*")
    $ProcessLog = Join-Path $CheckpointRoot "vexlife-patient-zero-process.log"
    $javaArgs = @(
        "-cp"
        $Classpath
        "vexlife.platform.windows.Windows4BPatientZeroProcessKt"
        "--source-sha"
        $BuildSourceSha
        "--home"
        $TechnicalHome
        "--model"
        $ModelPath
        "--llama-server"
        $llamaServer
        "--checkpoints"
        $CheckpointRoot
    )

    $priorPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $java @javaArgs 2>&1 | Tee-Object -FilePath $ProcessLog
        $javaExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $priorPreference
    }

    Write-Checkpoint -Name "10-process-exit.json" -Value ([ordered]@{
        stage = "PATIENT_ZERO_PROCESS_EXITED"
        exitCode = $javaExit
        technicalHome = $TechnicalHome
        realHomeTouched = $false
    })

    if ($javaExit -ne 0) {
        throw ("PATIENT_ZERO_JAVA_EXIT_" + $javaExit)
    }

    if (-not (Test-Path -LiteralPath (Join-Path $CheckpointRoot "09-terminal.json") -PathType Leaf)) {
        throw "PATIENT_ZERO_TERMINAL_CHECKPOINT_MISSING"
    }

    $disposition = "PASS"
    Write-Host "PASS: VexLife Windows 4B end-to-end lifecycle completed"
    Write-Host ""

    Write-Host "[6/6] Form checkpointed RETURN"
} catch {
    $failure = $_.Exception.Message
    Write-Host ""
    Write-Host ("SAFE_FAILURE: " + $failure)
    try {
        Write-Checkpoint -Name "99-harness-failure.json" -Value ([ordered]@{
            stage = "HARNESS_SAFE_FAILURE"
            failure = $failure
            realHomeTouched = $false
            technicalHome = $TechnicalHome
        })
    } catch {
    }
} finally {
    try {
        $null = Form-Return -Disposition $disposition -Failure $failure
    } catch {
        Write-Host ("RETURN_FORMATION_FAILED: " + $_.Exception.Message)
    }
}

if ($disposition -ne "PASS") {
    exit 2
}
