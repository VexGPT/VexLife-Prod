[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$CandidateHead = '5bb095247e6dc0470d1609e4cfc9908ce8a5e753'
$CandidateTree = '6714c2db9b1bd3d79da89b5a4469e2e5b966e006'
$CandidateBranch = 'VXG-111826-chatgpt-windows-setup-experience-a001'
$JdkUrl = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
$JdkSha256 = 'a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9'

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

function Invoke-GradleChecked {
    param([string]$Gradle, [string[]]$Arguments, [string]$FailureCode)
    $PriorPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $Output = @(& $Gradle @Arguments 2>&1)
        $ExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $PriorPreference
    }
    $Output | ForEach-Object { Write-Host ([string]$_) }
    if ($ExitCode -ne 0) {
        throw ($FailureCode + ':exit=' + $ExitCode)
    }
}

$BootstrapRoot = Join-Path $env:TEMP ('VXW-292-A004-' + $CandidateHead.Substring(0,8) + '-' + $PID)
if (Test-Path -LiteralPath $BootstrapRoot) {
    throw "BOOTSTRAP_ROOT_ALREADY_EXISTS:$BootstrapRoot"
}
New-Item -ItemType Directory -Path $BootstrapRoot | Out-Null

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

& $Gh auth status --hostname github.com
if ($LASTEXITCODE -ne 0) { throw 'GITHUB_LOGIN_REQUIRED' }

$ProductRoot = Join-Path $BootstrapRoot 'p'
Write-Host 'VexLife Windows setup A004: acquiring the exact corrected Product candidate...'
& $Gh repo clone 'VexGPT/VexLife-Prod' $ProductRoot -- --no-checkout --origin origin
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CLONE_FAILED' }

& $Git -C $ProductRoot fetch --no-tags --depth=64 origin $CandidateBranch
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CANDIDATE_FETCH_FAILED' }

$ObservedHead = (& $Git -C $ProductRoot rev-parse FETCH_HEAD).Trim()
if ($ObservedHead -ne $CandidateHead) {
    throw "PRODUCT_CANDIDATE_HEAD_MISMATCH:$ObservedHead"
}

& $Git -C $ProductRoot checkout --detach $CandidateHead
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CANDIDATE_CHECKOUT_FAILED' }

$ObservedTree = (& $Git -C $ProductRoot rev-parse 'HEAD^{tree}').Trim()
if ($ObservedTree -ne $CandidateTree) {
    throw "PRODUCT_CANDIDATE_TREE_MISMATCH:$ObservedTree"
}

$Status = @(& $Git -C $ProductRoot status --porcelain=v1 --untracked-files=all)
if ($LASTEXITCODE -ne 0 -or @($Status | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }).Count -ne 0) {
    throw 'PRODUCT_CANDIDATE_NOT_CLEAN'
}

Write-Host "Exact source: $CandidateHead"
Write-Host "Exact tree:   $CandidateTree"

$CachedJdk = Join-Path $HOME 'Downloads\VEXLIFE-WINDOWS-4B-PATIENT-ZERO-CACHE\OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
$JdkZip = $null
if (Test-Path -LiteralPath $CachedJdk -PathType Leaf) {
    $CachedSha = (Get-FileHash -LiteralPath $CachedJdk -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($CachedSha -eq $JdkSha256) {
        $JdkZip = $CachedJdk
        Write-Host 'Reusing the exact cached Temurin JDK 21.'
    } else {
        throw "CACHED_JDK_SHA256_MISMATCH:$CachedSha"
    }
}
if (-not $JdkZip) {
    $JdkZip = Join-Path $BootstrapRoot 'jdk21.zip'
    Write-Host 'Acquiring the exact pinned Temurin JDK 21...'
    Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $JdkZip
    $DownloadedSha = (Get-FileHash -LiteralPath $JdkZip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($DownloadedSha -ne $JdkSha256) {
        throw "DOWNLOADED_JDK_SHA256_MISMATCH:$DownloadedSha"
    }
}

$JdkExtract = Join-Path $BootstrapRoot 'j'
New-Item -ItemType Directory -Path $JdkExtract | Out-Null
Expand-Archive -LiteralPath $JdkZip -DestinationPath $JdkExtract
$JdkRoots = @(Get-ChildItem -LiteralPath $JdkExtract -Directory)
if ($JdkRoots.Count -ne 1) { throw 'JDK_ARCHIVE_ROOT_AMBIGUOUS' }

$JavaHome = $JdkRoots[0].FullName
$Java = Join-Path $JavaHome 'bin\java.exe'
$Jpackage = Join-Path $JavaHome 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $Java -PathType Leaf) -or
    -not (Test-Path -LiteralPath $Jpackage -PathType Leaf)) {
    throw 'JDK21_PACKAGING_TOOLS_MISSING'
}

