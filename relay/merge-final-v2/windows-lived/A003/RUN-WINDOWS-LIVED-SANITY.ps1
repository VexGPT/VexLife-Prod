[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$CandidateHead = '46a02c314e3cfd42798be88d811abd443c363cc8'
$CandidateTree = 'e2c61f40db38de295d3742b91b94bd71b37592f5'
$CandidateBranch = 'VXG-111826-chatgpt-merge-final-v2-a001'
$ExpectedMain = 'a58777106b08bd58c167608fa0115729185ae3dd'
$Repository = 'VexGPT/VexLife-Prod'
$Attempt = 'V2-WINDOWS-LIVED-A003'
$ReturnStem = 'RETURN-VEXLIFE-MERGE-FINAL-V2-WINDOWS-LIVED-A003'

$JdkUrl = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
$JdkSha256 = 'a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9'

$ModelSha256 = '7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159'
$ModelBytes = 2790000800
$RuntimeArchiveSha256 = '52133a0a5a8f6035b1bdd2f89c3425ea8b742413d9bdb9a2dee30e3a1681b18c'
$RuntimeArchiveBytes = 18213827
$RuntimeExeSha256 = 'af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7'

$HomePath = [IO.Path]::GetFullPath((Join-Path $HOME '.vexlife'))
$ModelPath = Join-Path $HomePath 'models\heretic\qwen3.8-4b-distill-gguf\c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61\model-Q4_K_M.gguf'
$RuntimeRoot = Join-Path $HomePath 'runtime\vexlife-prod\llama-cpp\b10107\windows-x64-cpu'
$RuntimeArchive = Join-Path $RuntimeRoot 'archive\llama-b10107-bin-win-cpu-x64.zip'
$RuntimeExe = Join-Path $RuntimeRoot 'image\llama-server.exe'
$Chronology = Join-Path $HomePath 'runtime\vexlife-prod\logs\chronology\chronology.jsonl'

$Downloads = Join-Path $HOME 'Downloads'
$BootstrapRoot = Join-Path $env:TEMP ('VXW-V2-LIVED-' + $CandidateHead.Substring(0,8) + '-' + $PID)
$Evidence = Join-Path $BootstrapRoot 'evidence'
$ProductRoot = Join-Path $BootstrapRoot 'product'
$PreviewOutput = Join-Path $Downloads ('VexLife-Windows-V2-' + $CandidateHead.Substring(0,12))
$ExtractRoot = Join-Path $BootstrapRoot 'preview'

$Success = $false
$TurnProof = 'NOT_REACHED'
$ReopenProof = 'NOT_REACHED'
$RuntimeReuse = 'NOT_PROVED'
$StartedAt = (Get-Date).ToUniversalTime().ToString('o')
$CurrentStage = 'INIT'
$FailureMessage = ''
$TranscriptStarted = $false
$TranscriptPath = Join-Path $Evidence 'powershell-transcript.txt'
$RunLog = Join-Path $Evidence 'run.log'

function Set-Stage {
    param([string]$Name)
    $script:CurrentStage = $Name
    $line = ((Get-Date).ToUniversalTime().ToString('o') + [char]9 + $Name)
    Write-Host ('STAGE=' + $Name)
    try { Add-Content -LiteralPath $RunLog -Value $line -Encoding UTF8 } catch {}
}

function Capture-FailureContext {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)

    $script:FailureMessage = $ErrorRecord.Exception.Message
    $Body = @"
stage=$CurrentStage
exceptionType=$($ErrorRecord.Exception.GetType().FullName)
message=$($ErrorRecord.Exception.Message)
category=$($ErrorRecord.CategoryInfo)
fullyQualifiedErrorId=$($ErrorRecord.FullyQualifiedErrorId)

invocation:
$($ErrorRecord.InvocationInfo.PositionMessage)

scriptStackTrace:
$($ErrorRecord.ScriptStackTrace)

fullError:
$($ErrorRecord | Out-String)
"@
    try { $Body | Set-Content -LiteralPath (Join-Path $Evidence 'failure.txt') -Encoding UTF8 } catch {}

    try {
        if (Test-Path -LiteralPath $Chronology -PathType Leaf) {
            Get-Content -LiteralPath $Chronology | Select-Object -Last 500 | Set-Content -LiteralPath (Join-Path $Evidence 'chronology-tail.jsonl') -Encoding UTF8
        }
    } catch {}

    try {
        $FailureRoot = Join-Path $HomePath 'recovery\direct-companion'
        if (Test-Path -LiteralPath $FailureRoot -PathType Container) {
            $Out = Join-Path $Evidence 'direct-companion-failures'
            New-Item -ItemType Directory -Path $Out -Force | Out-Null
            Get-ChildItem -LiteralPath $FailureRoot -File -Filter '*.json' | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 12 | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $Out $_.Name) -Force }
        }
    } catch {}

    foreach ($Pair in @(
        @('source-currentness','http://127.0.0.1:18210/api/v1/source-currentness'),
        @('current-state','http://127.0.0.1:18210/api/v1/current-state'),
        @('models','http://127.0.0.1:18210/api/v1/models'),
        @('settings','http://127.0.0.1:18210/api/v1/settings'),
        @('local-asr','http://127.0.0.1:18210/api/v1/local-asr')
    )) {
        try {
            $Name = [string]$Pair[0]
            $Url = [string]$Pair[1]
            $Value = Invoke-WebRequest -UseBasicParsing -Uri $Url -Method Get -TimeoutSec 5
            [IO.File]::WriteAllText((Join-Path $Evidence ($Name + '.failure.json')), [string]$Value.Content, (New-Object System.Text.UTF8Encoding($false)))
        } catch {}
    }
}

