# [VXG RealForever]
[CmdletBinding()]
param([string]$OutputDirectory = "$HOME\Downloads")

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$Head = 'bc231e314024e266138bde1e3ac3417934815b97'
$Tree = '469b0ce61702788cc6890c9edac05b70a603ff5b'
$Branch = 'VXG-111826-chatgpt-windows-current-main-integration-a001'
$Repo = 'VexGPT/VexLife-Prod'
$JdkUrl = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
$JdkSha = 'a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9'

function Sha([string]$Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}
function Save([string]$Path, [string]$Text) {
    $Utf8 = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($Path, $Text, $Utf8)
}
function Service-Reachable {
    try {
        $null = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18210/api/v1/source-currentness' -TimeoutSec 2
        return $true
    } catch { return $false }
}
function Wait-Service([bool]$Present) {
    for ($i = 0; $i -lt 90; $i++) {
        if ((Service-Reachable) -eq $Present) { return }
        Start-Sleep -Seconds 1
    }
    if ($Present) { throw 'SERVICE_NOT_REACHABLE' } else { throw 'SERVICE_DID_NOT_STOP' }
}
function Capture([string]$Name, [string]$Evidence) {
    $S = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18210/api/v1/source-currentness' -TimeoutSec 5
    $C = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18210/api/v1/current-state' -TimeoutSec 5
    Save (Join-Path $Evidence ("source-" + $Name + ".json")) ([string]$S.Content)
    Save (Join-Path $Evidence ("state-" + $Name + ".json")) ([string]$C.Content)
    $Source = $S.Content | ConvertFrom-Json
    $State = $C.Content | ConvertFrom-Json
    if ([string]$Source.sourceSha -ne $Head) { throw ("SOURCE_MISMATCH:" + [string]$Source.sourceSha) }
    return [ordered]@{
        source = [string]$Source.sourceSha
        pid = [long]$Source.processPid
        head = if ($null -eq $Source.currentConversationHeadSha256) { $null } else { [string]$Source.currentConversationHeadSha256 }
        home = [string]$State.homePath
    }
}
function Gradle([string]$Gradle, [string[]]$Args, [string]$Log, [string]$Failure) {
    $Old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $Out = @(& $Gradle @Args 2>&1)
        $Code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $Old }
    $Out | ForEach-Object { Write-Host ([string]$_) }
    Save $Log (($Out | ForEach-Object { [string]$_ }) -join [Environment]::NewLine)
    if ($Code -ne 0) { throw ($Failure + ':exit=' + $Code) }
}
function Return-Zip([string]$Evidence, [string]$Disposition, [string]$Failure) {
    Save (Join-Path $Evidence 'RESULT-SUMMARY.txt') (
        'VexLife PR317 final Windows sanity' + [Environment]::NewLine +
        'head=' + $Head + [Environment]::NewLine +
        'tree=' + $Tree + [Environment]::NewLine +
        'disposition=' + $Disposition + [Environment]::NewLine +
        'failure=' + $Failure + [Environment]::NewLine +
        'conversationContentIncluded=false' + [Environment]::NewLine
    )
    $Inventory = @()
    Get-ChildItem -LiteralPath $Evidence -File -Recurse | Sort-Object FullName | ForEach-Object {
        $Inventory += [ordered]@{
            path = $_.FullName.Substring($Evidence.Length).TrimStart('\')
            bytes = $_.Length
            sha256 = Sha $_.FullName
        }
    }
    Save (Join-Path $Evidence 'inventory.json') (($Inventory | ConvertTo-Json -Depth 6) + [Environment]::NewLine)
    $Tmp = Join-Path $OutputDirectory 'RETURN-VEXLIFE-PR317-WINDOWS-FINAL-A001.forming.zip'
    if (Test-Path $Tmp) { Remove-Item $Tmp -Force }
    Compress-Archive -Path (Join-Path $Evidence '*') -DestinationPath $Tmp -CompressionLevel Optimal
    $Digest = Sha $Tmp
    $Final = Join-Path $OutputDirectory ('RETURN-VEXLIFE-PR317-WINDOWS-FINAL-A001--sha256-' + $Digest + '.zip')
    if (Test-Path $Final) { Remove-Item $Final -Force }
    Move-Item $Tmp $Final
    Write-Host ('RETURN_ZIP=' + $Final)
    Write-Host ('RETURN_SHA256=' + $Digest)
    Write-Host ('PR317_WINDOWS_FINAL_DISPOSITION=' + $Disposition)
}

if ($env:OS -ne 'Windows_NT') { throw 'WINDOWS_REQUIRED' }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$Temp = Join-Path $env:TEMP ('vexlife-pr317-final-' + $PID)
$Evidence = Join-Path $OutputDirectory ('VEXLIFE-PR317-WINDOWS-FINAL-EVIDENCE-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory -Path $Temp | Out-Null
New-Item -ItemType Directory -Path $Evidence | Out-Null

$Disposition = 'SAFE_FAILURE'
$Failure = ''
$OldJava = $env:JAVA_HOME
$OldPath = $env:Path
$OldGradle = $env:GRADLE_OPTS

try {
    $Gh = (Get-Command gh -ErrorAction Stop).Source
    $Git = (Get-Command git -ErrorAction Stop).Source
    & $Gh auth status --hostname github.com
    if ($LASTEXITCODE -ne 0) { throw 'GITHUB_LOGIN_REQUIRED' }

    $Root = Join-Path $Temp 'repo'
    & $Gh repo clone $Repo $Root -- --no-checkout --origin origin
    if ($LASTEXITCODE -ne 0) { throw 'CLONE_FAILED' }
    & $Git -C $Root fetch --no-tags --depth=256 origin $Branch
    if ($LASTEXITCODE -ne 0) { throw 'FETCH_FAILED' }
    $Fetched = (& $Git -C $Root rev-parse FETCH_HEAD).Trim()
    if ($Fetched -ne $Head) { throw ('BRANCH_HEAD_MISMATCH:' + $Fetched) }
    & $Git -C $Root checkout --detach $Head
    if ($LASTEXITCODE -ne 0) { throw 'CHECKOUT_FAILED' }
    if ((& $Git -C $Root rev-parse 'HEAD^{tree}').Trim() -ne $Tree) { throw 'TREE_MISMATCH' }
    if (@(& $Git -C $Root status --porcelain=v1 --untracked-files=all).Count -ne 0) { throw 'SOURCE_NOT_CLEAN' }
    Save (Join-Path $Evidence 'source.txt') ('head=' + $Head + [Environment]::NewLine + 'tree=' + $Tree + [Environment]::NewLine)

    $Cache = Join-Path $OutputDirectory 'VEXLIFE-WINDOWS-FINAL-CACHE'
    New-Item -ItemType Directory -Path $Cache -Force | Out-Null
    $JdkZip = Join-Path $Cache 'jdk21.zip'
    if (-not (Test-Path $JdkZip)) { Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $JdkZip }
    if ((Sha $JdkZip) -ne $JdkSha) { throw 'JDK_SHA_MISMATCH' }
    $JdkDir = Join-Path $Temp 'jdk'
    New-Item -ItemType Directory -Path $JdkDir | Out-Null
    Expand-Archive $JdkZip $JdkDir
    $Dirs = @(Get-ChildItem $JdkDir -Directory)
    if ($Dirs.Count -ne 1) { throw 'JDK_ROOT_AMBIGUOUS' }
    $env:JAVA_HOME = $Dirs[0].FullName
    $env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $OldPath
    $env:GRADLE_OPTS = '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process'

    $Gradle = Join-Path $Root 'gradlew.bat'
    Gradle $Gradle @('--no-daemon','--max-workers=1','-p',$Root,'checkVexLifeStrings') (Join-Path $Evidence '01-strings.log') 'STRINGS_FAILED'
    Gradle $Gradle @(
        '--no-daemon','--max-workers=1','-p',$Root,'jvmTest',
        '--tests','vexlife.platform.windows.VexLifeProdWindowsPreviewTest',
        '--tests','vexlife.platform.windows.VexLifeProdWindowsRuntimeSessionTest',
        '--tests','vexlife.platform.windows.VexLifeProdWindowsSetupTest',
        '--tests','vexlife.platform.windows.VexLifeWindowsHereticRuntimeTest',
        '--tests','vexlife.platform.windows.VexLifeWindowsHostObservationTest',
        '--tests','vexlife.platform.windows.VexLifeWindowsReadinessTest',
        '--tests','vexlife.platform.windows.VexLifeWindowsRuntimeMaterializationTest',
        '--tests','vexlife.service.VexLifeProdLocalServiceTest',
        '--tests','vexlife.service.VexLifeProdServiceProcessTest'
    ) (Join-Path $Evidence '02-focused-tests.log') 'WINDOWS_FOCUSED_TESTS_FAILED'

    $Builder = Join-Path $Root 'tools\windows-preview\FORM-WINDOWS-PREVIEW.ps1'
    $OldPref = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $BuildOut = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $Builder -OutputDirectory $OutputDirectory 2>&1)
        $BuildCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $OldPref }
    $BuildOut | ForEach-Object { Write-Host ([string]$_) }
    Save (Join-Path $Evidence '03-builder.log') (($BuildOut | ForEach-Object { [string]$_ }) -join [Environment]::NewLine)
    if ($BuildCode -ne 0) { throw ('APP_IMAGE_BUILD_FAILED:' + $BuildCode) }

    $ZipLine = @($BuildOut | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_ZIP=') })[-1]
    $ShaLine = @($BuildOut | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_ZIP_SHA256=') })[-1]
    $SourceLine = @($BuildOut | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_SOURCE=') })[-1]
    $TreeLine = @($BuildOut | Where-Object { ([string]$_).StartsWith('VEXLIFE_WINDOWS_PREVIEW_TREE=') })[-1]
    if (-not $ZipLine -or -not $ShaLine -or -not $SourceLine -or -not $TreeLine) { throw 'BUILDER_POINTERS_MISSING' }
    if (([string]$SourceLine).Substring(31) -ne $Head) { throw 'BUILT_SOURCE_MISMATCH' }
    if (([string]$TreeLine).Substring(29) -ne $Tree) { throw 'BUILT_TREE_MISMATCH' }
    $Zip = [IO.Path]::GetFullPath(([string]$ZipLine).Substring(28))
    $ZipSha = ([string]$ShaLine).Substring(35).ToLowerInvariant()
    if ((Sha $Zip) -ne $ZipSha) { throw 'PREVIEW_ZIP_SHA_MISMATCH' }

    $Extract = Join-Path $OutputDirectory ('VexLife-PR317-Windows-' + $Head.Substring(0,12) + '-' + $ZipSha.Substring(0,12))
    if (Test-Path $Extract) { Remove-Item $Extract -Recurse -Force }
    Expand-Archive $Zip $Extract
    $App = Join-Path $Extract 'VexLife'
    $Exe = Join-Path $App 'VexLife.exe'
    $Receipt = Join-Path $App 'windows-lived-preview.receipt.json'
    if (-not (Test-Path $Exe) -or -not (Test-Path $Receipt)) { throw 'PREVIEW_FILES_MISSING' }
    $ReceiptJson = Get-Content $Receipt -Raw | ConvertFrom-Json
    if ([string]$ReceiptJson.sourceSha -ne $Head -or [string]$ReceiptJson.sourceTree -ne $Tree) { throw 'RECEIPT_SOURCE_MISMATCH' }
    Copy-Item $Receipt (Join-Path $Evidence 'windows-lived-preview.receipt.json') -Force

    if (Service-Reachable) {
        Write-Host 'Close any existing VexLife app/window normally. Do not kill Java/model processes.'
        $null = Read-Host 'Press Enter after VexLife is closed'
        Wait-Service $false
    }

    $P = Start-Process -FilePath $Exe -WorkingDirectory $App -PassThru
    Start-Sleep 5
    $P.Refresh()
    if ($P.HasExited) { throw ('ORDINARY_LAUNCH_EXITED_EARLY:' + $P.ExitCode) }

    Write-Host 'Use the native Setup Journey until the browser Product is Ready. Do not send a new message yet.'
    $null = Read-Host 'Press Enter when VexLife is Ready'
    Wait-Service $true
    $Before = Capture 'before' $Evidence
    Save (Join-Path $Evidence 'before-summary.json') (($Before | ConvertTo-Json -Depth 4) + [Environment]::NewLine)

    Write-Host 'Complete ONE real Conversation turn and receive Vex''s reply.'
    $null = Read-Host 'Press Enter after the turn completes'
    $After = Capture 'after' $Evidence
    Save (Join-Path $Evidence 'after-summary.json') (($After | ConvertTo-Json -Depth 4) + [Environment]::NewLine)
    if ([string]::IsNullOrWhiteSpace([string]$After.head)) { throw 'CONVERSATION_HEAD_MISSING_AFTER_TURN' }
    if ($null -ne $Before.head -and [string]$Before.head -eq [string]$After.head) { throw 'CONVERSATION_HEAD_DID_NOT_ADVANCE' }
    if ([string]$Before.home -ne [string]$After.home) { throw 'HOME_CHANGED_DURING_TURN' }

    Write-Host 'Close VexLife normally. Leave the Home untouched.'
    $null = Read-Host 'Press Enter after VexLife is closed'
    Wait-Service $false

    $P2 = Start-Process -FilePath $Exe -WorkingDirectory $App -PassThru
    Write-Host 'Let VexLife reopen, then confirm the prior Conversation is still visible in Archives.'
    $null = Read-Host 'Press Enter after continuity is visible'
    Wait-Service $true
    $Reopen = Capture 'reopen' $Evidence
    Save (Join-Path $Evidence 'reopen-summary.json') (($Reopen | ConvertTo-Json -Depth 4) + [Environment]::NewLine)
    if ([string]$Reopen.home -ne [string]$After.home) { throw 'HOME_CONTINUITY_MISMATCH' }
    if ([string]$Reopen.head -ne [string]$After.head) { throw 'CONVERSATION_HEAD_NOT_PRESERVED' }

    Save (Join-Path $Evidence 'human-confirmation.txt') (
        'oneRealConversationTurnCompleted=yes' + [Environment]::NewLine +
        'priorConversationVisibleAfterReopen=yes' + [Environment]::NewLine +
        'sameHome=' + [string]$After.home + [Environment]::NewLine +
        'candidateSource=' + $Head + [Environment]::NewLine
    )

    $Disposition = 'PASS'
} catch {
    $Failure = $_.Exception.Message
    Write-Host ('SAFE_FAILURE: ' + $Failure)
} finally {
    $env:JAVA_HOME = $OldJava
    $env:Path = $OldPath
    $env:GRADLE_OPTS = $OldGradle
    try { Remove-Item $Temp -Recurse -Force } catch {}
    Return-Zip $Evidence $Disposition $Failure
}

if ($Disposition -ne 'PASS') { exit 2 }
