[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$RelaySourceHead = '8681cee2b23b87d37e806851716b940e1e50f7aa'
$CandidateHead = '058514545ee55e18f5665d05effa70e293faa399'
$CandidateTree = '92f75c2a48016cc1efcedc9c494bf10217384e6f'
$CandidateBase = '470c02c9ac268118b0a5f5cf8b34261f096c9899'
$CandidateBranch = 'VXG-111826-chatgpt-windows-release-preview-a001'
$CandidateBaseBranch = 'VXG-111726-chatgpt-runtime-session-promotion-a001'
$SdkHead = '8543c7d755faba04d4c4019c02e84f992667918a'
$SdkTree = '6376f68279f0750ee6d3868b8472368199763064'
$ContractBlob = 'b53cbac01c2deeb45fad5c32ce98e0796c04126f'
$JdkUrl = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
$JdkSha256 = 'a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9'
$RawBase = "https://raw.githubusercontent.com/VexGPT/VexLife-Prod/$RelaySourceHead/relay/windows-release-preview/A002"
$BootstrapRoot = Join-Path $env:TEMP ('VXR-286A2-' + $CandidateHead.Substring(0,8))

if (Test-Path -LiteralPath $BootstrapRoot) {
    throw 'PR286_A002_BOOTSTRAP_ROOT_ALREADY_EXISTS_DO_NOT_REPLAY'
}
New-Item -ItemType Directory -Path $BootstrapRoot | Out-Null

$ContextPath = Join-Path $BootstrapRoot 'relay-context.json'
$FormPath = Join-Path $BootstrapRoot 'FORM-VEXRELAY.mjs'
Invoke-WebRequest -UseBasicParsing -Uri ($RawBase + '/relay-context.json') -OutFile $ContextPath
Invoke-WebRequest -UseBasicParsing -Uri ($RawBase + '/FORM-VEXRELAY.mjs') -OutFile $FormPath
$Context = Get-Content -LiteralPath $ContextPath -Raw | ConvertFrom-Json

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

$ProductRoot = Join-Path $BootstrapRoot 'p'
Write-Host 'VexLife #286 A002: acquiring exact Product candidate identity...'
& $Gh repo clone 'VexGPT/VexLife-Prod' $ProductRoot -- --no-checkout --origin origin
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CLONE_FAILED' }

& $Git -C $ProductRoot fetch --no-tags --depth=64 origin $CandidateBranch
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CANDIDATE_FETCH_FAILED' }
$ObservedCandidateHead = (& $Git -C $ProductRoot rev-parse FETCH_HEAD).Trim()
if ($ObservedCandidateHead -ne $CandidateHead) {
    throw "PRODUCT_CANDIDATE_HEAD_MISMATCH:$ObservedCandidateHead"
}
& $Git -C $ProductRoot checkout --detach $CandidateHead
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_CANDIDATE_CHECKOUT_FAILED' }

& $Git -C $ProductRoot fetch --no-tags --depth=64 origin $CandidateBaseBranch
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_BASE_FETCH_FAILED' }
$ObservedBase = (& $Git -C $ProductRoot rev-parse FETCH_HEAD).Trim()
if ($ObservedBase -ne $CandidateBase) {
    throw "PRODUCT_BASE_HEAD_MISMATCH:$ObservedBase"
}
& $Git -C $ProductRoot merge-base --is-ancestor $CandidateBase $CandidateHead
if ($LASTEXITCODE -ne 0) { throw 'PRODUCT_FIXED_BASE_NOT_ANCESTOR' }

$ObservedCandidateTree = (& $Git -C $ProductRoot rev-parse 'HEAD^{tree}').Trim()
if ($ObservedCandidateTree -ne $CandidateTree) {
    throw "PRODUCT_CANDIDATE_TREE_MISMATCH:$ObservedCandidateTree"
}
$ProductStatus = @(& $Git -C $ProductRoot status --porcelain=v1 --untracked-files=all)
if ($LASTEXITCODE -ne 0 -or @($ProductStatus | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }).Count -ne 0) {
    throw 'PRODUCT_CANDIDATE_NOT_CLEAN'
}
Write-Host "VexLife #286 A002: exact candidate tree $CandidateTree"

