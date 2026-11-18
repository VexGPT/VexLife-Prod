[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'

function Resolve-Executable {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Candidates,
        [Parameter(Mandatory = $true)]
        [string]$CommandName
    )

    foreach ($Candidate in $Candidates) {
        if ($Candidate -and (Test-Path -LiteralPath $Candidate -PathType Leaf)) {
            return [IO.Path]::GetFullPath($Candidate)
        }
    }

    $Command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($Command) {
        return [IO.Path]::GetFullPath($Command.Source)
    }

    throw "REQUIRED_EXECUTABLE_UNAVAILABLE:$CommandName"
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Executable,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$FailureCode
    )

    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ($FailureCode + ':exit=' + $LASTEXITCODE)
    }
}

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepositoryRoot = [IO.Path]::GetFullPath((Join-Path $ScriptRoot '..\..'))
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)

$Git = Resolve-Executable -Candidates @(
    'C:\Program Files\Git\cmd\git.exe',
    'C:\Program Files\Git\bin\git.exe'
) -CommandName 'git'

$Gradle = Join-Path $RepositoryRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $Gradle -PathType Leaf)) {
    throw 'GRADLE_WRAPPER_UNAVAILABLE'
}

$SourceSha = (& $Git -C $RepositoryRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $SourceSha -notmatch '^[0-9a-f]{40}$') {
    throw "SOURCE_SHA_UNRESOLVED:$SourceSha"
}

$SourceTree = (& $Git -C $RepositoryRoot rev-parse 'HEAD^{tree}').Trim()
if ($LASTEXITCODE -ne 0 -or $SourceTree -notmatch '^[0-9a-f]{40}$') {
    throw "SOURCE_TREE_UNRESOLVED:$SourceTree"
}

$Status = @(& $Git -C $RepositoryRoot status --porcelain=v1 --untracked-files=all)
if ($LASTEXITCODE -ne 0) {
    throw 'SOURCE_STATUS_UNAVAILABLE'
}
if (@($Status | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }).Count -ne 0) {
    throw 'WINDOWS_PREVIEW_REQUIRES_CLEAN_EXACT_SOURCE'
}

if (-not [Environment]::Is64BitOperatingSystem) {
    throw 'WINDOWS_X64_REQUIRED'
}
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'WINDOWS_PREVIEW_MUST_BE_FORMED_ON_WINDOWS'
}

$JavaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw 'JAVA_HOME_REQUIRED'
}
$JavaHome = [IO.Path]::GetFullPath($JavaHome)

$Java = Join-Path $JavaHome 'bin\java.exe'
$Jdeps = Join-Path $JavaHome 'bin\jdeps.exe'
$Jlink = Join-Path $JavaHome 'bin\jlink.exe'
$Jpackage = Join-Path $JavaHome 'bin\jpackage.exe'
foreach ($Required in @($Java, $Jdeps, $Jlink, $Jpackage)) {
    if (-not (Test-Path -LiteralPath $Required -PathType Leaf)) {
        throw "JDK21_PACKAGING_TOOL_MISSING:$Required"
    }
}

$PriorNativeErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $JavaVersion = @(& $Java -version 2>&1) -join [Environment]::NewLine
    $JavaVersionExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $PriorNativeErrorActionPreference
}
if ($JavaVersionExit -ne 0 -or $JavaVersion -notmatch 'version "21(?:\.|")') {
    throw "JDK21_REQUIRED:$JavaVersion"
}

$WorkRoot = Join-Path $RepositoryRoot 'build\windows-lived-preview'
$InputDir = Join-Path $WorkRoot 'input'
$RuntimeDir = Join-Path $WorkRoot 'runtime'
$ImageDest = Join-Path $WorkRoot 'image'
$ZipStage = Join-Path $WorkRoot 'zip'
$InitScript = Join-Path $RepositoryRoot 'tools\windows-preview\stage-preview.init.gradle'

