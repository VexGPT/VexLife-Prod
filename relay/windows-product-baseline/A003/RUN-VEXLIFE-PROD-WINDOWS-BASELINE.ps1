# [VXG RealForever]
[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$OutputDirectory = "$HOME\Downloads"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$NL = [Environment]::NewLine

$SourceRepository = "VexGPT/VexLife-Prod"
$SourceCommit = "655e7c78cefba06a5e87afad285bb25a08632f05"
$SourceTree = "886c131da9e3c011ed88a6badad401430362b35c"
$Attempt = "A003"

$JdkVersion = "21.0.13+9"
$JdkAssetName = "OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"
$JdkSha256 = "a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9"
$JdkUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"

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

function Normalize-WindowsArchitecture {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $null }
    switch ($Value.Trim().ToUpperInvariant()) {
        "AMD64" { return "X86_64" }
        "X64" { return "X86_64" }
        "X86_64" { return "X86_64" }
        "ARM64" { return "ARM64" }
        "AARCH64" { return "ARM64" }
        default { return "UNKNOWN:" + $Value }
    }
}

function Invoke-GradleStep {
    param(
        [Parameter(Mandatory = $true)][string]$Task,
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$GradleWrapper,
        [Parameter(Mandatory = $true)][string]$LogPath
    )

    $started = [DateTimeOffset]::UtcNow
    $cmd = $env:ComSpec
    if ([string]::IsNullOrWhiteSpace($cmd) -or -not (Test-Path -LiteralPath $cmd -PathType Leaf)) {
        throw "WINDOWS_COMSPEC_UNAVAILABLE"
    }

    $line = '""' + $GradleWrapper + '" --no-daemon ' + $Task + '"'

    Push-Location $RepoRoot
    try {
        $output = & $cmd /d /s /c $line 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    $finished = [DateTimeOffset]::UtcNow

    $text = @(
        "task=$Task"
        "command=$cmd /d /s /c <source-managed-gradlew-command>"
        "startedAtUtc=$($started.ToString('o'))"
        "finishedAtUtc=$($finished.ToString('o'))"
        "exitCode=$exitCode"
        ""
        ($output | Out-String)
    ) -join $NL

    Write-Utf8NoBom -Path $LogPath -Text ($text + $NL)

    return [ordered]@{
        task = $Task
        exitCode = $exitCode
        startedAtUtc = $started.ToString("o")
        finishedAtUtc = $finished.ToString("o")
        log = [IO.Path]::GetFileName($LogPath)
    }
}

if ($env:OS -ne "Windows_NT") {
    throw "WINDOWS_BASELINE_REQUIRES_WINDOWS_NT"
}

$nativeArchitectureRaw =
    if (-not [string]::IsNullOrWhiteSpace($env:PROCESSOR_ARCHITEW6432)) {
        $env:PROCESSOR_ARCHITEW6432
    } else {
        $env:PROCESSOR_ARCHITECTURE
    }
$nativeArchitecture = Normalize-WindowsArchitecture $nativeArchitectureRaw
$processArchitecture = Normalize-WindowsArchitecture $env:PROCESSOR_ARCHITECTURE

if ($nativeArchitecture -ne "X86_64") {
    throw ("A003_PINNED_JDK_REQUIRES_WINDOWS_X86_64:" + $nativeArchitecture)
}

$capsule = Join-Path $env:TEMP ("vexlife-prod-windows-baseline-" + $Attempt.ToLowerInvariant() + "-" + $SourceCommit.Substring(0,12))
if (Test-Path -LiteralPath $capsule) {
    Remove-Item -LiteralPath $capsule -Recurse -Force
}
New-Item -ItemType Directory -Path $capsule | Out-Null

$logsRoot = Join-Path $capsule "logs"
$returnRoot = Join-Path $capsule "return"
$sourceRoot = Join-Path $capsule "source"
$gradleHome = Join-Path $capsule "gradle-home"
$jdkArchive = Join-Path $capsule $JdkAssetName
$jdkExtractRoot = Join-Path $capsule "jdk"

New-Item -ItemType Directory -Path $logsRoot | Out-Null
New-Item -ItemType Directory -Path $returnRoot | Out-Null
New-Item -ItemType Directory -Path $sourceRoot | Out-Null
New-Item -ItemType Directory -Path $gradleHome | Out-Null
New-Item -ItemType Directory -Path $jdkExtractRoot | Out-Null
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$env:GRADLE_USER_HOME = $gradleHome

$sourceZip = Join-Path $capsule "source.zip"
$sourceUrl = "https://github.com/VexGPT/VexLife-Prod/archive/$SourceCommit.zip"
$sourceDownloaded = $false
$sourceZipSha256 = $null
$jdkDownloaded = $false
$jdkObservedSha256 = $null
$jdkHome = $null
$javaVersion = $null
$javacVersion = $null
$repoRoot = $null
$steps = @()
$firstFailure = $null

try {
    Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $jdkArchive
    $jdkDownloaded = $true
    $jdkObservedSha256 = Get-LowerSha256 $jdkArchive
    if ($jdkObservedSha256 -ne $JdkSha256) {
        throw ("JDK_SHA256_MISMATCH expected=" + $JdkSha256 + " observed=" + $jdkObservedSha256)
    }

    Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkExtractRoot
    $jdkRoots = @(Get-ChildItem -LiteralPath $jdkExtractRoot -Directory)
    if ($jdkRoots.Count -ne 1) {
        throw "JDK_ARCHIVE_ROOT_AMBIGUOUS"
    }
    $jdkHome = $jdkRoots[0].FullName
    $javaExe = Join-Path $jdkHome "bin\java.exe"
    $javacExe = Join-Path $jdkHome "bin\javac.exe"
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        throw "JDK_JAVA_EXECUTABLE_MISSING"
    }
    if (-not (Test-Path -LiteralPath $javacExe -PathType Leaf)) {
        throw "JDK_JAVAC_EXECUTABLE_MISSING"
    }

    $env:JAVA_HOME = $jdkHome
    $env:PATH = (Join-Path $jdkHome "bin") + [IO.Path]::PathSeparator + $env:PATH

    $javaVersion = (& $javaExe -version 2>&1 | Out-String).Trim()
    $javacVersion = (& $javacExe -version 2>&1 | Out-String).Trim()

    Invoke-WebRequest -UseBasicParsing -Uri $sourceUrl -OutFile $sourceZip
    $sourceDownloaded = $true
    $sourceZipSha256 = Get-LowerSha256 $sourceZip

    Expand-Archive -LiteralPath $sourceZip -DestinationPath $sourceRoot
    $roots = @(Get-ChildItem -LiteralPath $sourceRoot -Directory)
    if ($roots.Count -ne 1) {
        throw "SOURCE_ARCHIVE_ROOT_AMBIGUOUS"
    }
    $repoRoot = $roots[0].FullName
    if (-not $roots[0].Name.EndsWith($SourceCommit)) {
        throw ("SOURCE_ARCHIVE_ROOT_IDENTITY_DRIFT:" + $roots[0].Name)
    }

    $gradlew = Join-Path $repoRoot "gradlew.bat"
    if (-not (Test-Path -LiteralPath $gradlew -PathType Leaf)) {
        throw "GRADLE_WRAPPER_MISSING"
    }

    foreach ($task in @("jvmTest", "compileKotlinJs", "compileTestKotlinJs", "checkVexLifeStrings")) {
        $step = Invoke-GradleStep -Task $task -RepoRoot $repoRoot -GradleWrapper $gradlew -LogPath (Join-Path $logsRoot ($task + ".log"))
        $steps += $step
        if ($null -eq $firstFailure -and $step.exitCode -ne 0) {
            $firstFailure = $task
        }
    }
} catch {
    if ($null -eq $firstFailure) {
        $firstFailure = $_.Exception.Message
    }
}