function Resolve-Executable {
    param([string[]]$Candidates, [string]$CommandName)
    foreach ($Candidate in $Candidates) {
        if ($Candidate -and (Test-Path -LiteralPath $Candidate -PathType Leaf)) {
            return [IO.Path]::GetFullPath($Candidate)
        }
    }
    $Command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($Command) { return [IO.Path]::GetFullPath($Command.Source) }
    throw "REQUIRED_EXECUTABLE_UNAVAILABLE:$CommandName"
}

function Write-Text {
    param([string]$Path, [string]$Value)
    $Value | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Save-Json {
    param([string]$Path, $Value)
    $Value | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Get-Json {
    param([string]$Url)
    return Invoke-RestMethod -UseBasicParsing -Uri $Url -Method Get -TimeoutSec 10
}

function Wait-Service {
    param([int]$Seconds = 180)
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $value = Get-Json 'http://127.0.0.1:18210/api/v1/source-currentness'
            if ($value) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-ServiceStopped {
    param([int]$Seconds = 90)
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $null = Get-Json 'http://127.0.0.1:18210/api/v1/source-currentness'
        } catch {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Capture-Service {
    param([string]$Suffix)
    $source = Get-Json 'http://127.0.0.1:18210/api/v1/source-currentness'
    $current = Get-Json 'http://127.0.0.1:18210/api/v1/current-state'
    $models = Get-Json 'http://127.0.0.1:18210/api/v1/models'
    $settings = Get-Json 'http://127.0.0.1:18210/api/v1/settings'
    $localAsr = Get-Json 'http://127.0.0.1:18210/api/v1/local-asr'
    Save-Json (Join-Path $Evidence ("source-currentness.$Suffix.json")) $source
    Save-Json (Join-Path $Evidence ("current-state.$Suffix.json")) $current
    Save-Json (Join-Path $Evidence ("models.$Suffix.json")) $models
    Save-Json (Join-Path $Evidence ("settings.$Suffix.json")) $settings
    Save-Json (Join-Path $Evidence ("local-asr.$Suffix.json")) $localAsr
    return [pscustomobject]@{
        Source = $source
        Current = $current
        Models = $models
        Settings = $settings
        LocalAsr = $localAsr
    }
}

function Assert-Hash {
    param([string]$Path, [string]$ExpectedSha256, [Nullable[long]]$ExpectedBytes, [string]$FailureCode)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw ($FailureCode + '_MISSING:' + $Path)
    }
    if ($null -ne $ExpectedBytes) {
        $bytes = (Get-Item -LiteralPath $Path).Length
        if ($bytes -ne [long]$ExpectedBytes) {
            throw ($FailureCode + '_BYTES:' + $bytes)
        }
    }
    $sha = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($sha -ne $ExpectedSha256) {
        throw ($FailureCode + '_SHA256:' + $sha)
    }
    return $sha
}

function Write-Summary {
    @"
attempt=$Attempt
candidateHead=$CandidateHead
candidateTree=$CandidateTree
expectedMain=$ExpectedMain
startedAt=$StartedAt
runtimeReuse=$RuntimeReuse
turnProof=$TurnProof
reopenProof=$ReopenProof
success=$([int]$Success)
stage=$CurrentStage
failureMessage=$FailureMessage
"@ | Set-Content -LiteralPath (Join-Path $Evidence 'summary.txt') -Encoding UTF8
}

function Finalize-Return {
    param([int]$ExitCode)
    if ($TranscriptStarted) { try { Stop-Transcript | Out-Null } catch {}; $script:TranscriptStarted = $false }
    try { Write-Summary } catch {}
    Write-Text (Join-Path $Evidence 'exit-code.txt') ([string]$ExitCode)

    $HashLines = @()
    Get-ChildItem -LiteralPath $Evidence -File -Recurse |
        Where-Object { $_.Name -ne 'SHA256SUMS.txt' } |
        Sort-Object FullName |
        ForEach-Object {
            $sha = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            $relative = $_.FullName.Substring($Evidence.Length).TrimStart('\') -replace '\\','/'
            $HashLines += ($sha + '  ./' + $relative)
        }
    [IO.File]::WriteAllLines((Join-Path $Evidence 'SHA256SUMS.txt'), [string[]]$HashLines, [Text.Encoding]::ASCII)

    $TempZip = Join-Path $Downloads ($ReturnStem + '.zip')
    if (Test-Path -LiteralPath $TempZip) { Remove-Item -LiteralPath $TempZip -Force }
    Compress-Archive -LiteralPath $Evidence -DestinationPath $TempZip -CompressionLevel Optimal

    $Outer = (Get-FileHash -LiteralPath $TempZip -Algorithm SHA256).Hash.ToLowerInvariant()
    $FinalZip = Join-Path $Downloads ($ReturnStem + '--sha256-' + $Outer + '.zip')
    if (Test-Path -LiteralPath $FinalZip) { Remove-Item -LiteralPath $FinalZip -Force }
    Move-Item -LiteralPath $TempZip -Destination $FinalZip

    Write-Host ''
    Write-Host '============================================================'
    if ($Success) { Write-Host 'WINDOWS_LIVED_DISPOSITION=PASS' } else { Write-Host 'WINDOWS_LIVED_DISPOSITION=SAFE_FAILURE' }
    Write-Host "RETURN_ZIP=$FinalZip"
    Write-Host "RETURN_SHA256=$Outer"
    Write-Host '============================================================'
}

if (Test-Path -LiteralPath $BootstrapRoot) {
    throw "BOOTSTRAP_ROOT_ALREADY_EXISTS:$BootstrapRoot"
}
New-Item -ItemType Directory -Path $Evidence -Force | Out-Null
New-Item -ItemType Directory -Path $PreviewOutput -Force | Out-Null
'' | Set-Content -LiteralPath $RunLog -Encoding UTF8
try { Start-Transcript -Path $TranscriptPath -Force | Out-Null; $TranscriptStarted = $true } catch { $_ | Out-String | Set-Content -LiteralPath (Join-Path $Evidence 'transcript-start.error.txt') -Encoding UTF8 }
Set-Stage 'HOST_TOOLS'

$Gh = Resolve-Executable -Candidates @(
    'C:\Program Files\GitHub CLI\gh.exe',
    'C:\Program Files (x86)\GitHub CLI\gh.exe'
) -CommandName 'gh'
$Git = Resolve-Executable -Candidates @(
    'C:\Program Files\Git\cmd\git.exe',
    'C:\Program Files\Git\bin\git.exe'
) -CommandName 'git'
$PowerShellExe = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
if (-not (Test-Path -LiteralPath $PowerShellExe -PathType Leaf)) {
    throw 'WINDOWS_POWERSHELL_REQUIRED'
}

$ExitCode = 1
try {
    Set-Stage 'CURRENTNESS'
    & $Gh auth status --hostname github.com | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'GITHUB_LOGIN_REQUIRED' }

    $ObservedMain = (& $Gh api "repos/$Repository/commits/main" --jq '.sha').Trim()
    Write-Text (Join-Path $Evidence 'observed-main.txt') $ObservedMain
    if ($ObservedMain -ne $ExpectedMain) { throw "ORIGIN_MAIN_MOVED:$ObservedMain" }

    $ObservedBranch = (& $Gh api "repos/$Repository/branches/$CandidateBranch" --jq '.commit.sha').Trim()
    Write-Text (Join-Path $Evidence 'observed-candidate-head.txt') $ObservedBranch
    if ($ObservedBranch -ne $CandidateHead) { throw "CANDIDATE_MOVED:$ObservedBranch" }

    Write-Host 'VexLife merge-final V2 Windows lived sanity'
    Write-Host "Candidate: $CandidateHead"
    Write-Host "Tree:      $CandidateTree"
    Write-Host ''

    Set-Stage 'SOURCE_CHECKOUT'
    Write-Host 'Acquiring exact Product source...'
    & $Gh repo clone $Repository $ProductRoot -- --no-checkout --origin origin
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CLONE_FAILED' }
    & $Git -C $ProductRoot fetch --no-tags --depth=256 origin $CandidateBranch main
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_FETCH_FAILED' }

    $FetchedHead = (& $Git -C $ProductRoot rev-parse ("refs/remotes/origin/" + $CandidateBranch)).Trim()
    if ($FetchedHead -ne $CandidateHead) { throw "FETCHED_CANDIDATE_MISMATCH:$FetchedHead" }
    $FetchedMain = (& $Git -C $ProductRoot rev-parse 'refs/remotes/origin/main').Trim()
    if ($FetchedMain -ne $ExpectedMain) { throw "FETCHED_MAIN_MOVED:$FetchedMain" }

    & $Git -C $ProductRoot checkout --detach $CandidateHead
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CHECKOUT_FAILED' }
    $ObservedTree = (& $Git -C $ProductRoot rev-parse 'HEAD^{tree}').Trim()
    if ($ObservedTree -ne $CandidateTree) { throw "PRODUCT_TREE_MISMATCH:$ObservedTree" }
    Write-Text (Join-Path $Evidence 'source-head.txt') $CandidateHead
    Write-Text (Join-Path $Evidence 'source-tree.txt') $ObservedTree

    Set-Stage 'PREEXISTING_RUNTIME'
    Write-Host 'Checking existing Windows Home/runtime bytes (no Product acquisition is authorized)...'
    if (-not (Test-Path -LiteralPath $HomePath -PathType Container)) {
        throw "WINDOWS_HOME_MISSING:$HomePath"
    }

    $ObservedModelSha = Assert-Hash -Path $ModelPath -ExpectedSha256 $ModelSha256 -ExpectedBytes ([long]$ModelBytes) -FailureCode 'WINDOWS_4B_MODEL'
    $ObservedArchiveSha = Assert-Hash -Path $RuntimeArchive -ExpectedSha256 $RuntimeArchiveSha256 -ExpectedBytes ([long]$RuntimeArchiveBytes) -FailureCode 'WINDOWS_LLAMA_ARCHIVE'
    $ObservedExeSha = Assert-Hash -Path $RuntimeExe -ExpectedSha256 $RuntimeExeSha256 -ExpectedBytes $null -FailureCode 'WINDOWS_LLAMA_EXE'
    @"
home=$HomePath
model=$ModelPath
modelSha256=$ObservedModelSha
runtimeArchive=$RuntimeArchive
runtimeArchiveSha256=$ObservedArchiveSha
runtimeExe=$RuntimeExe
runtimeExeSha256=$ObservedExeSha
"@ | Set-Content -LiteralPath (Join-Path $Evidence 'preexisting-runtime.txt') -Encoding UTF8
    $RuntimeReuse = 'EXACT_PREEXISTING_MODEL_RUNTIME_BYTES_PRESENT'

    $CachedJdk = Join-Path $Downloads 'VEXLIFE-WINDOWS-4B-PATIENT-ZERO-CACHE\OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
    $JdkZip = $null
    if (Test-Path -LiteralPath $CachedJdk -PathType Leaf) {
        $CachedSha = (Get-FileHash -LiteralPath $CachedJdk -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($CachedSha -ne $JdkSha256) { throw "CACHED_JDK_SHA256_MISMATCH:$CachedSha" }
        $JdkZip = $CachedJdk
        Write-Text (Join-Path $Evidence 'jdk-source.txt') 'REUSED_EXACT_CACHE'
        Write-Host 'Reusing exact cached Temurin JDK 21.'
    }
    if (-not $JdkZip) {
        $JdkZip = Join-Path $BootstrapRoot 'jdk21.zip'
        Write-Host 'Exact JDK cache absent; downloading pinned Temurin JDK 21 for app-image formation only...'
        Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $JdkZip
        $DownloadedSha = (Get-FileHash -LiteralPath $JdkZip -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($DownloadedSha -ne $JdkSha256) { throw "DOWNLOADED_JDK_SHA256_MISMATCH:$DownloadedSha" }
        Write-Text (Join-Path $Evidence 'jdk-source.txt') 'DOWNLOADED_PINNED_BUILD_JDK'
    }

    $JdkExtract = Join-Path $BootstrapRoot 'jdk'
    New-Item -ItemType Directory -Path $JdkExtract | Out-Null
    Expand-Archive -LiteralPath $JdkZip -DestinationPath $JdkExtract
    $JdkRoots = @(Get-ChildItem -LiteralPath $JdkExtract -Directory)
    if ($JdkRoots.Count -ne 1) { throw 'JDK_ARCHIVE_ROOT_AMBIGUOUS' }
    $JavaHome = $JdkRoots[0].FullName

    $PriorJavaHome = $env:JAVA_HOME
    $PriorPath = $env:Path
    $PriorGradleOpts = $env:GRADLE_OPTS
    $env:JAVA_HOME = $JavaHome
    $env:Path = (Join-Path $JavaHome 'bin') + ';' + $PriorPath
    $env:GRADLE_OPTS = '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dorg.gradle.parallel=false -Dkotlin.compiler.execution.strategy=in-process'

    try {
        $Builder = Join-Path $ProductRoot 'tools\windows-preview\FORM-WINDOWS-PREVIEW.ps1'
        if (-not (Test-Path -LiteralPath $Builder -PathType Leaf)) { throw 'WINDOWS_PREVIEW_BUILDER_MISSING' }

        Write-Host ''
        Set-Stage 'WINDOWS_APP_FORMATION'
        Write-Host 'Forming exact self-contained Windows VexLife app image...'
        $BuilderOutputPath = Join-Path $Evidence 'builder.log'
        $PriorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            $BuilderOutput = @(& $PowerShellExe -NoProfile -ExecutionPolicy Bypass -File $Builder -OutputDirectory $PreviewOutput 2>&1)
            $BuilderExit = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $PriorPreference
        }
        $BuilderOutput | Set-Content -LiteralPath $BuilderOutputPath -Encoding UTF8
        $BuilderOutput | ForEach-Object { Write-Host ([string]$_) }
        if ($BuilderExit -ne 0) { throw "WINDOWS_PREVIEW_FORMATION_FAILED:exit=$BuilderExit" }

        $SourceLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_SOURCE=') })[-1]
        $TreeLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_TREE=') })[-1]
        $ZipLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_ZIP=') })[-1]
        $ShaLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_ZIP_SHA256=') })[-1]
        if (-not $SourceLine -or -not $TreeLine -or -not $ZipLine -or -not $ShaLine) {
            throw 'WINDOWS_PREVIEW_POINTERS_MISSING'
        }
        $BuiltSource = ([string]$SourceLine).Substring('VEXLIFE_WINDOWS_PREVIEW_SOURCE='.Length)
        $BuiltTree = ([string]$TreeLine).Substring('VEXLIFE_WINDOWS_PREVIEW_TREE='.Length)
        $PreviewZip = [IO.Path]::GetFullPath(([string]$ZipLine).Substring('VEXLIFE_WINDOWS_PREVIEW_ZIP='.Length))
        $PreviewSha = ([string]$ShaLine).Substring('VEXLIFE_WINDOWS_PREVIEW_ZIP_SHA256='.Length).ToLowerInvariant()
        if ($BuiltSource -ne $CandidateHead -or $BuiltTree -ne $CandidateTree) { throw 'WINDOWS_PREVIEW_SOURCE_MISMATCH' }
        if (-not (Test-Path -LiteralPath $PreviewZip -PathType Leaf)) { throw 'WINDOWS_PREVIEW_ZIP_MISSING' }
        $ObservedPreviewSha = (Get-FileHash -LiteralPath $PreviewZip -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($ObservedPreviewSha -ne $PreviewSha) { throw "WINDOWS_PREVIEW_ZIP_SHA_MISMATCH:$ObservedPreviewSha" }
        @"
zip=$PreviewZip
sha256=$PreviewSha
source=$BuiltSource
tree=$BuiltTree
"@ | Set-Content -LiteralPath (Join-Path $Evidence 'preview-artifact.txt') -Encoding UTF8

        New-Item -ItemType Directory -Path $ExtractRoot | Out-Null
        Expand-Archive -LiteralPath $PreviewZip -DestinationPath $ExtractRoot
        $AppRoot = Join-Path $ExtractRoot 'VexLife'
        $PreviewExe = Join-Path $AppRoot 'VexLife.exe'
        $ReceiptPath = Join-Path $AppRoot 'windows-lived-preview.receipt.json'
        if (-not (Test-Path -LiteralPath $PreviewExe -PathType Leaf)) { throw 'WINDOWS_PREVIEW_EXE_MISSING' }
        if (-not (Test-Path -LiteralPath $ReceiptPath -PathType Leaf)) { throw 'WINDOWS_PREVIEW_RECEIPT_MISSING' }
        $Receipt = Get-Content -LiteralPath $ReceiptPath -Raw | ConvertFrom-Json
        if ($Receipt.sourceSha -ne $CandidateHead -or $Receipt.sourceTree -ne $CandidateTree) {
            throw 'WINDOWS_PREVIEW_RECEIPT_SOURCE_MISMATCH'
        }

        $ObservedMainAfterBuild = (& $Gh api "repos/$Repository/commits/main" --jq '.sha').Trim()
        if ($ObservedMainAfterBuild -ne $ExpectedMain) { throw "ORIGIN_MAIN_MOVED_AFTER_BUILD:$ObservedMainAfterBuild" }

        $BaselineLines = 0
        if (Test-Path -LiteralPath $Chronology -PathType Leaf) {
            $BaselineLines = @(Get-Content -LiteralPath $Chronology).Count
        }
        Write-Text (Join-Path $Evidence 'chronology-baseline-lines.txt') ([string]$BaselineLines)

        Write-Host ''
        Set-Stage 'PRODUCT_LAUNCH'
        Write-Host 'Launching exact VexLife.exe...'
        $Process = Start-Process -FilePath $PreviewExe -WorkingDirectory $AppRoot -PassThru

        Write-Host ''
        Write-Host '================ WINDOWS LIVED CHECK ================'
        Write-Host 'Use the native VexLife window normally.'
        Write-Host 'Preserve the existing Vex Home.'
        Write-Host ''
        Write-Host 'IMPORTANT: this sanity does NOT authorize model/runtime acquisition.'
        Write-Host 'If the UI asks you to download/setup new model bytes, return here and type HELD.'
        Write-Host 'If it offers Start/Open using the existing exact 4B realization, proceed.'
        Write-Host 'When the browser Product is open and usable, return here.'
        Write-Host '======================================================'
        $Entry = Read-Host 'Type PASS when Product is open, or HELD if it requests acquisition / errors'
        if ($Entry -ne 'PASS') { throw "HUMAN_ENTRY_HELD:$Entry" }

        if (-not (Wait-Service -Seconds 180)) { throw 'WINDOWS_PRODUCT_SERVICE_NOT_REACHABLE' }
        Set-Stage 'PRODUCT_STATE'
        $First = Capture-Service 'first'

        if ($First.Source.sourceSha -ne $CandidateHead) { throw "WINDOWS_SOURCE_MISMATCH:$($First.Source.sourceSha)" }
        if ([IO.Path]::GetFullPath([string]$First.Current.homePath) -ne $HomePath) { throw "WINDOWS_HOME_CHANGED:$($First.Current.homePath)" }
        if ([string]$First.Current.platform -ne 'windows') { throw "WINDOWS_PLATFORM_PROJECTION:$($First.Current.platform)" }
        if ([string]$First.Models.selectedHandle -ne 'heretic-4b') { throw "WINDOWS_SELECTED_MODEL:$($First.Models.selectedHandle)" }
        if ([string]$First.Models.servingHandle -ne 'heretic-4b') { throw "WINDOWS_SERVING_MODEL:$($First.Models.servingHandle)" }
        if ([bool]$First.Models.switchPending) { throw 'WINDOWS_MODEL_SWITCH_PENDING' }
        if ([string]$First.Current.runtimeEndpoint -ne 'http://127.0.0.1:18080') { throw ('WINDOWS_RUNTIME_ENDPOINT:' + [string]$First.Current.runtimeEndpoint) }
        if ([string]$First.Current.runtimeRealizationRef -ne 'realization.vexlife-prod.heretic-4b.windows-x64.llama-cpp.cpu.b10107.001') { throw ('WINDOWS_RUNTIME_REALIZATION:' + [string]$First.Current.runtimeRealizationRef) }
        $LanguageCodes = @($First.Settings.languages | ForEach-Object { [string]$_.code })
        if ($LanguageCodes -notcontains 'zh-Hans' -or $LanguageCodes -notcontains 'zh-Hant') {
            throw 'WINDOWS_LANGUAGE_PROJECTION_MISSING'
        }
        $RuntimeReuse = 'EXACT_PREEXISTING_BYTES_PLUS_RUNNING_HERETIC_4B'
        if (-not (Test-Path -LiteralPath $Chronology -PathType Leaf)) { throw 'WINDOWS_VEXLOGGER_CHRONOLOGY_NOT_BOUND' }

        Write-Host ''
        Set-Stage 'REAL_TURN'
        Write-Host '================ REAL TURN CHECK ===================='
        Write-Host 'Send ONE ordinary message to Vex and wait for the full reply.'
        Write-Host 'Do not switch models.'
        Write-Host '======================================================'
        $TurnHuman = Read-Host 'Type PASS after the reply is complete, or HELD if the turn failed'
        if ($TurnHuman -ne 'PASS') { throw "HUMAN_TURN_HELD:$TurnHuman" }

        $TurnDeadline = (Get-Date).AddMinutes(10)
        $Completed = $false
        while ((Get-Date) -lt $TurnDeadline) {
            if (Test-Path -LiteralPath $Chronology -PathType Leaf) {
                $NewLines = @(Get-Content -LiteralPath $Chronology | Select-Object -Skip $BaselineLines)
                $NewLines | Set-Content -LiteralPath (Join-Path $Evidence 'chronology-new.jsonl') -Encoding UTF8
                if (@($NewLines | Select-String -SimpleMatch '"fact":"TURN_COMPLETED"').Count -gt 0) {
                    $Completed = $true
                    break
                }
                if (@($NewLines | Select-String -SimpleMatch '"fact":"TURN_FAILED"').Count -gt 0) {
                    throw 'WINDOWS_TURN_FAILED_FACT_OBSERVED'
                }
            }
            Start-Sleep -Seconds 2
        }
        if (-not $Completed) { throw 'WINDOWS_TURN_COMPLETED_FACT_NOT_OBSERVED' }
        $TurnProof = 'TURN_COMPLETED_OBSERVED'

        $AfterTurn = Capture-Service 'after-turn'
        if ($AfterTurn.Source.sourceSha -ne $CandidateHead) { throw 'WINDOWS_AFTER_TURN_SOURCE_CHANGED' }
        if ([string]$AfterTurn.Models.servingHandle -ne 'heretic-4b' -or [bool]$AfterTurn.Models.switchPending) {
            throw 'WINDOWS_AFTER_TURN_MODEL_NOT_CURRENT'
        }

        Write-Host ''
        Set-Stage 'NATIVE_CLOSE'
        Write-Host 'Close the native VexLife window normally now. Leave the browser tab alone.'
        $null = Read-Host 'Press Enter after the native VexLife window is closed'

        if (-not (Wait-ServiceStopped -Seconds 90)) { throw 'WINDOWS_PREVIEW_SERVICE_DID_NOT_STOP_WITH_EXE' }
        Write-Text (Join-Path $Evidence 'service-stop-after-close.txt') 'STOPPED'

        Set-Stage 'REOPEN'
        Write-Host 'Reopening the same exact VexLife.exe...'
        $Process2 = Start-Process -FilePath $PreviewExe -WorkingDirectory $AppRoot -PassThru
        if (-not (Wait-Service -Seconds 180)) { throw 'WINDOWS_REOPEN_SERVICE_NOT_REACHABLE' }

        Write-Host ''
        Write-Host '================ REOPEN CHECK ======================='
        Write-Host 'Confirm VexLife is usable, the prior Conversation is visible in Archives,'
        Write-Host 'and it did not require a different Home/model or new download.'
        Write-Host '======================================================'
        $ReopenHuman = Read-Host 'Type PASS when confirmed, or HELD if not'
        if ($ReopenHuman -ne 'PASS') { throw "HUMAN_REOPEN_HELD:$ReopenHuman" }

        $Reopen = Capture-Service 'reopen'
        if ($Reopen.Source.sourceSha -ne $CandidateHead) { throw 'WINDOWS_REOPEN_SOURCE_MISMATCH' }
        if ([IO.Path]::GetFullPath([string]$Reopen.Current.homePath) -ne $HomePath) { throw 'WINDOWS_REOPEN_HOME_MISMATCH' }
        if ([string]$Reopen.Models.selectedHandle -ne 'heretic-4b' -or [string]$Reopen.Models.servingHandle -ne 'heretic-4b') {
            throw 'WINDOWS_REOPEN_MODEL_MISMATCH'
        }
        if ([bool]$Reopen.Models.switchPending) { throw 'WINDOWS_REOPEN_MODEL_SWITCH_PENDING' }

        $ReopenProof = 'EXACT_SOURCE_SAME_HOME_HERETIC_4B_ARCHIVE_VISIBLE'
        @"
entryUsable=yes
oneRealConversationTurnCompleted=yes
serviceStoppedWithNativeExe=yes
reopenUsable=yes
priorConversationVisibleAfterReopen=yes
sameHome=$HomePath
sameServingModel=heretic-4b
candidateSource=$CandidateHead
"@ | Set-Content -LiteralPath (Join-Path $Evidence 'human-confirmation.txt') -Encoding UTF8

        Write-Host ''
        Write-Host 'Final observations captured. Close the native VexLife window normally.'
        $null = Read-Host 'Press Enter after the native window is closed'

        Set-Stage 'COMPLETE'
        $Success = $true
        $ExitCode = 0
    } finally {
        $env:JAVA_HOME = $PriorJavaHome
        $env:Path = $PriorPath
        $env:GRADLE_OPTS = $PriorGradleOpts
    }
} catch {
    $Failure = $_
    Write-Host ''
    Write-Host ('SAFE_FAILURE=' + $Failure.Exception.Message)
    Capture-FailureContext -ErrorRecord $Failure
    $ExitCode = 1
} finally {
    Finalize-Return -ExitCode $ExitCode
    try { Remove-Item -LiteralPath $BootstrapRoot -Recurse -Force } catch {}
}

exit $ExitCode