if (Test-Path -LiteralPath $WorkRoot) {
    Remove-Item -LiteralPath $WorkRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $InputDir -Force | Out-Null
New-Item -ItemType Directory -Path $ImageDest -Force | Out-Null
New-Item -ItemType Directory -Path $ZipStage -Force | Out-Null
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$DiagnosticPath = Join-Path $OutputDirectory ('VexLife-Windows-preview-diagnostic-' + $SourceSha.Substring(0,12) + '.log')
$DiagnosticLines = New-Object System.Collections.Generic.List[string]
function Add-Diagnostic {
    param([string]$Line)
    $DiagnosticLines.Add($Line)
}
function Flush-Diagnostic {
    $DiagnosticLines | Set-Content -LiteralPath $DiagnosticPath -Encoding UTF8
}

function Invoke-NativeCaptured {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Stage,
        [Parameter(Mandatory = $true)]
        [string]$Executable,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$FailureCode
    )

    Add-Diagnostic "stage=$Stage"
    Add-Diagnostic ("executable=" + $Executable)
    Add-Diagnostic ("arguments=" + ($Arguments -join ' '))
    Flush-Diagnostic

    $PriorPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $Captured = @(& $Executable @Arguments 2>&1)
        $ExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $PriorPreference
    }

    $Captured | ForEach-Object { Write-Host ([string]$_) }
    Add-Diagnostic ("exit=" + $ExitCode)
    $Captured | ForEach-Object { Add-Diagnostic ("output=" + [string]$_) }
    Flush-Diagnostic

    if ($ExitCode -ne 0) {
        throw ($FailureCode + ':exit=' + $ExitCode + ' diagnostic=' + $DiagnosticPath)
    }

    return @($Captured)
}
Add-Diagnostic "sourceSha=$SourceSha"
Add-Diagnostic "sourceTree=$SourceTree"
Add-Diagnostic "repositoryRoot=$RepositoryRoot"
Add-Diagnostic "callerCwd=$([Environment]::CurrentDirectory)"
Add-Diagnostic "powershellLocation=$((Get-Location).Path)"
Add-Diagnostic "javaHome=$JavaHome"
Add-Diagnostic "java=$Java"
Add-Diagnostic "jdeps=$Jdeps"
Add-Diagnostic "jlink=$Jlink"
Add-Diagnostic "jpackage=$Jpackage"
Add-Diagnostic "gradle=$Gradle"
Add-Diagnostic "initScript=$InitScript"
Add-Diagnostic "outputDirectory=$OutputDirectory"
Add-Diagnostic "javaVersionExit=$JavaVersionExit"
Add-Diagnostic "javaVersion=$($JavaVersion -replace '[\r\n]+',' | ')"
Flush-Diagnostic

Write-Host "VexLife Windows preview: compiling exact Product source $SourceSha ..."
$GradleArgs = @(
    '--no-daemon',
    '--max-workers=1',
    '-p',
    $RepositoryRoot,
    '-I',
    $InitScript,
    "-PvexlifeWindowsPreviewInputDir=$InputDir",
    'stageVexLifeWindowsPreviewInputs'
)
Add-Diagnostic "stage=INPUT_STAGE"
Add-Diagnostic ("gradleArgs=" + ($GradleArgs -join ' '))
Flush-Diagnostic
$PriorStageErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $StageOutput = & $Gradle @GradleArgs 2>&1
    $StageExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $PriorStageErrorActionPreference
}
$StageOutput | ForEach-Object { Write-Host ([string]$_) }
Add-Diagnostic "stageExit=$StageExit"
$StageOutput | ForEach-Object { Add-Diagnostic ("stageOutput=" + [string]$_) }
Flush-Diagnostic
if ($StageExit -ne 0) {
    throw "WINDOWS_PREVIEW_INPUT_STAGE_FAILED:exit=$StageExit diagnostic=$DiagnosticPath"
}

$MainJarPointer = Join-Path $InputDir 'vexlife-main-jar.txt'
if (-not (Test-Path -LiteralPath $MainJarPointer -PathType Leaf)) {
    throw 'WINDOWS_PREVIEW_MAIN_JAR_POINTER_MISSING'
}
$MainJarName = (Get-Content -LiteralPath $MainJarPointer -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($MainJarName) -or [IO.Path]::GetFileName($MainJarName) -ne $MainJarName) {
    throw "WINDOWS_PREVIEW_MAIN_JAR_POINTER_INVALID:$MainJarName"
}
$MainJar = Join-Path $InputDir $MainJarName
if (-not (Test-Path -LiteralPath $MainJar -PathType Leaf)) {
    throw "WINDOWS_PREVIEW_MAIN_JAR_MISSING:$MainJar"
}
Remove-Item -LiteralPath $MainJarPointer -Force

$DependencyJars =
    @(Get-ChildItem -LiteralPath $InputDir -File -Filter '*.jar' |
        Where-Object { $_.Name -ne $MainJarName } |
        Sort-Object Name)
if ($DependencyJars.Count -eq 0) {
    throw 'WINDOWS_PREVIEW_RUNTIME_DEPENDENCIES_EMPTY'
}

