# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$OutputDirectory = "$HOME\Downloads"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Attempt = "R011"
$Repository = "VexGPT/VexLife-Prod"
$ProductSourceSha = "9c0cbb5dc9800cb35624dd432616b84ac9f5c248"
$SourceZipUrl = "https://github.com/VexGPT/VexLife-Prod/archive/$ProductSourceSha.zip"

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
    Write-Utf8NoBom -Path $path -Text (($Value | ConvertTo-Json -Depth 14) + [Environment]::NewLine)
    Write-Host ("checkpoint=" + $path)
}

function Host-Memory {
    $os = Get-CimInstance Win32_OperatingSystem
    $total = [long]$os.TotalVisibleMemorySize * 1024L
    $free = [long]$os.FreePhysicalMemory * 1024L
    return [ordered]@{
        totalMemoryBytes = $total
        freeMemoryBytes = $free
        usedMemoryBytes = $total - $free
        usedPercent = [math]::Round((($total - $free) * 100.0) / $total, 1)
    }
}

function Invoke-Gradle {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$LogPath
    )

    $before = Host-Memory
    Write-Host ("START " + $Label + " | usedRAM=" + $before.usedPercent + "%")
    Push-Location $RepoRoot
    $priorPreference = $ErrorActionPreference
    try {
        # Native stderr is evidence, not a PowerShell terminating exception. Keep the stream visible
        # and let the exact process exit code decide the Gradle result.
        $ErrorActionPreference = "Continue"
        & $env:ComSpec /d /s /c ("call gradlew.bat " + ($Arguments -join " ")) 2>&1 |
            Tee-Object -FilePath $LogPath
        $exit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $priorPreference
        Pop-Location
    }
    $after = Host-Memory
    if ($exit -ne 0) {
        throw ($Label + "_EXIT_" + $exit)
    }
    Write-Host ("PASS " + $Label + " | usedRAM=" + $after.usedPercent + "%")
}