$PriorJavaHome = $env:JAVA_HOME
$PriorPath = $env:Path
$PriorGradleOpts = $env:GRADLE_OPTS
$env:JAVA_HOME = $JavaHome
$env:Path = (Join-Path $JavaHome 'bin') + ';' + $PriorPath
$env:GRADLE_OPTS = '-Dorg.gradle.jvmargs=-Xmx2048m -Dorg.gradle.workers.max=1 -Dorg.gradle.parallel=false -Dkotlin.compiler.execution.strategy=in-process'

$Success = $false
try {
    $Gradle = Join-Path $ProductRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $Gradle -PathType Leaf)) {
        throw 'GRADLE_WRAPPER_MISSING'
    }

    Write-Host ''
    Write-Host '[1/3] Checking keyed string source/manifest...'
    Invoke-GradleChecked -Gradle $Gradle -Arguments @(
        '--no-daemon',
        '--max-workers=1',
        '-p', $ProductRoot,
        'checkVexLifeStrings'
    ) -FailureCode 'WINDOWS_SETUP_STRINGS_FAILED'

    Write-Host ''
    Write-Host '[2/3] Proving runtime string loading + Windows setup entry...'
    Invoke-GradleChecked -Gradle $Gradle -Arguments @(
        '--no-daemon',
        '--max-workers=1',
        '-p', $ProductRoot,
        'jvmTest',
        '--tests', 'vexlife.text.VexLifeStringsTest',
        '--tests', 'vexlife.platform.windows.VexLifeProdWindowsPreviewTest',
        '--tests', 'vexlife.platform.windows.VexLifeProdWindowsSetupTest'
    ) -FailureCode 'WINDOWS_SETUP_RUNTIME_CATALOG_PROOF_FAILED'

    $Builder = Join-Path $ProductRoot 'tools\windows-preview\FORM-WINDOWS-PREVIEW.ps1'
    if (-not (Test-Path -LiteralPath $Builder -PathType Leaf)) {
        throw 'WINDOWS_PREVIEW_BUILDER_MISSING'
    }

    Write-Host ''
    Write-Host '[3/3] Forming the self-contained Windows app image...'
    $BuilderArgs = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', $Builder,
        '-OutputDirectory', (Join-Path $HOME 'Downloads')
    )

    $PriorPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $BuilderOutput = @(& $PowerShellExe @BuilderArgs 2>&1)
        $BuilderExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $PriorPreference
    }

    $BuilderOutput | ForEach-Object { Write-Host ([string]$_) }

    if ($BuilderExit -ne 0) {
        $ExpectedDiagnostic = Join-Path (Join-Path $HOME 'Downloads') ('VexLife-Windows-preview-diagnostic-' + $CandidateHead.Substring(0,12) + '.log')
        if (Test-Path -LiteralPath $ExpectedDiagnostic -PathType Leaf) {
            Write-Host ''
            Write-Host '================ PACKAGING DIAGNOSTIC ================'
            Get-Content -LiteralPath $ExpectedDiagnostic | ForEach-Object { Write-Host ([string]$_) }
            Write-Host '======================================================'
            Write-Host "PACKAGING_DIAGNOSTIC=$ExpectedDiagnostic"
        }
        throw "WINDOWS_SETUP_APP_IMAGE_FORMATION_FAILED:exit=$BuilderExit"
    }

    $SourcePrefix = 'VEXLIFE_WINDOWS_PREVIEW_SOURCE='
    $TreePrefix = 'VEXLIFE_WINDOWS_PREVIEW_TREE='
    $ZipPrefix = 'VEXLIFE_WINDOWS_PREVIEW_ZIP='
    $ShaPrefix = 'VEXLIFE_WINDOWS_PREVIEW_ZIP_SHA256='

    $SourceLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith($SourcePrefix) })[-1]
    $TreeLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith($TreePrefix) })[-1]
    $ZipLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith($ZipPrefix) })[-1]
    $ShaLine = @($BuilderOutput | Where-Object { ([string]$_).StartsWith($ShaPrefix) })[-1]

    if (-not $SourceLine -or -not $TreeLine -or -not $ZipLine -or -not $ShaLine) {
        throw 'WINDOWS_SETUP_APP_IMAGE_POINTERS_MISSING'
    }

    $BuiltSource = ([string]$SourceLine).Substring($SourcePrefix.Length)
    $BuiltTree = ([string]$TreeLine).Substring($TreePrefix.Length)
    if ($BuiltSource -ne $CandidateHead) {
        throw "WINDOWS_SETUP_BUILT_SOURCE_MISMATCH:$BuiltSource"
    }
    if ($BuiltTree -ne $CandidateTree) {
        throw "WINDOWS_SETUP_BUILT_TREE_MISMATCH:$BuiltTree"
    }

    $PreviewZip = [IO.Path]::GetFullPath(([string]$ZipLine).Substring($ZipPrefix.Length))
    $PreviewSha = ([string]$ShaLine).Substring($ShaPrefix.Length).ToLowerInvariant()
    if (-not (Test-Path -LiteralPath $PreviewZip -PathType Leaf)) {
        throw "WINDOWS_SETUP_PREVIEW_ZIP_MISSING:$PreviewZip"
    }
    $ObservedZipSha = (Get-FileHash -LiteralPath $PreviewZip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($ObservedZipSha -ne $PreviewSha) {
        throw "WINDOWS_SETUP_PREVIEW_ZIP_SHA256_MISMATCH:$ObservedZipSha"
    }

    $ExtractName =
        'VexLife-Windows-setup-' +
        $CandidateHead.Substring(0,8) +
        '-' +
        $PreviewSha.Substring(0,12)
    $ExtractRoot = Join-Path (Join-Path $HOME 'Downloads') $ExtractName
    if (Test-Path -LiteralPath $ExtractRoot) {
        throw "WINDOWS_SETUP_PREVIEW_EXTRACT_TARGET_ALREADY_EXISTS:$ExtractRoot"
    }

    Expand-Archive -LiteralPath $PreviewZip -DestinationPath $ExtractRoot
    $AppRoot = Join-Path $ExtractRoot 'VexLife'
    $PreviewExe = Join-Path $AppRoot 'VexLife.exe'
    $DebugExe = Join-Path $AppRoot 'VexLife-Debug.exe'
    $ReceiptPath = Join-Path $AppRoot 'windows-lived-preview.receipt.json'

    foreach ($Required in @($PreviewExe, $DebugExe, $ReceiptPath)) {
        if (-not (Test-Path -LiteralPath $Required -PathType Leaf)) {
            throw "WINDOWS_SETUP_PREVIEW_REQUIRED_FILE_MISSING:$Required"
        }
    }

    $Receipt = Get-Content -LiteralPath $ReceiptPath -Raw | ConvertFrom-Json
    if ($Receipt.sourceSha -ne $CandidateHead -or $Receipt.sourceTree -ne $CandidateTree) {
        throw 'WINDOWS_SETUP_PREVIEW_RECEIPT_SOURCE_MISMATCH'
    }
    if ($Receipt.setupJourney -ne 'WINDOWS_NATIVE_PRODUCT_ENTRY_PREVIEW' -or
        $Receipt.diagnosticEntrypoint -ne 'VexLife-Debug.exe') {
        throw 'WINDOWS_SETUP_PREVIEW_RECEIPT_DIAGNOSTIC_BINDING_MISMATCH'
    }

    $StartupLog = Join-Path $env:LOCALAPPDATA 'VexLife\logs\windows-startup.log'
    if (Test-Path -LiteralPath $StartupLog -PathType Leaf) {
        Remove-Item -LiteralPath $StartupLog -Force
    }

    Write-Host ''
    Write-Host '============================================================'
    Write-Host 'VexLife Windows Setup Journey preview is formed and verified.'
    Write-Host "ZIP:    $PreviewZip"
    Write-Host "SHA256: $PreviewSha"
    Write-Host "EXE:    $PreviewExe"
    Write-Host "DEBUG:  $DebugExe"
    Write-Host "LOG:    $StartupLog"
    Write-Host '============================================================'
    Write-Host ''
    Write-Host 'Launching ordinary VexLife.exe...'

    $Process = Start-Process -FilePath $PreviewExe -WorkingDirectory $AppRoot -PassThru
    Start-Sleep -Seconds 5
    $Process.Refresh()

    if ($Process.HasExited) {
        Write-Host "VEXLIFE_ORDINARY_LAUNCH_EXITED_EARLY=$($Process.ExitCode)"
        if (Test-Path -LiteralPath $StartupLog -PathType Leaf) {
            Write-Host ''
            Write-Host '================ STARTUP LOG ================='
            Get-Content -LiteralPath $StartupLog | ForEach-Object { Write-Host ([string]$_) }
            Write-Host '=============================================='
        }
        Write-Host ''
        Write-Host 'Ordinary launch exited early; opening VexLife-Debug.exe for a visible console...'
        Start-Process -FilePath $DebugExe -WorkingDirectory $AppRoot | Out-Null
    } else {
        Write-Host "VEXLIFE_ORDINARY_LAUNCH_PID=$($Process.Id)"
        Write-Host 'Ordinary VexLife.exe is still running after the startup observation window.'
    }

    $Success = $true
} finally {
    $env:JAVA_HOME = $PriorJavaHome
    $env:Path = $PriorPath
    $env:GRADLE_OPTS = $PriorGradleOpts
    try {
        Remove-Item -LiteralPath $BootstrapRoot -Recurse -Force
    } catch {
        Write-Host "Temporary build cleanup was non-terminal: $BootstrapRoot"
    }
}

if (-not $Success) { exit 1 }

Write-Host ''
Write-Host 'The exact digest-bound ZIP remains in Downloads.'