$disposition = if ($null -eq $firstFailure) { "PASS" } else { "SAFE_FAILURE" }

$environment = [ordered]@{
    os = $env:OS
    nativeArchitectureRaw = $nativeArchitectureRaw
    nativeArchitecture = $nativeArchitecture
    processArchitectureRaw = $env:PROCESSOR_ARCHITECTURE
    processArchitecture = $processArchitecture
    processorArchitectureW6432 = $env:PROCESSOR_ARCHITEW6432
    powershellVersion = $PSVersionTable.PSVersion.ToString()
    comSpec = $env:ComSpec
    jdkVersion = $JdkVersion
    jdkAssetName = $JdkAssetName
    jdkExpectedSha256 = $JdkSha256
    jdkDownloaded = $jdkDownloaded
    jdkObservedSha256 = $jdkObservedSha256
    jdkHome = $jdkHome
    javaVersion = $javaVersion
    javacVersion = $javacVersion
}

$result = [ordered]@{
    schemaVersion = "vexlife-prod.windows-source-baseline-return/v1"
    continuity = "[VXG RealForever]"
    attempt = $Attempt
    disposition = $disposition
    sourceRepository = $SourceRepository
    sourceCommit = $SourceCommit
    sourceTree = $SourceTree
    sourceUrl = $sourceUrl
    sourceDownloaded = $sourceDownloaded
    sourceZipSha256 = $sourceZipSha256
    environment = $environment
    checks = $steps
    firstFailure = $firstFailure
    effects = [ordered]@{
        productMutation = $false
        vexHomeMutation = $false
        modelWeightDownload = $false
        productRuntimeStart = $false
        productServiceStart = $false
        productInstallation = $false
        releasePublication = $false
        persistentGlobalJdkInstallation = $false
        attemptLocalJdkMaterialization = $jdkDownloaded
    }
}

