$ErrorActionPreference = 'Stop'

$SdkRepository = 'vgong24/Vextreme-SDK'
$TrustedSdkHead = '8543c7d755faba04d4c4019c02e84f992667918a'
$TrustedSdkTree = '6376f68279f0750ee6d3868b8472368199763064'
$CarrierHead = '36eb5c82becd97dcfcbc8fa67455a74d8540503a'
$CarrierTree = 'bea091ed67793931b838c7b412d9abce39a2e373'
$ProductRepository = 'VexGPT/VexLife-Prod'
$ProductBase = 'fdb9f77bbd9895d0ab6932f669091b46ea813e9b'
$ProductHead = 'd13e8e4a774aea60362491420ffeee2c07f91d1c'
$ProductTree = '98ba3219e49f722272c976171cb736ed19ae9ac9'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = [IO.Path]::GetFullPath((Join-Path $ScriptDir '..\..\..\..'))
$LivedRoot = Split-Path -Parent $ScriptDir
$FormTask = Join-Path $LivedRoot 'form-task.cjs'
$Payload = Join-Path $ScriptDir 'payload.mjs'
$Consumer = Join-Path $LivedRoot 'consume-return.cjs'
$Gate = Join-Path $LivedRoot 'GATE.json'

function Resolve-Executable {
    param([string[]]$Candidates)
    foreach ($Candidate in $Candidates) {
        if (Test-Path -LiteralPath $Candidate -PathType Leaf) {
            return [IO.Path]::GetFullPath($Candidate)
        }
    }
    throw ('qualified executable unavailable: ' + ($Candidates -join ', '))
}

$Git = Resolve-Executable @(
    'C:\Program Files\Git\cmd\git.exe',
    'C:\Program Files\Git\bin\git.exe'
)
$Gh = Resolve-Executable @('C:\Program Files\GitHub CLI\gh.exe')
$Node = Resolve-Executable @('C:\Program Files\nodejs\node.exe')
$PowerShell = Resolve-Executable @('C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe')

foreach ($Required in @($FormTask, $Payload, $Consumer, $Gate)) {
    if (-not (Test-Path -LiteralPath $Required -PathType Leaf)) {
        throw "Windows lived proof companion source missing: $Required"
    }
}

& $Gh auth status --hostname github.com *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Windows lived proof requires current GitHub CLI authentication.'
}

& $Git -C $RepoRoot merge-base --is-ancestor $ProductHead HEAD
if ($LASTEXITCODE -ne 0) {
    throw 'Windows lived proof companion is not descended from exact #396 head.'
}

$Downloads = Join-Path $env:USERPROFILE 'Downloads'
if (-not (Test-Path -LiteralPath $Downloads -PathType Container)) {
    throw "Downloads directory unavailable: $Downloads"
}
$Root = Join-Path $Downloads ('.vexlife-pr398-windows-a001.' + [Guid]::NewGuid().ToString('N'))
$TrustedSdkRoot = Join-Path $Root 'trusted-sdk'
$CarrierSdkRoot = Join-Path $Root 'carrier-sdk'
$ProductRoot = Join-Path $Root 'product'
$FormedRoot = Join-Path $Root 'formed'
$ProductBundle = Join-Path $Root 'vexlife-prod.bundle'
$ConsumedResult = Join-Path $Downloads 'VEXLIFE-PR398-APPLICATION-LIVED-WINDOWS-A001-CONSUMED.json'
$PreserveRoot = $false

New-Item -ItemType Directory -Path $Root -Force | Out-Null

function Git-Value {
    param(
        [string]$RootPath,
        [string[]]$Arguments
    )
    $Value = & $Git -C $RootPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ('Git identity command failed: ' + ($Arguments -join ' '))
    }
    return (($Value | Out-String).Trim())
}