$Classpath = ($DependencyJars | ForEach-Object { $_.FullName }) -join ';'
$JdepsArgs = @(
    '--multi-release',
    '21',
    '--print-module-deps',
    '--class-path',
    $Classpath,
    $MainJar
)
$JdepsOutput = Invoke-NativeCaptured -Stage 'JDEPS' -Executable $Jdeps -Arguments $JdepsArgs -FailureCode 'WINDOWS_PREVIEW_JDEPS_FAILED'
$DerivedModules =
    @(
        $JdepsOutput |
            ForEach-Object { ([string]$_).Trim() } |
            Where-Object { $_ -match '^[A-Za-z0-9_.]+(?:,[A-Za-z0-9_.]+)*$' } |
            Select-Object -Last 1
    )
if ($DerivedModules.Count -ne 1) {
    throw 'WINDOWS_PREVIEW_JDEPS_MODULE_SET_MISSING'
}

$Modules =
    @(
        ($DerivedModules[0] -split ',') +
        @('java.desktop', 'java.net.http', 'jdk.httpserver', 'jdk.crypto.ec') |
            Sort-Object -Unique
    )
$ModuleSet = $Modules -join ','

Write-Host "VexLife Windows preview: forming private Java 21 runtime ..."
$JlinkArgs = @(
    '--add-modules',
    $ModuleSet,
    '--output',
    $RuntimeDir,
    '--strip-debug',
    '--no-header-files',
    '--no-man-pages'
)
Invoke-NativeCaptured -Stage 'JLINK' -Executable $Jlink -Arguments $JlinkArgs -FailureCode 'WINDOWS_PREVIEW_JLINK_FAILED' | Out-Null

$RuntimeJava = Join-Path $RuntimeDir 'bin\java.exe'
if (-not (Test-Path -LiteralPath $RuntimeJava -PathType Leaf)) {
    throw 'WINDOWS_PREVIEW_PRIVATE_RUNTIME_JAVA_MISSING'
}

Write-Host 'VexLife Windows preview: forming VexLife.exe app image ...'
$JpackageArgs = @(
    '--type',
    'app-image',
    '--name',
    'VexLife',
    '--input',
    $InputDir,
    '--main-jar',
    $MainJarName,
    '--main-class',
    'vexlife.platform.windows.VexLifeProdWindowsPreviewKt',
    '--runtime-image',
    $RuntimeDir,
    '--dest',
    $ImageDest,
    '--vendor',
    'VexGPT',
    '--description',
    'VexLife Windows setup preview',
    '--java-options',
    "-Dvexlife.prod.sourceSha=$SourceSha"
)
Invoke-NativeCaptured -Stage 'JPACKAGE' -Executable $Jpackage -Arguments $JpackageArgs -FailureCode 'WINDOWS_PREVIEW_JPACKAGE_FAILED' | Out-Null

$AppImage = Join-Path $ImageDest 'VexLife'
$AppExe = Join-Path $AppImage 'VexLife.exe'
if (-not (Test-Path -LiteralPath $AppExe -PathType Leaf)) {
    throw "WINDOWS_PREVIEW_EXE_MISSING:$AppExe"
}

$Receipt = [ordered]@{
    schemaVersion = 'vexlife-prod.windows-lived-preview-receipt/v1'
    truthClass = 'SOURCE_BOUND_UNSIGNED_WINDOWS_LIVED_PREVIEW'
    repository = 'VexGPT/VexLife-Prod'
    sourceSha = $SourceSha
    sourceTree = $SourceTree
    platform = 'windows-x64'
    javaRuntimeMajor = 21
    applicationName = 'VexLife'
    entrypoint = 'VexLife.exe'
    productOrigin = 'http://127.0.0.1:18210'
    defaultHome = '~/.vexlife'
    homeOverrideEnvironment = 'VEXLIFE_HOME'
    installer = $false
    windowsServiceInstalled = $false
    autoStartConfigured = $false
    signed = $false
    setupJourney = 'WINDOWS_NATIVE_PRODUCT_ENTRY_PREVIEW'
    productLifetime = 'EXE_OWNS_SERVICE_AND_RUNTIME_WHILE_OPEN'
    safeCancelDuringActiveSetup = $false
    windowsSpecificLocalization = 'ENGLISH_PREVIEW'
    livedAcceptance = 'PENDING_VICTOR_WINDOWS_RUN'
    runtimeModules = $Modules
}
$ReceiptPath = Join-Path $AppImage 'windows-lived-preview.receipt.json'
$Receipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $ReceiptPath -Encoding UTF8

