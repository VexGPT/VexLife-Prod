[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$RelaySourceHead = '5babe5da48a00c938a86a92045f71eb8ced0f634'
$CandidateHead = '2c54c81f3d318163ee5b09a6d35363af75c33c9d'
$SdkHead = '8543c7d755faba04d4c4019c02e84f992667918a'
$SdkTree = '6376f68279f0750ee6d3868b8472368199763064'
$ContractBlob = 'b53cbac01c2deeb45fad5c32ce98e0796c04126f'
$RawBase = "https://raw.githubusercontent.com/VexGPT/VexLife-Prod/$RelaySourceHead/relay/windows-runtime-realization/A002"
$RunRoot = Join-Path $HOME ('Downloads\VEXRELAY-WINDOWS-RUNTIME-REALIZATION-SOURCEOWNER-A002-run-' + $CandidateHead.Substring(0,16))

if (Test-Path -LiteralPath $RunRoot) {
    throw 'A002_RUN_ROOT_ALREADY_EXISTS_DO_NOT_REPLAY'
}
New-Item -ItemType Directory -Path $RunRoot | Out-Null

$ContextPath = Join-Path $RunRoot 'relay-context.json'
$FormPath = Join-Path $RunRoot 'FORM-VEXRELAY.mjs'
Invoke-WebRequest -UseBasicParsing -Uri ($RawBase + '/relay-context.json') -OutFile $ContextPath
Invoke-WebRequest -UseBasicParsing -Uri ($RawBase + '/FORM-VEXRELAY.mjs') -OutFile $FormPath

$Gh = $null
foreach ($Candidate in @(
    'C:\Program Files\GitHub CLI\gh.exe',
    'C:\Program Files (x86)\GitHub CLI\gh.exe'
)) {
    if (Test-Path -LiteralPath $Candidate -PathType Leaf) { $Gh = $Candidate; break }
}
if (-not $Gh) {
    $Command = Get-Command gh -ErrorAction SilentlyContinue
    if ($Command) { $Gh = $Command.Source }
}
if (-not $Gh) { throw 'GITHUB_CLI_REQUIRED' }

& $Gh auth status --hostname github.com
if ($LASTEXITCODE -ne 0) { throw 'GITHUB_LOGIN_REQUIRED' }

$Git = $null
foreach ($Candidate in @(
    'C:\Program Files\Git\cmd\git.exe',
    'C:\Program Files\Git\bin\git.exe'
)) {
    if (Test-Path -LiteralPath $Candidate -PathType Leaf) { $Git = $Candidate; break }
}
if (-not $Git) {
    $Command = Get-Command git -ErrorAction SilentlyContinue
    if ($Command) { $Git = $Command.Source }
}
if (-not $Git) { throw 'GIT_REQUIRED' }

$SdkRoot = Join-Path $RunRoot 'sdk'
Write-Host 'VexRelay: acquiring exact accepted SDK source...'
& $Gh repo clone 'vgong24/Vextreme-SDK' $SdkRoot -- --no-checkout --origin origin
if ($LASTEXITCODE -ne 0) { throw 'SDK_CLONE_FAILED' }

& $Git -C $SdkRoot fetch --no-tags --depth=1 origin $SdkHead
if ($LASTEXITCODE -ne 0) { throw 'SDK_EXACT_HEAD_FETCH_FAILED' }
& $Git -C $SdkRoot checkout --detach $SdkHead
if ($LASTEXITCODE -ne 0) { throw 'SDK_EXACT_HEAD_CHECKOUT_FAILED' }

$ObservedHead = (& $Git -C $SdkRoot rev-parse HEAD).Trim()
$ObservedTree = (& $Git -C $SdkRoot rev-parse 'HEAD^{tree}').Trim()
if ($ObservedHead -ne $SdkHead) { throw "SDK_HEAD_MISMATCH:$ObservedHead" }
if ($ObservedTree -ne $SdkTree) { throw "SDK_TREE_MISMATCH:$ObservedTree" }

$ContractSpec = $SdkHead + ':docs/private-continuity/vexrelay/current-contract.json'
$ObservedContractBlob = (& $Git -C $SdkRoot rev-parse $ContractSpec).Trim()
if ($ObservedContractBlob -ne $ContractBlob) { throw "SDK_CONTRACT_BLOB_MISMATCH:$ObservedContractBlob" }

$Contract = Get-Content -LiteralPath (Join-Path $SdkRoot 'docs\private-continuity\vexrelay\current-contract.json') -Raw | ConvertFrom-Json
if ($Contract.contractRef -ne 'source.vexrelay.current-contract/v1') {
    throw "SDK_CONTRACT_REF_MISMATCH:$($Contract.contractRef)"
}

$ProfilePath = Join-Path $SdkRoot 'docs\private-continuity\vexlocalbridge\profiles\windows-victor-vextreme-sdk.json'
$Profile = Get-Content -LiteralPath $ProfilePath -Raw | ConvertFrom-Json

$Node = $null
foreach ($Candidate in @($Profile.executables.node)) {
    if (Test-Path -LiteralPath $Candidate -PathType Leaf) { $Node = $Candidate; break }
}
if (-not $Node) { throw 'QUALIFIED_NODE_UNAVAILABLE' }

$PowerShell = $null
foreach ($Candidate in @($Profile.executables.powershell)) {
    if (Test-Path -LiteralPath $Candidate -PathType Leaf) { $PowerShell = $Candidate; break }
}
if (-not $PowerShell) { throw 'QUALIFIED_POWERSHELL_UNAVAILABLE' }

Write-Host 'VexRelay: forming #243 ExactHeadSourceOwnerProof A002...'
$FormOutput = & $Node $FormPath $ContextPath $SdkRoot (Join-Path $HOME 'Downloads') 2>&1
$FormOutput | ForEach-Object { Write-Host ([string]$_) }

$Prefix = "VEXRELAY_FORMED`t"
$Pointer = @($FormOutput | Where-Object { ([string]$_).StartsWith($Prefix) })[-1]
if (-not $Pointer) { throw 'VEXRELAY_FORMATION_POINTER_MISSING' }
$Formed = ([string]$Pointer).Substring($Prefix.Length) | ConvertFrom-Json

if (-not (Test-Path -LiteralPath $Formed.outerZip -PathType Leaf)) {
    throw "VEXRELAY_OUTER_PACKAGE_MISSING:$($Formed.outerZip)"
}
$ObservedOuter = (Get-FileHash -LiteralPath $Formed.outerZip -Algorithm SHA256).Hash.ToLowerInvariant()
if ($ObservedOuter -ne $Formed.outerSha256) {
    throw "VEXRELAY_OUTER_DIGEST_MISMATCH:$ObservedOuter"
}

Write-Host "VexRelay: formed $($Formed.outerZip)"
Write-Host 'VexRelay: executing source-managed Windows VexLocalBridge carrier...'
$Runner = Join-Path $SdkRoot 'lib\vexlocalbridge\templates\RUN-VEX-TASK.ps1'
& $PowerShell -NoProfile -ExecutionPolicy Bypass -File $Runner -Package $Formed.outerZip
$Rc = $LASTEXITCODE
Write-Host "VexRelay A002 completed with exit code $Rc"
exit $Rc