Write-Utf8NoBom -Path (Join-Path $returnRoot "result.json") -Text (($result | ConvertTo-Json -Depth 10) + $NL)
Write-Utf8NoBom -Path (Join-Path $returnRoot "environment.json") -Text (($environment | ConvertTo-Json -Depth 8) + $NL)

foreach ($step in $steps) {
    $logSource = Join-Path $logsRoot $step.log
    if (Test-Path -LiteralPath $logSource -PathType Leaf) {
        Copy-Item -LiteralPath $logSource -Destination (Join-Path $returnRoot $step.log)
    }
}

$summary = @(
    "VexLife-Prod Windows source baseline A003"
    "source=$SourceCommit"
    "tree=$SourceTree"
    "host=WINDOWS/$nativeArchitecture"
    "processArchitecture=$processArchitecture"
    "jdk=$JdkVersion"
    "jdkSha256=$jdkObservedSha256"
    "disposition=$disposition"
    "firstFailure=$firstFailure"
    "productEffectsPerformed=false"
    "persistentGlobalJdkInstallation=false"
    ""
    "This run proves source/build portability only."
    "The JDK exists only inside this attempt capsule."
    "It does not claim Windows onboarding, model runtime, Product service lifetime, native setup UI, packaging, or release support."
) -join $NL
Write-Utf8NoBom -Path (Join-Path $returnRoot "RESULT-SUMMARY.txt") -Text ($summary + $NL)

$inventory = @()
Get-ChildItem -LiteralPath $returnRoot -File | Sort-Object Name | ForEach-Object {
    $inventory += [ordered]@{
        path = $_.Name
        bytes = $_.Length
        sha256 = (Get-LowerSha256 $_.FullName)
    }
}
Write-Utf8NoBom -Path (Join-Path $returnRoot "inventory.json") -Text (($inventory | ConvertTo-Json -Depth 5) + $NL)

$tempZip = Join-Path $OutputDirectory "RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A003.forming.zip"
if (Test-Path -LiteralPath $tempZip) {
    Remove-Item -LiteralPath $tempZip -Force
}
Compress-Archive -Path (Join-Path $returnRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal

$returnSha = Get-LowerSha256 $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A003--sha256-" + $returnSha + ".zip")
if (Test-Path -LiteralPath $finalZip) {
    Remove-Item -LiteralPath $finalZip -Force
}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host ""
Write-Host "VEXLIFE_PROD_WINDOWS_BASELINE_RETURN"
Write-Host ("path=" + $finalZip)
Write-Host ("sha256=" + $returnSha)
Write-Host ("disposition=" + $disposition)
Write-Host ("host=WINDOWS/" + $nativeArchitecture)
Write-Host ("source=" + $SourceCommit)
Write-Host ("jdk=" + $JdkVersion)

try {
    $explorerArg = '/select,"{0}"' -f $finalZip
    Start-Process explorer.exe -ArgumentList $explorerArg | Out-Null
} catch {
    # Reveal is optional; exact printed path remains authoritative.
}

Remove-Item -LiteralPath $capsule -Recurse -Force

if ($disposition -ne "PASS") {
    exit 2
}