$HumanReadme = @"
VexLife Windows lived preview

This is a source-bound pre-merge preview, not a final installer.

Run:
  VexLife.exe

Default Vex Home:
  %USERPROFILE%\.vexlife

The app opens the native Windows VexLife Setup Journey first. It preserves and inspects the selected
Vex Home, keeps Home creation separate from local-model/runtime setup, then starts the shared Product
service and opens the canonical browser presentation.

Keep the VexLife window open while using the browser Product. After setup is idle, closing the window
stops this preview's Product service and local model.

This build does not install a Windows Service, configure auto-start, or claim code signing. Active
setup/download work is not presented as safely cancellable in this preview. Lived acceptance remains
pending until this exact artifact is exercised on Victor's Windows machine.

Source:
  $SourceSha
Tree:
  $SourceTree
"@
Set-Content -LiteralPath (Join-Path $AppImage 'README-WINDOWS-PREVIEW.txt') -Value $HumanReadme -Encoding UTF8

$CurrentSourceSha = (& $Git -C $RepositoryRoot rev-parse HEAD).Trim()
$CurrentTree = (& $Git -C $RepositoryRoot rev-parse 'HEAD^{tree}').Trim()
$CurrentStatus = @(& $Git -C $RepositoryRoot status --porcelain=v1 --untracked-files=all)
if ($LASTEXITCODE -ne 0) {
    throw 'SOURCE_RECHECK_FAILED'
}
if ($CurrentSourceSha -ne $SourceSha -or $CurrentTree -ne $SourceTree) {
    throw 'SOURCE_IDENTITY_CHANGED_DURING_WINDOWS_PREVIEW_FORMATION'
}
$UnexpectedStatus =
    @(
        $CurrentStatus |
            Where-Object {
                -not [string]::IsNullOrWhiteSpace([string]$_) -and
                ([string]$_) -notmatch '^\?\? build/'
            }
    )
if ($UnexpectedStatus.Count -ne 0) {
    throw 'SOURCE_CHANGED_DURING_WINDOWS_PREVIEW_FORMATION'
}

$TemporaryZip = Join-Path $ZipStage 'VexLife-Windows-x64-preview.zip'
Compress-Archive -LiteralPath $AppImage -DestinationPath $TemporaryZip -CompressionLevel Optimal

$ZipSha256 = (Get-FileHash -LiteralPath $TemporaryZip -Algorithm SHA256).Hash.ToLowerInvariant()
$ZipBytes = (Get-Item -LiteralPath $TemporaryZip).Length
$FinalName =
    'VexLife-Windows-x64-preview-' +
    $SourceSha.Substring(0, 12) +
    '--sha256-' +
    $ZipSha256 +
    '.zip'
$FinalZip = Join-Path $OutputDirectory $FinalName

if (Test-Path -LiteralPath $FinalZip -PathType Leaf) {
    $ExistingSha = (Get-FileHash -LiteralPath $FinalZip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($ExistingSha -ne $ZipSha256) {
        throw "WINDOWS_PREVIEW_OUTPUT_COLLISION:$FinalZip"
    }
} else {
    Copy-Item -LiteralPath $TemporaryZip -Destination $FinalZip
}

$CopiedSha = (Get-FileHash -LiteralPath $FinalZip -Algorithm SHA256).Hash.ToLowerInvariant()
if ($CopiedSha -ne $ZipSha256) {
    throw "WINDOWS_PREVIEW_OUTPUT_DIGEST_MISMATCH:$CopiedSha"
}

Write-Host ''
Write-Host "VEXLIFE_WINDOWS_PREVIEW_SOURCE=$SourceSha"
Write-Host "VEXLIFE_WINDOWS_PREVIEW_TREE=$SourceTree"
Write-Host "VEXLIFE_WINDOWS_PREVIEW_EXE=VexLife\VexLife.exe"
Write-Host "VEXLIFE_WINDOWS_PREVIEW_ZIP=$FinalZip"
Write-Host "VEXLIFE_WINDOWS_PREVIEW_ZIP_SHA256=$ZipSha256"
Add-Diagnostic "stage=COMPLETE"
Add-Diagnostic "zip=$FinalZip"
Add-Diagnostic "zipSha256=$ZipSha256"
Add-Diagnostic "zipBytes=$ZipBytes"
Flush-Diagnostic
Write-Host "VEXLIFE_WINDOWS_PREVIEW_DIAGNOSTIC=$DiagnosticPath"
Write-Host "VEXLIFE_WINDOWS_PREVIEW_ZIP_BYTES=$ZipBytes"