function Form-Return {
    param(
        [Parameter(Mandatory = $true)][string]$Disposition,
        [string]$Failure
    )

    $summary = @(
        "VexLife Windows 4B patient-zero R011"
        "productSource=$ProductSourceSha"
        "execution=LOCAL_RELAY_NO_GITHUB_ACTIONS"
        "disposition=$Disposition"
        "failure=$Failure"
        "realHomeTouched=false"
        "technicalHome=$TechnicalHome"
        ""
        "R011 re-proves the upstream Entry/Models Family-to-realization chain after completing its target-coverage and multilingual test-surface migration, then re-runs the lived Windows path."
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
    Write-Utf8NoBom -Path (Join-Path $CheckpointRoot "inventory.json") -Text (($inventory | ConvertTo-Json -Depth 8) + [Environment]::NewLine)

    $forming = Join-Path $OutputDirectory "RETURN-VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R011.forming.zip"
    if (Test-Path -LiteralPath $forming) {
        Remove-Item -LiteralPath $forming -Force
    }
    Compress-Archive -Path (Join-Path $CheckpointRoot "*") -DestinationPath $forming -CompressionLevel Optimal
    $returnSha = Get-LowerSha256 $forming
    $final = Join-Path $OutputDirectory ("RETURN-VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R011--sha256-" + $returnSha + ".zip")
    if (Test-Path -LiteralPath $final) {
        Remove-Item -LiteralPath $final -Force
    }
    Move-Item -LiteralPath $forming -Destination $final

    Write-Host ""
    Write-Host "VEXLIFE_WINDOWS_4B_PATIENT_ZERO_R011_RETURN"
    Write-Host ("path=" + $final)
    Write-Host ("sha256=" + $returnSha)
    Write-Host ("disposition=" + $Disposition)
}

if ($env:OS -ne "Windows_NT") {
    throw "WINDOWS_4B_PATIENT_ZERO_R011_REQUIRES_WINDOWS"
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$Timestamp = [DateTimeOffset]::UtcNow.ToString("yyyyMMddTHHmmssZ")
$CheckpointRoot = Join-Path $OutputDirectory ("VEXLIFE-WINDOWS-4B-PATIENT-ZERO-R011-CHECKPOINTS-" + $Timestamp)
New-Item -ItemType Directory -Path $CheckpointRoot | Out-Null

$CacheRoot = Join-Path $OutputDirectory "VEXLIFE-WINDOWS-4B-PATIENT-ZERO-CACHE"
New-Item -ItemType Directory -Path $CacheRoot -Force | Out-Null
$TechnicalHome = Join-Path $OutputDirectory "VEXLIFE-WINDOWS-4B-PATIENT-ZERO-HOME"

$failure = $null
$disposition = "SAFE_FAILURE"
$priorJavaHome = $env:JAVA_HOME
$priorPath = $env:Path
$priorGradleOpts = $env:GRADLE_OPTS

try {
    Write-Host ""
    Write-Host "VexLife Windows 4B patient-zero R011"
    Write-Host "Local Relay execution. No GitHub Actions."
    Write-Host ("Product source: " + $ProductSourceSha)
    Write-Host ("Technical Home: " + $TechnicalHome)
    Write-Host ("Real Home remains protected: " + (Join-Path $HOME ".vexlife"))
    Write-Host ""

    Write-Host "[1/7] Verify cached model and reusable Windows runtime dependency"
    $ModelPath = Join-Path $CacheRoot "model-Q4_K_M.gguf"
    if (-not (Test-Path -LiteralPath $ModelPath -PathType Leaf)) {
        Write-Host "Current 4B cache is absent; downloading ~2.8 GB once."
        $partial = $ModelPath + ".partial"
        if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial -Force }
        & curl.exe -L --fail --progress-bar -o $partial $ModelUrl
        if ($LASTEXITCODE -ne 0) { throw "CURRENT_4B_GGUF_DOWNLOAD_FAILED" }
        if ((Get-Item -LiteralPath $partial).Length -ne $ModelBytes) { throw "CURRENT_4B_GGUF_SIZE_MISMATCH" }
        if ((Get-LowerSha256 $partial) -ne $ModelSha256) { throw "CURRENT_4B_GGUF_HASH_MISMATCH" }
        Move-Item -LiteralPath $partial -Destination $ModelPath
    }
    if ((Get-Item -LiteralPath $ModelPath).Length -ne $ModelBytes -or (Get-LowerSha256 $ModelPath) -ne $ModelSha256) {
        throw "CACHED_4B_GGUF_NOT_EXACT"
    }

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

    Write-Checkpoint -Name "01-inputs.json" -Value ([ordered]@{
        stage = "EXACT_INPUTS_READY"
        modelSha256 = $ModelSha256
        modelBytes = $ModelBytes
        llamaServerSha256 = $observedLlamaSha
        predecessorModelAuthorityUsed = $false
        realHomeMutationPerformed = $false
    })
    Write-Host "PASS: exact current 4B + exact reusable llama.cpp binary ready"
    Write-Host ""

    Write-Host "[2/7] Materialize attempt-local Java 21"
    $JdkZip = Join-Path $CacheRoot "OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"
    if (-not (Test-Path -LiteralPath $JdkZip -PathType Leaf)) {
        $jdkPartial = $JdkZip + ".partial"
        if (Test-Path -LiteralPath $jdkPartial) { Remove-Item -LiteralPath $jdkPartial -Force }
        Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $jdkPartial
        if ((Get-LowerSha256 $jdkPartial) -ne $JdkSha256) { throw "JDK_SHA256_MISMATCH" }
        Move-Item -LiteralPath $jdkPartial -Destination $JdkZip
    } elseif ((Get-LowerSha256 $JdkZip) -ne $JdkSha256) {
        throw "CACHED_JDK_SHA256_MISMATCH"
    }

    $JdkRoot = Join-Path $env:TEMP "vexlife-win4b-r011-jdk-21.0.13"
    if (Test-Path -LiteralPath $JdkRoot) { Remove-Item -LiteralPath $JdkRoot -Recurse -Force }
    New-Item -ItemType Directory -Path $JdkRoot | Out-Null
    Expand-Archive -LiteralPath $JdkZip -DestinationPath $JdkRoot
    $jdkDirs = @(Get-ChildItem -LiteralPath $JdkRoot -Directory)
    if ($jdkDirs.Count -ne 1) { throw "JDK_ARCHIVE_ROOT_AMBIGUOUS" }
    $javaHome = $jdkDirs[0].FullName
    $java = Join-Path $javaHome "bin\java.exe"
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "JDK_JAVA_MISSING" }

    $env:JAVA_HOME = $javaHome
    $env:Path = (Join-Path $javaHome "bin") + ";" + $priorPath
    $env:GRADLE_OPTS = "-Dorg.gradle.jvmargs=-Xmx2048m -Dorg.gradle.workers.max=1 -Dorg.gradle.parallel=false -Dkotlin.compiler.execution.strategy=in-process"

    Write-Checkpoint -Name "02-java.json" -Value ([ordered]@{
        stage = "ATTEMPT_LOCAL_JAVA_READY"
        jdkSha256 = $JdkSha256
        gradleHeapLimit = "2048m"
        maxWorkers = 1
        kotlinCompiler = "in-process"
        persistentGlobalInstall = $false
    })
    Write-Host "PASS: attempt-local Java ready"
    Write-Host ""

    Write-Host "[3/7] Acquire exact Product source and compile topology contract locally"
    $SourceZip = Join-Path $CacheRoot ("VexLife-Prod-" + $ProductSourceSha + ".zip")
    if (-not (Test-Path -LiteralPath $SourceZip -PathType Leaf)) {
        Invoke-WebRequest -UseBasicParsing -Uri $SourceZipUrl -OutFile $SourceZip
    }
    $sourceArchiveSha = Get-LowerSha256 $SourceZip
    $SourceRoot = Join-Path $env:TEMP ("vexlife-prod-r011-" + $ProductSourceSha.Substring(0,12))
    if (Test-Path -LiteralPath $SourceRoot) { Remove-Item -LiteralPath $SourceRoot -Recurse -Force }
    New-Item -ItemType Directory -Path $SourceRoot | Out-Null
    Expand-Archive -LiteralPath $SourceZip -DestinationPath $SourceRoot
    $roots = @(Get-ChildItem -LiteralPath $SourceRoot -Directory)
    if ($roots.Count -ne 1) { throw "SOURCE_ARCHIVE_ROOT_AMBIGUOUS" }
    $RepoRoot = $roots[0].FullName
    if (-not (Test-Path -LiteralPath (Join-Path $RepoRoot "gradlew.bat") -PathType Leaf)) {
        throw "SOURCE_GRADLE_WRAPPER_MISSING"
    }

    Write-Checkpoint -Name "03-source.json" -Value ([ordered]@{
        stage = "EXACT_SOURCE_READY"
        productSourceSha = $ProductSourceSha
        archiveSha256 = $sourceArchiveSha
        sourceUrl = $SourceZipUrl
        localCompile = $true
        githubActionsUsed = $false
    })

    $StringsLog = Join-Path $CheckpointRoot "gradle-strings-check.log"
    Invoke-Gradle -Label "STRING_MANIFEST_CHECK" -LogPath $StringsLog -Arguments @(
        "--no-daemon"
        "--max-workers=1"
        "checkVexLifeStrings"
    )

    Write-Checkpoint -Name "04-strings.json" -Value ([ordered]@{
        stage = "PORTABLE_STRING_MANIFEST_PASS"
        productSourceSha = $ProductSourceSha
        githubActionsUsed = $false
    })

    $CompileLog = Join-Path $CheckpointRoot "gradle-upstream-family-tests.log"
    Invoke-Gradle -Label "UPSTREAM_FAMILY_REALIZATION_TESTS" -LogPath $CompileLog -Arguments @(
        "--no-daemon"
        "--max-workers=1"
        "jvmTest"
        "--tests"
        '"vexlife.entry.VexLifeProdEntryFamilyTest"'
        "--tests"
        '"vexlife.model.VexLifeModelsFamilyTest"'
        "--tests"
        '"vexlife.model.VexLifeModelsTest"'
        "--tests"
        '"vexlife.model.heretic.HereticModelCatalogTest"'
        "--tests"
        '"vexlife.model.heretic.HereticProductModelTest"'
        "--tests"
        '"vexlife.model.heretic.HereticRuntimeRealizationTest"'
        "--tests"
        '"vexlife.model.heretic.HereticRuntimeRealizationSequenceTest"'
        "--tests"
        '"vexlife.model.heretic.HereticRuntimeRealizationArchitectureTest"'
        "--tests"
        '"vexlife.model.heretic.HereticRuntimeAvailabilityTest"'
        "--tests"
        '"vexlife.model.heretic.HereticRuntimeContractTest"'
        "--tests"
        '"vexlife.companion.DirectCompanionConnectionTest"'
        "--tests"
        '"vexlife.companion.DirectCompanionTurnTest"'
        "--tests"
        '"vexlife.local.VexLifeProdLocalSetupTest"'
        "--tests"
        '"vexlife.service.VexLifeProdServiceProcessTest"'
        "--tests"
        '"vexlife.setup.VexLifeProdSetupModelTest"'
        "--tests"
        '"vexlife.setup.VexLifeProdSetupLanguageTest"'
        "--tests"
        '"vexlife.setup.VexLifeProdMacFrontDoorTest"'
        "--tests"
        '"vexlife.setup.VexLifeProdSetupProcessTest"'
        "--tests"
        '"vexlife.web.VexLifeWebP1RuntimeArtifactsTest"'
    )

    Write-Checkpoint -Name "05-upstream-family-tests.json" -Value ([ordered]@{
        stage = "UPSTREAM_FAMILY_REALIZATION_TESTS_PASS"
        productSourceSha = $ProductSourceSha
        tests = @(
            "VexLifeProdEntryFamilyTest"
            "VexLifeModelsFamilyTest"
            "VexLifeModelsTest"
            "HereticModelCatalogTest"
            "HereticProductModelTest"
            "HereticRuntimeRealizationTest"
            "HereticRuntimeRealizationSequenceTest"
            "HereticRuntimeRealizationArchitectureTest"
            "HereticRuntimeAvailabilityTest"
            "HereticRuntimeContractTest"
            "DirectCompanionConnectionTest"
            "DirectCompanionTurnTest"
            "VexLifeProdLocalSetupTest"
            "VexLifeProdServiceProcessTest"
            "VexLifeProdSetupModelTest"
            "VexLifeProdSetupLanguageTest"
            "VexLifeProdMacFrontDoorTest"
            "VexLifeProdSetupProcessTest"
            "VexLifeWebP1RuntimeArtifactsTest"
        )
        fullJvmSuiteOnWindowsRequired = $false
        githubActionsUsed = $false
    })
    Write-Host ""

    Write-Host "[4/7] Assemble local VexLife patient-zero backend bundle"
    $BuildLog = Join-Path $CheckpointRoot "gradle-bundle.log"
    Invoke-Gradle -Label "ASSEMBLE_PATIENT_ZERO_BUNDLE" -LogPath $BuildLog -Arguments @(
        "--no-daemon"
        "--max-workers=1"
        "assembleWindows4BPatientZeroBundle"
    )

    $AppRoot = Join-Path $RepoRoot "build\windows-4b-patient-zero\bundle"
    if (-not (Test-Path -LiteralPath (Join-Path $AppRoot "vexlife-prod.jar") -PathType Leaf)) {
        throw "LOCAL_PATIENT_ZERO_JAR_MISSING"
    }
    $libCount = @(Get-ChildItem -LiteralPath (Join-Path $AppRoot "lib") -Filter "*.jar" -File).Count
    if ($libCount -lt 1) { throw "LOCAL_PATIENT_ZERO_RUNTIME_LIBS_MISSING" }

    Write-Checkpoint -Name "05-bundle.json" -Value ([ordered]@{
        stage = "LOCAL_PATIENT_ZERO_BUNDLE_READY"
        appJarSha256 = (Get-LowerSha256 (Join-Path $AppRoot "vexlife-prod.jar"))
        runtimeLibraryCount = $libCount
        productSourceSha = $ProductSourceSha
        githubActionsUsed = $false
    })
    Write-Host "PASS: local backend bundle ready"
    Write-Host ""

    Write-Host "[5/7] Run mapped Windows runtime + shared Product service + restart continuity"
    Write-Host "Qualification profile now comes from the realization topology:"
    Write-Host "  maxTokens=1 | temperature=0 | enableThinking=false | stream=false"
    Write-Host ("Technical Home: " + $TechnicalHome)

    $Classpath = (Join-Path $AppRoot "vexlife-prod.jar") + ";" + (Join-Path $AppRoot "lib\*")
    $ProcessLog = Join-Path $CheckpointRoot "vexlife-patient-zero-process.log"
    $javaArgs = @(
        "-cp"
        $Classpath
        "vexlife.platform.windows.Windows4BPatientZeroProcessKt"
        "--source-sha"
        $ProductSourceSha
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

    if ($javaExit -ne 0) { throw ("PATIENT_ZERO_JAVA_EXIT_" + $javaExit) }
    if (-not (Test-Path -LiteralPath (Join-Path $CheckpointRoot "09-terminal.json") -PathType Leaf)) {
        throw "PATIENT_ZERO_TERMINAL_CHECKPOINT_MISSING"
    }

    Write-Host "PASS: Windows 4B backend end-to-end lifecycle completed"
    Write-Host ""

    Write-Host "[6/7] Verify technical Home retained explicit realization-bound Product model"
    $bindingPath = Join-Path $TechnicalHome "runtime\vexlife-prod\model-binding.json"
    if (-not (Test-Path -LiteralPath $bindingPath -PathType Leaf)) {
        throw "PRODUCT_MODEL_BINDING_MISSING_AFTER_PASS"
    }
    $binding = Get-Content -LiteralPath $bindingPath -Raw | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace([string]$binding.realizationRef)) {
        throw "PRODUCT_MODEL_BINDING_HAS_NO_REALIZATION_REF"
    }
    Write-Checkpoint -Name "11-binding-topology.json" -Value ([ordered]@{
        stage = "PRODUCT_BINDING_CARRIES_EXPLICIT_REALIZATION"
        modelRef = [string]$binding.modelRef
        realizationRef = [string]$binding.realizationRef
        runtimeReceiptPath = [string]$binding.runtimeReceiptPath
        automaticFallback = [bool]$binding.automaticFallback
        automaticDownload = [bool]$binding.automaticDownload
        automaticActivation = [bool]$binding.automaticActivation
    })
    Write-Host ("PASS: Product binding realization=" + [string]$binding.realizationRef)
    Write-Host ""

    $disposition = "PASS"
    Write-Host "[7/7] Form checkpointed RETURN"
} catch {
    $failure = $_.Exception.Message
    Write-Host ""
    Write-Host ("SAFE_FAILURE: " + $failure)
    try {
        Write-Checkpoint -Name "99-harness-failure.json" -Value ([ordered]@{
            stage = "R011_SAFE_FAILURE"
            failure = $failure
            productSourceSha = $ProductSourceSha
            githubActionsUsed = $false
            realHomeTouched = $false
            technicalHome = $TechnicalHome
        })
    } catch {
    }
} finally {
    $env:JAVA_HOME = $priorJavaHome
    $env:Path = $priorPath
    $env:GRADLE_OPTS = $priorGradleOpts
    try {
        Form-Return -Disposition $disposition -Failure $failure
    } catch {
        Write-Host ("RETURN_FORMATION_FAILED: " + $_.Exception.Message)
    }
}

if ($disposition -ne "PASS") {
    exit 2
}