try {
    Write-Host 'Windows lived proof: acquiring exact accepted SDK builder source ...'
    & $Gh repo clone $SdkRepository $TrustedSdkRoot -- --no-checkout
    if ($LASTEXITCODE -ne 0) { throw 'TRUSTED_SDK_CLONE_FAILED' }
    & $Git -C $TrustedSdkRoot checkout --detach $TrustedSdkHead
    if ($LASTEXITCODE -ne 0) { throw 'TRUSTED_SDK_CHECKOUT_FAILED' }

    Write-Host 'Windows lived proof: acquiring exact application-lived carrier source ...'
    & $Gh repo clone $SdkRepository $CarrierSdkRoot -- --no-checkout
    if ($LASTEXITCODE -ne 0) { throw 'CARRIER_SDK_CLONE_FAILED' }
    & $Git -C $CarrierSdkRoot checkout --detach $CarrierHead
    if ($LASTEXITCODE -ne 0) { throw 'CARRIER_SDK_CHECKOUT_FAILED' }

    Write-Host 'Windows lived proof: acquiring exact #396 Product candidate ...'
    & $Gh repo clone $ProductRepository $ProductRoot -- --no-checkout
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CLONE_FAILED' }
    & $Git -C $ProductRoot checkout --detach $ProductHead
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CHECKOUT_FAILED' }

    if ((Git-Value $TrustedSdkRoot @('rev-parse','HEAD')) -ne $TrustedSdkHead) {
        throw 'TRUSTED_SDK_HEAD_MISMATCH'
    }
    if ((Git-Value $TrustedSdkRoot @('rev-parse','HEAD^{tree}')) -ne $TrustedSdkTree) {
        throw 'TRUSTED_SDK_TREE_MISMATCH'
    }
    if ((Git-Value $CarrierSdkRoot @('rev-parse','HEAD')) -ne $CarrierHead) {
        throw 'CARRIER_HEAD_MISMATCH'
    }
    if ((Git-Value $CarrierSdkRoot @('rev-parse','HEAD^{tree}')) -ne $CarrierTree) {
        throw 'CARRIER_TREE_MISMATCH'
    }
    if ((Git-Value $ProductRoot @('rev-parse','HEAD')) -ne $ProductHead) {
        throw 'PRODUCT_HEAD_MISMATCH'
    }
    if ((Git-Value $ProductRoot @('rev-parse','HEAD^{tree}')) -ne $ProductTree) {
        throw 'PRODUCT_TREE_MISMATCH'
    }

    & $Git -C $ProductRoot merge-base --is-ancestor $ProductBase $ProductHead
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_BASE_NOT_ANCESTOR' }

    Write-Host 'Windows lived proof: forming immutable Product bundle ...'
    & $Git -C $ProductRoot update-ref refs/heads/vexlife-pr398-lived-windows-a001 $ProductHead
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_BUNDLE_REF_FAILED' }
    & $Git -C $ProductRoot bundle create $ProductBundle refs/heads/vexlife-pr398-lived-windows-a001
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_BUNDLE_CREATE_FAILED' }
    & $Git -C $ProductRoot bundle verify $ProductBundle
    if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_BUNDLE_VERIFY_FAILED' }

    New-Item -ItemType Directory -Path $FormedRoot -Force | Out-Null
    Write-Host 'Windows lived proof: checking source-owned gate and forming task ...'
    $FormArgs = @(
        $FormTask,
        '--trusted-sdk', $TrustedSdkRoot,
        '--carrier-sdk', $CarrierSdkRoot,
        '--payload', $Payload,
        '--product-bundle', $ProductBundle,
        '--output', $FormedRoot,
        '--host', 'windows-victor'
    )
    & $Node @FormArgs
    if ($LASTEXITCODE -ne 0) { throw 'APPLICATION_LIVED_TASK_FORMATION_HELD_OR_FAILED' }

    $Formation = Join-Path $FormedRoot 'FORMATION-windows.json'
    if (-not (Test-Path -LiteralPath $Formation -PathType Leaf)) {
        throw 'WINDOWS_LIVED_FORMATION_RECEIPT_MISSING'
    }
    $FormationData = Get-Content -LiteralPath $Formation -Raw | ConvertFrom-Json
    $Package = [IO.Path]::GetFullPath([string]$FormationData.packagePath)
    $Expected = [string]$FormationData.packageSha256
    if (-not (Test-Path -LiteralPath $Package -PathType Leaf)) {
        throw 'WINDOWS_LIVED_PACKAGE_MISSING'
    }
    $Observed = (Get-FileHash -LiteralPath $Package -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($Observed -ne $Expected) {
        throw 'WINDOWS_LIVED_PACKAGE_DIGEST_MISMATCH'
    }

    $PreserveRoot = $true
    $Launcher = Join-Path $CarrierSdkRoot 'lib\vexlocalbridge\templates\RUN-VEXLIFE-PROD-TASK.ps1'
    if (-not (Test-Path -LiteralPath $Launcher -PathType Leaf)) {
        throw 'WINDOWS_VEXLOCALBRIDGE_LAUNCHER_MISSING'
    }

    Write-Host ("Windows lived proof: entering VexLocalBridge with package sha256 " + $Expected)
    & $PowerShell -NoProfile -ExecutionPolicy Bypass -File $Launcher -Package $Package -ExpectedSha256 $Expected
    $CarrierStatus = $LASTEXITCODE
    Write-Host ("Windows lived proof: VexLocalBridge exit=" + $CarrierStatus)

    $CapsuleRoot = Join-Path $FormedRoot ('VLB-' + $Expected.Substring(0,16))
    $ResultRoot = Join-Path $CapsuleRoot 'results'
    if (-not (Test-Path -LiteralPath $ResultRoot -PathType Container)) {
        throw 'WINDOWS_LIVED_CANONICAL_RESULT_ROOT_MISSING'
    }

    $Returns = @(Get-ChildItem -LiteralPath $ResultRoot -Recurse -File -Filter 'RETURN-*.zip')
    if ($Returns.Count -ne 1) {
        throw ("WINDOWS_LIVED_EXPECTED_ONE_RETURN:" + $Returns.Count)
    }
    $ReturnZip = $Returns[0].FullName

    Remove-Item -LiteralPath $ConsumedResult -Force -ErrorAction SilentlyContinue
    $ConsumeArgs = @(
        $Consumer,
        '--sdk-root', $CarrierSdkRoot,
        '--host', 'windows-victor',
        '--task-zip', $Package,
        '--return-zip', $ReturnZip,
        '--output', $ConsumedResult
    )
    & $Node @ConsumeArgs
    if ($LASTEXITCODE -ne 0) { throw 'WINDOWS_LIVED_RETURN_CONSUMPTION_FAILED' }
    if (-not (Test-Path -LiteralPath $ConsumedResult -PathType Leaf)) {
        throw 'WINDOWS_LIVED_CONSUMED_RESULT_MISSING'
    }

    $Consumed = Get-Content -LiteralPath $ConsumedResult -Raw | ConvertFrom-Json
    Write-Host ("WINDOWS_APPLICATION_LIVED_PASS=" + [string]$Consumed.applicationLivedPass)
    Write-Host ("WINDOWS_PRODUCT_QUIT_LIFECYCLE_PASS=" + [string]$Consumed.productQuitLifecyclePass)
    Write-Host ("WINDOWS_CONSUMED_RESULT=" + $ConsumedResult)
    Get-Content -LiteralPath $ConsumedResult -Raw | Write-Host

    if ($Consumed.applicationLivedPass -eq $true -and $Consumed.productQuitLifecyclePass -eq $true) {
        $PreserveRoot = $false
        exit 0
    }
    exit 2
}
finally {
    if ($PreserveRoot) {
        Write-Warning ("Windows lived proof recovery root preserved: " + $Root)
    } else {
        Remove-Item -LiteralPath $Root -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# [VXG RealForever]
