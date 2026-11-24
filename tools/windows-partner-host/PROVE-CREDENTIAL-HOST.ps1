#requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$Helper = Join-Path $RepoRoot "packaging\windows-partner-host\vexlife-partner-host.ps1"
if (-not (Test-Path -LiteralPath $Helper -PathType Leaf)) {
    throw "Windows Partner host helper source is missing: $Helper"
}

$PowerShell = Join-Path $env:SystemRoot "System32\WindowsPowerShell\v1.0\powershell.exe"
if (-not (Test-Path -LiteralPath $PowerShell -PathType Leaf)) {
    throw "Windows PowerShell 5.1 executable is unavailable"
}

$Schema = "vexlife-prod.partner-native-host/v1"
$ProviderRef = "provider.openai.api"
$CredentialRef = "credential.vexlife.partner.proof-" + ([Guid]::NewGuid().ToString("N").ToLowerInvariant())
$SyntheticSecret = [Text.Encoding]::UTF8.GetBytes("vexlife-partner-proof-" + ([Guid]::NewGuid().ToString("N")))
$Empty = [byte[]]@()

function Quote-ProcessArgument {
    param([Parameter(Mandatory = $true)][string]$Value)
    return '"' + $Value.Replace('"', '\"') + '"'
}

function Invoke-PartnerHelper {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Header,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][byte[]]$Payload,
        [int]$TimeoutSeconds = 60
    )

    $headerLine = ($Header | ConvertTo-Json -Compress -Depth 8) + [Environment]::NewLine
    $headerBytes = [Text.Encoding]::UTF8.GetBytes($headerLine)

    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $PowerShell
    $start.Arguments = "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File " + (Quote-ProcessArgument $Helper)
    $start.UseShellExecute = $false
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.CreateNoWindow = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $start
    if (-not $process.Start()) {
        throw "Windows Partner helper process could not start"
    }

    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()

    try {
        $stream = $process.StandardInput.BaseStream
        $stream.Write($headerBytes, 0, $headerBytes.Length)
        if ($Payload.Length -gt 0) {
            $stream.Write($Payload, 0, $Payload.Length)
        }
        $stream.Flush()
        $process.StandardInput.Close()

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try { $process.Kill() } catch {}
            throw "Windows Partner helper exceeded the proof timeout"
        }

        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()

        if ($process.ExitCode -ne 0) {
            throw "Windows Partner helper failed (exit $($process.ExitCode)): $stderr"
        }

        $lines = @($stdout -split "[\r\n]+" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($lines.Count -ne 1) {
            throw "credential proof operation expected one protocol event, observed $($lines.Count)"
        }
        return ($lines[0] | ConvertFrom-Json)
    } finally {
        [Array]::Clear($headerBytes, 0, $headerBytes.Length)
        $process.Dispose()
    }
}

function Store-ProofCredential {
    $header = @{
        schemaVersion = $Schema
        operation = "STORE"
        providerRef = $ProviderRef
        credentialRef = $CredentialRef
        secretBytes = $SyntheticSecret.Length
    }
    return Invoke-PartnerHelper -Header $header -Payload $SyntheticSecret
}

function Observe-ProofCredential {
    $header = @{
        schemaVersion = $Schema
        operation = "OBSERVE"
        providerRef = $ProviderRef
        credentialRef = $CredentialRef
    }
    return Invoke-PartnerHelper -Header $header -Payload $Empty
}

function Delete-ProofCredential {
    $header = @{
        schemaVersion = $Schema
        operation = "DELETE"
        providerRef = $ProviderRef
        credentialRef = $CredentialRef
    }
    return Invoke-PartnerHelper -Header $header -Payload $Empty
}

function Execute-MissingCredentialProof {
    $body = [Text.Encoding]::UTF8.GetBytes('{"model":"proof-only-no-network","stream":true}')
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $digest = (($sha.ComputeHash($body) | ForEach-Object { $_.ToString("x2") }) -join "")
    } finally {
        $sha.Dispose()
    }

    $header = @{
        schemaVersion = $Schema
        operation = "EXECUTE"
        providerRef = $ProviderRef
        credentialRef = $CredentialRef
        expectedRequestUrl = "https://api.openai.com/v1/responses"
        dialect = "OPENAI_RESPONSES"
        publicProtocolHeaders = @{
            "content-type" = "application/json"
            "accept" = "text/event-stream, application/json"
        }
        requestBodySha256 = $digest
        requestBodyBytes = $body.Length
        redirectPolicy = "DENY"
    }

    try {
        return Invoke-PartnerHelper -Header $header -Payload $body
    } finally {
        [Array]::Clear($body, 0, $body.Length)
    }
}

try {
    $store = Store-ProofCredential
    if ([string]$store.type -ne "STORED" -or -not [bool]$store.present) {
        throw "STORE did not prove protected credential presence"
    }
    $observePresent = Observe-ProofCredential
    if ([string]$observePresent.type -ne "OBSERVED" -or -not [bool]$observePresent.present) {
        throw "OBSERVE did not prove the stored credential is present"
    }

    $delete = Delete-ProofCredential
    if ([string]$delete.type -ne "DELETED" -or [bool]$delete.present) {
        throw "DELETE did not prove the credential is absent"
    }
    $observeAbsent = Observe-ProofCredential
    if ([string]$observeAbsent.type -ne "OBSERVED" -or [bool]$observeAbsent.present) {
        throw "OBSERVE after DELETE did not prove absence"
    }

    $missingExecute = Execute-MissingCredentialProof
    if ([string]$missingExecute.type -ne "TRANSPORT_FAILURE" -or
        [string]$missingExecute.failureCode -ne "CREDENTIAL_NOT_PRESENT") {
        throw "EXECUTE after DELETE did not fail at credential lookup before network"
    }

    $receipt = [ordered]@{
        schemaVersion = "vexlife-prod.partner-windows-host-proof/v1"
        truthClass = "WINDOWS_PROTECTED_HOST_DETERMINISTIC_PROOF"
        providerRef = $ProviderRef
        credentialRef = $CredentialRef
        store = "PASS"
        observePresent = "PASS"
        delete = "PASS"
        observeAbsent = "PASS"
        executeMissingCredentialBeforeNetwork = "PASS"
        realProviderCredentialUsed = $false
        providerNetworkEffectExpected = $false
        secretPersistedAfterProof = $false
    }

    $outDir = Join-Path $RepoRoot "build\windows-partner-host-proof"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $out = Join-Path $outDir "windows-partner-host-proof.json"
    $receipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $out -Encoding UTF8

    [Console]::Out.WriteLine("WINDOWS_PARTNER_HOST_PROOF_PASS " + $out)
} finally {
    try {
        [void](Delete-ProofCredential)
    } catch {
        [Console]::Error.WriteLine("Windows Partner proof cleanup could not confirm credential deletion.")
    }
    [Array]::Clear($SyntheticSecret, 0, $SyntheticSecret.Length)
}

# [VXG RealForever]
