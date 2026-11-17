# [VXG RealForever]
[CmdletBinding()]
param([Parameter(Mandatory=$false)][string]$OutputDirectory="$HOME\Downloads")

$ErrorActionPreference="Stop"
Set-StrictMode -Version Latest
$NL=[Environment]::NewLine

$SourceRepository="VexGPT/VexLife-Prod"
$SourceCommit="655e7c78cefba06a5e87afad285bb25a08632f05"
$SourceTree="886c131da9e3c011ed88a6badad401430362b35c"
$Attempt="A006"

$JdkVersion="21.0.13+9"
$JdkAssetName="OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"
$JdkSha256="a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9"
$JdkUrl="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"

function Get-LowerSha256 {
  param([Parameter(Mandatory=$true)][string]$Path)
  return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Write-Utf8NoBom {
  param([Parameter(Mandatory=$true)][string]$Path,[Parameter(Mandatory=$true)][string]$Text)
  $utf8=New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($Path,$Text,$utf8)
}

function Normalize-WindowsArchitecture {
  param([string]$Value)
  if([string]::IsNullOrWhiteSpace($Value)){return $null}
  switch($Value.Trim().ToUpperInvariant()){
    "AMD64"{return "X86_64"}
    "X64"{return "X86_64"}
    "X86_64"{return "X86_64"}
    "ARM64"{return "ARM64"}
    "AARCH64"{return "ARM64"}
    default{return "UNKNOWN:"+$Value}
  }
}

function Observe-Memory {
  $os=Get-CimInstance Win32_OperatingSystem
  $total=[long]$os.TotalVisibleMemorySize*1024L
  $free=[long]$os.FreePhysicalMemory*1024L
  $used=$total-$free
  $pct=if($total -gt 0){[math]::Round(($used*100.0)/$total,1)}else{$null}
  return [ordered]@{
    observedAtUtc=[DateTimeOffset]::UtcNow.ToString("o")
    totalBytes=$total
    freeBytes=$free
    usedBytes=$used
    usedPercent=$pct
  }
}

function Invoke-ProcessCaptured {
  param(
    [Parameter(Mandatory=$true)][string]$Name,
    [Parameter(Mandatory=$true)][string]$FilePath,
    [Parameter(Mandatory=$true)][string[]]$ArgumentList,
    [Parameter(Mandatory=$true)][string]$WorkingDirectory,
    [Parameter(Mandatory=$true)][string]$LogPath
  )

  $started=[DateTimeOffset]::UtcNow
  $stdoutPath=$LogPath+".stdout"
  $stderrPath=$LogPath+".stderr"
  if(Test-Path -LiteralPath $stdoutPath){Remove-Item -LiteralPath $stdoutPath -Force}
  if(Test-Path -LiteralPath $stderrPath){Remove-Item -LiteralPath $stderrPath -Force}

  $process=Start-Process -FilePath $FilePath -ArgumentList $ArgumentList -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -NoNewWindow -Wait -PassThru
  $finished=[DateTimeOffset]::UtcNow
  $stdout=if(Test-Path -LiteralPath $stdoutPath){Get-Content -LiteralPath $stdoutPath -Raw}else{""}
  $stderr=if(Test-Path -LiteralPath $stderrPath){Get-Content -LiteralPath $stderrPath -Raw}else{""}

  Write-Utf8NoBom -Path $LogPath -Text ((@(
    "name=$Name"
    "filePath=$FilePath"
    "startedAtUtc=$($started.ToString('o'))"
    "finishedAtUtc=$($finished.ToString('o'))"
    "exitCode=$($process.ExitCode)"
    ""
    "=== STDOUT ==="
    $stdout
    ""
    "=== STDERR ==="
    $stderr
  ) -join $NL)+$NL)

  Remove-Item -LiteralPath $stdoutPath -Force -ErrorAction SilentlyContinue
  Remove-Item -LiteralPath $stderrPath -Force -ErrorAction SilentlyContinue

  return [ordered]@{
    name=$Name
    exitCode=$process.ExitCode
    stdout=$stdout
    stderr=$stderr
  }
}

function Invoke-VisibleGradleStep {
  param(
    [Parameter(Mandatory=$true)][string]$Task,
    [Parameter(Mandatory=$true)][string]$RepoRoot,
    [Parameter(Mandatory=$true)][string]$LogPath
  )

  $cmd=$env:ComSpec
  if([string]::IsNullOrWhiteSpace($cmd) -or -not(Test-Path -LiteralPath $cmd -PathType Leaf)){
    throw "WINDOWS_COMSPEC_UNAVAILABLE"
  }

  $before=Observe-Memory
  $freeGiB=[math]::Round($before.freeBytes/1GB,2)
  $usedPct=$before.usedPercent
  Write-Host ""
  Write-Host ("A006 START "+$Task+" | freeRAM="+$freeGiB+" GiB | used="+$usedPct+"%")
  if($before.freeBytes -lt 2GB){
    throw ("INSUFFICIENT_FREE_MEMORY_BEFORE_"+$Task+": freeBytes="+$before.freeBytes)
  }

  $started=[DateTimeOffset]::UtcNow
  $prior=$ErrorActionPreference
  Push-Location $RepoRoot
  try {
    $ErrorActionPreference="Continue"
    & $cmd /d /s /c ("call gradlew.bat --no-daemon --max-workers=1 --console=plain "+$Task) 2>&1 |
      Tee-Object -FilePath $LogPath
    $exitCode=$LASTEXITCODE
  } finally {
    $ErrorActionPreference=$prior
    Pop-Location
  }
  $finished=[DateTimeOffset]::UtcNow
  $after=Observe-Memory

  if($exitCode -eq 0){
    Write-Host ("A006 PASS  "+$Task+" | used="+$after.usedPercent+"%")
  } else {
    Write-Host ("A006 FAIL  "+$Task+" | exit="+$exitCode+" | used="+$after.usedPercent+"%")
  }

  return [ordered]@{
    task=$Task
    exitCode=$exitCode
    startedAtUtc=$started.ToString("o")
    finishedAtUtc=$finished.ToString("o")
    memoryBefore=$before
    memoryAfter=$after
    log=[IO.Path]::GetFileName($LogPath)
  }
}

if($env:OS -ne "Windows_NT"){throw "WINDOWS_BASELINE_REQUIRES_WINDOWS_NT"}

$nativeArchitectureRaw=if(-not[string]::IsNullOrWhiteSpace($env:PROCESSOR_ARCHITEW6432)){$env:PROCESSOR_ARCHITEW6432}else{$env:PROCESSOR_ARCHITECTURE}
$nativeArchitecture=Normalize-WindowsArchitecture $nativeArchitectureRaw
$processArchitecture=Normalize-WindowsArchitecture $env:PROCESSOR_ARCHITECTURE
if($nativeArchitecture -ne "X86_64"){throw ("A006_PINNED_JDK_REQUIRES_WINDOWS_X86_64:"+$nativeArchitecture)}

$capsule=Join-Path $env:TEMP ("vexlife-prod-windows-baseline-"+$Attempt.ToLowerInvariant()+"-"+$SourceCommit.Substring(0,12))
if(Test-Path -LiteralPath $capsule){Remove-Item -LiteralPath $capsule -Recurse -Force}
New-Item -ItemType Directory -Path $capsule|Out-Null

$logsRoot=Join-Path $capsule "logs"
$returnRoot=Join-Path $capsule "return"
$sourceRoot=Join-Path $capsule "source"
$gradleHome=Join-Path $capsule "gradle-home"
$jdkArchive=Join-Path $capsule $JdkAssetName
$jdkExtractRoot=Join-Path $capsule "jdk"
foreach($dir in @($logsRoot,$returnRoot,$sourceRoot,$gradleHome,$jdkExtractRoot)){New-Item -ItemType Directory -Path $dir|Out-Null}
New-Item -ItemType Directory -Path $OutputDirectory -Force|Out-Null

$gradleProperties=@(
  "org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
  "org.gradle.workers.max=1"
  "org.gradle.parallel=false"
  "kotlin.compiler.execution.strategy=in-process"
) -join $NL
Write-Utf8NoBom -Path (Join-Path $gradleHome "gradle.properties") -Text ($gradleProperties+$NL)

$env:GRADLE_USER_HOME=$gradleHome
$env:JAVA_TOOL_OPTIONS="-Xmx1024m -Dfile.encoding=UTF-8"

$sourceZip=Join-Path $capsule "source.zip"
$sourceUrl="https://github.com/VexGPT/VexLife-Prod/archive/$SourceCommit.zip"
$sourceDownloaded=$false
$sourceZipSha256=$null
$jdkDownloaded=$false
$jdkObservedSha256=$null
$jdkHome=$null
$javaVersion=$null
$repoRoot=$null
$steps=@()
$firstFailure=$null
$memoryStart=Observe-Memory

try {
  Write-Host ("A006 PREP  host=WINDOWS/"+$nativeArchitecture+" | usedRAM="+$memoryStart.usedPercent+"%")
  Write-Host "A006 PREP  downloading verified attempt-local JDK 21..."
  Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $jdkArchive
  $jdkDownloaded=$true
  $jdkObservedSha256=Get-LowerSha256 $jdkArchive
  if($jdkObservedSha256 -ne $JdkSha256){throw ("JDK_SHA256_MISMATCH expected="+$JdkSha256+" observed="+$jdkObservedSha256)}

  Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkExtractRoot
  $jdkRoots=@(Get-ChildItem -LiteralPath $jdkExtractRoot -Directory)
  if($jdkRoots.Count -ne 1){throw "JDK_ARCHIVE_ROOT_AMBIGUOUS"}
  $jdkHome=$jdkRoots[0].FullName
  $javaExe=Join-Path $jdkHome "bin\java.exe"
  if(-not(Test-Path -LiteralPath $javaExe -PathType Leaf)){throw "JDK_JAVA_EXECUTABLE_MISSING"}

  $env:JAVA_HOME=$jdkHome
  $env:PATH=(Join-Path $jdkHome "bin")+[IO.Path]::PathSeparator+$env:PATH

  $javaCheck=Invoke-ProcessCaptured -Name "java-version" -FilePath $javaExe -ArgumentList @("-version") -WorkingDirectory $jdkHome -LogPath (Join-Path $logsRoot "java-version.log")
  if($javaCheck.exitCode -ne 0){throw ("JAVA_VERSION_CHECK_FAILED:"+$javaCheck.exitCode)}
  $javaVersion=(($javaCheck.stderr+$NL+$javaCheck.stdout).Trim())
  Write-Host "A006 PREP  JDK verified."

  Write-Host "A006 PREP  downloading exact VexLife-Prod source..."
  Invoke-WebRequest -UseBasicParsing -Uri $sourceUrl -OutFile $sourceZip
  $sourceDownloaded=$true
  $sourceZipSha256=Get-LowerSha256 $sourceZip
  Expand-Archive -LiteralPath $sourceZip -DestinationPath $sourceRoot
  $roots=@(Get-ChildItem -LiteralPath $sourceRoot -Directory)
  if($roots.Count -ne 1){throw "SOURCE_ARCHIVE_ROOT_AMBIGUOUS"}
  $repoRoot=$roots[0].FullName
  if(-not $roots[0].Name.EndsWith($SourceCommit)){throw ("SOURCE_ARCHIVE_ROOT_IDENTITY_DRIFT:"+$roots[0].Name)}
  if(-not(Test-Path -LiteralPath (Join-Path $repoRoot "gradlew.bat") -PathType Leaf)){throw "GRADLE_WRAPPER_MISSING"}
  Write-Host "A006 PREP  source ready."

  foreach($task in @("checkVexLifeStrings","compileKotlinJs")){
    $step=Invoke-VisibleGradleStep -Task $task -RepoRoot $repoRoot -LogPath (Join-Path $logsRoot ($task+".log"))
    $steps+=$step
    if($step.exitCode -ne 0){
      $firstFailure=$task
      break
    }
  }
} catch {
  if($null -eq $firstFailure){$firstFailure=$_.Exception.Message}
}

$memoryEnd=Observe-Memory
$disposition=if($null -eq $firstFailure){"PASS"}else{"SAFE_FAILURE"}

$environment=[ordered]@{
  os=$env:OS
  nativeArchitecture=$nativeArchitecture
  processArchitecture=$processArchitecture
  powershellVersion=$PSVersionTable.PSVersion.ToString()
  jdkVersion=$JdkVersion
  jdkExpectedSha256=$JdkSha256
  jdkObservedSha256=$jdkObservedSha256
  javaVersion=$javaVersion
  buildPolicy=[ordered]@{
    gradleMaxHeapBytes=2147483648
    javaToolMaxHeapBytes=1073741824
    maxWorkers=1
    parallel=$false
    kotlinCompilerExecution="in-process"
    fullJvmTestRun=$false
  }
  memoryAtStart=$memoryStart
  memoryAtEnd=$memoryEnd
}

$result=[ordered]@{
  schemaVersion="vexlife-prod.windows-lightweight-source-baseline-return/v1"
  continuity="[VXG RealForever]"
  attempt=$Attempt
  disposition=$disposition
  sourceRepository=$SourceRepository
  sourceCommit=$SourceCommit
  sourceTree=$SourceTree
  sourceDownloaded=$sourceDownloaded
  sourceZipSha256=$sourceZipSha256
  environment=$environment
  checks=$steps
  firstFailure=$firstFailure
  effects=[ordered]@{
    productMutation=$false
    vexHomeMutation=$false
    modelWeightDownload=$false
    modelRuntimeLoad=$false
    productRuntimeStart=$false
    productServiceStart=$false
    productInstallation=$false
    releasePublication=$false
    persistentGlobalJdkInstallation=$false
    attemptLocalJdkMaterialization=$jdkDownloaded
  }
}

Write-Utf8NoBom -Path (Join-Path $returnRoot "result.json") -Text (($result|ConvertTo-Json -Depth 12)+$NL)
Write-Utf8NoBom -Path (Join-Path $returnRoot "environment.json") -Text (($environment|ConvertTo-Json -Depth 10)+$NL)
foreach($logName in @("java-version.log","checkVexLifeStrings.log","compileKotlinJs.log")){
  $src=Join-Path $logsRoot $logName
  if(Test-Path -LiteralPath $src -PathType Leaf){Copy-Item -LiteralPath $src -Destination (Join-Path $returnRoot $logName)}
}

$summary=@(
  "VexLife-Prod Windows lightweight source baseline A006"
  "source=$SourceCommit"
  "host=WINDOWS/$nativeArchitecture"
  "disposition=$disposition"
  "firstFailure=$firstFailure"
  "fullJvmTestRun=false"
  "modelWeightDownload=false"
  "modelRuntimeLoad=false"
  "Gradle max heap=2 GiB"
  "Java child max heap=1 GiB"
  "Gradle workers=1"
  ""
  "A006 answers only whether current Product JVM/localization and shared JS main source compile on Windows under a bounded build envelope."
) -join $NL
Write-Utf8NoBom -Path (Join-Path $returnRoot "RESULT-SUMMARY.txt") -Text ($summary+$NL)

$inventory=@()
Get-ChildItem -LiteralPath $returnRoot -File|Sort-Object Name|ForEach-Object{
  $inventory+=[ordered]@{path=$_.Name;bytes=$_.Length;sha256=(Get-LowerSha256 $_.FullName)}
}
Write-Utf8NoBom -Path (Join-Path $returnRoot "inventory.json") -Text (($inventory|ConvertTo-Json -Depth 5)+$NL)

$tempZip=Join-Path $OutputDirectory "RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A006.forming.zip"
if(Test-Path -LiteralPath $tempZip){Remove-Item -LiteralPath $tempZip -Force}
Compress-Archive -Path (Join-Path $returnRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha=Get-LowerSha256 $tempZip
$finalZip=Join-Path $OutputDirectory ("RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A006--sha256-"+$returnSha+".zip")
if(Test-Path -LiteralPath $finalZip){Remove-Item -LiteralPath $finalZip -Force}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host ""
Write-Host "VEXLIFE_PROD_WINDOWS_BASELINE_RETURN"
Write-Host ("path="+$finalZip)
Write-Host ("sha256="+$returnSha)
Write-Host ("disposition="+$disposition)
Write-Host ("memoryEndUsedPercent="+$memoryEnd.usedPercent)

try{$explorerArg='/select,"{0}"' -f $finalZip;Start-Process explorer.exe -ArgumentList $explorerArg|Out-Null}catch{}
Remove-Item -LiteralPath $capsule -Recurse -Force
if($disposition -ne "PASS"){exit 2}