$SdkRoot = Join-Path $BootstrapRoot 's'
Write-Host 'VexLife #286 A002: acquiring exact accepted SDK Relay source...'
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
if ($ObservedContractBlob -ne $ContractBlob) {
    throw "SDK_CONTRACT_BLOB_MISMATCH:$ObservedContractBlob"
}
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

$CachedJdk = Join-Path $HOME 'Downloads\VEXLIFE-WINDOWS-4B-PATIENT-ZERO-CACHE\OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip'
$JdkZip = $null
if (Test-Path -LiteralPath $CachedJdk -PathType Leaf) {
    $ObservedCachedJdk = (Get-FileHash -LiteralPath $CachedJdk -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($ObservedCachedJdk -eq $JdkSha256) {
        $JdkZip = $CachedJdk
        Write-Host 'VexLife #286 A002: reusing exact cached Temurin JDK 21.'
    } else {
        throw "CACHED_JDK_SHA256_MISMATCH:$ObservedCachedJdk"
    }
}
if (-not $JdkZip) {
    $JdkZip = Join-Path $BootstrapRoot 'jdk21.zip'
    Write-Host 'VexLife #286 A002: acquiring exact pinned Temurin JDK 21 bytes...'
    Invoke-WebRequest -UseBasicParsing -Uri $JdkUrl -OutFile $JdkZip
    $ObservedDownloadedJdk = (Get-FileHash -LiteralPath $JdkZip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($ObservedDownloadedJdk -ne $JdkSha256) {
        throw "DOWNLOADED_JDK_SHA256_MISMATCH:$ObservedDownloadedJdk"
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
if (-not (Test-Path -LiteralPath $Java -PathType Leaf)) { throw 'JDK_JAVA_MISSING' }
if (-not (Test-Path -LiteralPath $Jpackage -PathType Leaf)) { throw 'JDK_JPACKAGE_MISSING' }

& $Java -version
if ($LASTEXITCODE -ne 0) { throw 'JDK_JAVA_VERSION_CHECK_FAILED' }

$PriorJavaHome = $env:JAVA_HOME
$PriorPath = $env:Path
$PriorGradleOpts = $env:GRADLE_OPTS
$env:JAVA_HOME = $JavaHome
$env:Path = (Join-Path $JavaHome 'bin') + ';' + $PriorPath
$env:GRADLE_OPTS = '-Dorg.gradle.jvmargs=-Xmx2048m -Dorg.gradle.workers.max=1 -Dorg.gradle.parallel=false -Dkotlin.compiler.execution.strategy=in-process'

$ResultZip = $null
$ResultDigest = $null
$DownloadsReturn = $null
$PreviewZip = $null
$PreviewDigest = $null
$PreviewExe = $null
$Rc = 1
try {
    $OuterOutput = Join-Path $HOME 'Downloads'

    Write-Host 'VexLife #286 A002: forming corrected ExactHeadSourceOwnerProof from the qualified Downloads input root...'
    $FormOutput = & $Node $FormPath $ContextPath $SdkRoot $OuterOutput 2>&1
    $FormOutput | ForEach-Object { Write-Host ([string]$_) }

    $Prefix = 'VEXRELAY_FORMED' + [char]9
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
    $CapsuleRoot = $Formed.outerZip.Substring(0, $Formed.outerZip.Length - 4) + '-run-' + $Formed.outerSha256.Substring(0,16)
    $Cli = Join-Path $SdkRoot 'lib\vexlocalbridge\cli.js'
    $StderrPath = Join-Path $BootstrapRoot 'vlb-stderr.log'

    Write-Host 'VexLife #286 A002: executing source-managed Windows carrier...'
    $PriorErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $JsonLines = & $Node $Cli run --package $Formed.outerZip --profile $ProfilePath --capsule-root $CapsuleRoot 2> $StderrPath
        $Rc = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $PriorErrorActionPreference
    }

    if (Test-Path -LiteralPath $StderrPath -PathType Leaf) {
        Get-Content -LiteralPath $StderrPath | ForEach-Object {
            if (-not [string]::IsNullOrWhiteSpace([string]$_)) {
                [Console]::Error.WriteLine([string]$_)
            }
        }
    }

    $ResultText = (@($JsonLines) | ForEach-Object { [string]$_ }) -join [Environment]::NewLine
    if ([string]::IsNullOrWhiteSpace($ResultText)) {
        throw "VEXLOCALBRIDGE_RESULT_POINTER_MISSING:exit=$Rc"
    }
    try {
        $Result = $ResultText | ConvertFrom-Json
    } catch {
        throw "VEXLOCALBRIDGE_RESULT_JSON_INVALID:exit=$Rc"
    }

    if (-not $Result.resultZip -or -not $Result.resultZipSha256) {
        throw "VEXLOCALBRIDGE_RESULT_ZIP_BINDING_MISSING:exit=$Rc"
    }
    $ResultZip = [IO.Path]::GetFullPath([string]$Result.resultZip)
    $ResultDigest = ([string]$Result.resultZipSha256).ToLowerInvariant()
    if (-not (Test-Path -LiteralPath $ResultZip -PathType Leaf)) {
        throw "VEXLOCALBRIDGE_RESULT_ZIP_MISSING:$ResultZip"
    }
    $ObservedResult = (Get-FileHash -LiteralPath $ResultZip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($ObservedResult -ne $ResultDigest) {
        throw "VEXLOCALBRIDGE_RESULT_ZIP_DIGEST_MISMATCH:$ObservedResult"
    }

    $OriginalName = [IO.Path]::GetFileName($ResultZip)
    $DigestSuffix = '--sha256-' + $ResultDigest + '.zip'
    if (-not $OriginalName.ToLowerInvariant().EndsWith($DigestSuffix)) {
        throw "VEXLOCALBRIDGE_RESULT_NAME_NOT_DIGEST_BOUND:$OriginalName"
    }
    $OriginalPrefix = $OriginalName.Substring(0, $OriginalName.Length - $DigestSuffix.Length)
    $MaxFilenameChars = [int]$Context.internalPathPolicy.finalDownloadsFilenameMaxChars
    $MaxPrefixChars = $MaxFilenameChars - $DigestSuffix.Length
    if ($MaxPrefixChars -lt 1) { throw 'DOWNLOADS_RETURN_FILENAME_POLICY_INVALID' }
    $VisiblePrefix = $OriginalPrefix
    if ($VisiblePrefix.Length -gt $MaxPrefixChars) {
        $VisiblePrefix = $VisiblePrefix.Substring(0, $MaxPrefixChars)
    }
    $DownloadsName = $VisiblePrefix + $DigestSuffix
    $DownloadsReturn = Join-Path (Join-Path $HOME 'Downloads') $DownloadsName

    if (Test-Path -LiteralPath $DownloadsReturn -PathType Leaf) {
        $ExistingDigest = (Get-FileHash -LiteralPath $DownloadsReturn -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($ExistingDigest -ne $ResultDigest) {
            throw "DOWNLOADS_RETURN_COLLISION:$DownloadsReturn"
        }
    } else {
        Copy-Item -LiteralPath $ResultZip -Destination $DownloadsReturn
    }
    $CopiedDigest = (Get-FileHash -LiteralPath $DownloadsReturn -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($CopiedDigest -ne $ResultDigest) {
        throw "DOWNLOADS_RETURN_COPY_DIGEST_MISMATCH:$CopiedDigest"
    }

    Write-Host ''
    Write-Host "VexLife #286 A002 disposition: $($Result.disposition)"
    Write-Host "CANONICAL_RETURN_DOWNLOADS=$DownloadsReturn"
    Write-Host "CANONICAL_RETURN_SHA256=$ResultDigest"

    if ([string]$Result.disposition -eq 'PASS') {
        Write-Host ''
        Write-Host 'VexLife #286 A002: source proof passed; forming the exact lived Windows preview artifact...'
        $Builder = Join-Path $ProductRoot 'tools\windows-preview\FORM-WINDOWS-PREVIEW.ps1'
        if (-not (Test-Path -LiteralPath $Builder -PathType Leaf)) {
            throw 'WINDOWS_PREVIEW_BUILDER_MISSING'
        }

        $PowerShellExe = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
        $PreviewArgs = @(
            '-NoProfile',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            $Builder,
            '-OutputDirectory',
            (Join-Path $HOME 'Downloads')
        )
        $PreviewOutput = & $PowerShellExe @PreviewArgs 2>&1
        $PreviewRc = $LASTEXITCODE
        $PreviewOutput | ForEach-Object { Write-Host ([string]$_) }
        if ($PreviewRc -ne 0) {
            throw "WINDOWS_PREVIEW_FORMATION_FAILED:exit=$PreviewRc"
        }

        $ZipPrefix = 'VEXLIFE_WINDOWS_PREVIEW_ZIP='
        $ShaPrefix = 'VEXLIFE_WINDOWS_PREVIEW_ZIP_SHA256='
        $PreviewZipLine = @($PreviewOutput | Where-Object { ([string]$_).StartsWith($ZipPrefix) })[-1]
        $PreviewShaLine = @($PreviewOutput | Where-Object { ([string]$_).StartsWith($ShaPrefix) })[-1]
        if (-not $PreviewZipLine -or -not $PreviewShaLine) {
            throw 'WINDOWS_PREVIEW_FORMATION_POINTER_MISSING'
        }

        $PreviewZip = [IO.Path]::GetFullPath(([string]$PreviewZipLine).Substring($ZipPrefix.Length))
        $PreviewDigest = ([string]$PreviewShaLine).Substring($ShaPrefix.Length).ToLowerInvariant()
        if (-not (Test-Path -LiteralPath $PreviewZip -PathType Leaf)) {
            throw "WINDOWS_PREVIEW_ZIP_MISSING:$PreviewZip"
        }
        $ObservedPreviewDigest = (Get-FileHash -LiteralPath $PreviewZip -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($ObservedPreviewDigest -ne $PreviewDigest) {
            throw "WINDOWS_PREVIEW_ZIP_DIGEST_MISMATCH:$ObservedPreviewDigest"
        }

        $ExtractName =
            'VexLife-Windows-preview-' +
            $CandidateHead.Substring(0,8) +
            '-' +
            $PreviewDigest.Substring(0,12)
        $ExtractRoot = Join-Path (Join-Path $HOME 'Downloads') $ExtractName
        if (Test-Path -LiteralPath $ExtractRoot) {
            throw "WINDOWS_PREVIEW_EXTRACT_TARGET_ALREADY_EXISTS:$ExtractRoot"
        }
        Expand-Archive -LiteralPath $PreviewZip -DestinationPath $ExtractRoot
        $PreviewExe = Join-Path $ExtractRoot 'VexLife\VexLife.exe'
        if (-not (Test-Path -LiteralPath $PreviewExe -PathType Leaf)) {
            throw "WINDOWS_PREVIEW_EXTRACTED_EXE_MISSING:$PreviewExe"
        }

        Write-Host ''
        Write-Host "VEXLIFE_PREVIEW_READY=$PreviewExe"
        Write-Host "VEXLIFE_PREVIEW_ZIP=$PreviewZip"
        Write-Host "VEXLIFE_PREVIEW_ZIP_SHA256=$PreviewDigest"

        try {
            Start-Process -FilePath $PreviewExe | Out-Null
            Write-Host 'VEXLIFE_PREVIEW_LAUNCH=STARTED'
            Write-Host 'The exact VexLife Windows preview is launching now.'
        } catch {
            Write-Host 'VEXLIFE_PREVIEW_LAUNCH=FAILED'
            try {
                & 'C:\Windows\explorer.exe' ("/select," + $PreviewExe) | Out-Null
            } catch {
                Write-Host 'Explorer selection was also unavailable; the exact preview EXE path is printed above.'
            }
            throw
        }
    }
} finally {
    $env:JAVA_HOME = $PriorJavaHome
    $env:Path = $PriorPath
    $env:GRADLE_OPTS = $PriorGradleOpts
}

if ($ResultZip -and $ResultDigest) {
    try {
        Remove-Item -LiteralPath $BootstrapRoot -Recurse -Force
    } catch {
        Write-Host 'PR286 A002 bootstrap cleanup was non-terminal; retained Downloads artifacts remain exact.'
    }
}

exit $Rc
