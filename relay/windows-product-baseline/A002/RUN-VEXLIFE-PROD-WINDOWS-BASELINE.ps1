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
$Attempt = "A002"

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

    # Windows .bat/.cmd scripts cross the exact command-script boundary rather than native spawn.
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

$capsule = Join-Path $env:TEMP ("vexlife-prod-windows-baseline-" + $Attempt.ToLowerInvariant() + "-" + $SourceCommit.Substring(0,12))
if (Test-Path -LiteralPath $capsule) {
    Remove-Item -LiteralPath $capsule -Recurse -Force
}
New-Item -ItemType Directory -Path $capsule | Out-Null

$logsRoot = Join-Path $capsule "logs"
$returnRoot = Join-Path $capsule "return"
$sourceRoot = Join-Path $capsule "source"
$gradleHome = Join-Path $capsule "gradle-home"
New-Item -ItemType Directory -Path $logsRoot | Out-Null
New-Item -ItemType Directorcy -Path $returnRoot | Out-Null
New-Item -ItemType Directory -Path $sourceRoot | Out-Null
New-Item -ItemType Directory -Path $gradleHome | Out-Null
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$env:GRADLE_USER_HOME = $gradleHome

$sourceZip = Join-Path $capsule "source.zip"
$sourceUrl = "https://github.com/VexGPT/VexLife-Prod/archive/$SourceCommit.zip"
$sourceDownloaded = $false
$sourceZipSha256 = $null
$repoRoot = $null
$javaPresent = $false
$javaVersion = $null
$steps = @()
$firstFailure = $null

try {
    Invoke-WebRequest -UseBasicParsing -Uri $sourceUrl -OutFile $sourceZip
    $sourceDownloaded = $true
    $sourceZipSha256 = Get-LowerSha256 $sourceZip

    Expand-Archive -LiteralPath $sourceZip -DestinationPath $sourceRoot

    $roots = @(Get-ChildItem -LiteralPath $sourceRoot -Directory)
    if ($roots.Count -ne 1) {
        throw "SOURCE_ARCHIVE_ROOT_AMBIGUOUS"
    }
    $repoRoot = $roots[0].FullName

    $gradlew = Join-Path $repoRoot "gradlew.bat"
    if (-not (Test-Path -LiteralPath $gradlew -PathType Leaf)) {
        throw "GRADLE_WRAPPER_MISSING"
    }

    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -eq $java) {
        throw "JAVA_RUNTIME_UNAVAILABLE"
    }
    $javaPresent = $true
    $javaVersion = (& $java.Source -version 2>&1 | Out-String).Trim()

    foreach ($task in @("jvmTest", "compileKotlinJs", "compileTestKotlinJs", "checkVexLifeStrings")) {
        $step = Invoke-GradleStep `
            -Task $task `
            -RepoRoot $repoRoot `
            -GradleWrapper $gradlew `
            -LogPath (Join-Path $logsRoot ($task + ".log"))
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
    javaPresent = $javaPresent
    javaVersion = $javaVersion
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
    }
}

Write-Utf8NoBom -Path (Join-Path $returnRoot "result.json") -Text (($result | ConvertTo-Json -Depth 10) + $NL)
Write-Utf8NoBom -Path (Join-Path $returnRoot "environment.json") -Text (($environment | ConvertTo-Json -Depth 6) + $NL)

foreach ($step in $steps) {
    $logSource = Join-Path $logsRoot $step.log
    if (Test-Path -LiteralPath $logSource -PathType Leaf) {
        Copy-Item -LiteralPath $logSource -Destination (Join-Path $returnRoot $step.log)
    }
}

$summary = @(
    "VexLife-Prod Windows source baseline A002"
    "source=$SourceCommit"
    "tree=$SourceTree"
    "host=WINDOWS/$nativeArchitecture"
    "processArchitecture=$processArchitecture"
    "disposition=$disposition"
    "firstFailure=$firstFailure"
    "productEffectsPerformed=false"
    ""
    "This run proves source/build portability only."
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

$tempZip = Join-Path $OutputDirectory "RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A002.forming.zip"
if (Test-Path -LiteralPath $tempZip) {
    Remove-Item -LiteralPath $tempZip -Force
}
Compress-Archive -Path (Join-Path $returnRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal

$returnSha = Get-LowerSha256 $tempZip
$finalZip = Join-Path $OutputDirectory ("RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A002--sha256-" + $returnSha + ".zip")
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

try {
    $explorerArg = '/select,"{0}"' -f $finalZip
    Start-Process explorer.exe -ArgumentList $explorerArg | Out-Null
} catch {
    # Reveal is optional; exact printed path remains authoritative.
}

if ($disposition -ne "PASS") {
    exit 2
}
