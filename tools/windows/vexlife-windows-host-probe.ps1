# [VXG RealForever]
# Read-only Windows host architecture observation for VexLife-Prod routing experiments.
# This script performs no network, registry, Product, Home, model, service, or release mutation.

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Normalize-VexLifeArchitecture {
    param([Parameter(Mandatory = $true)][string]$Value)

    switch ($Value.Trim().ToUpperInvariant()) {
        "AMD64" { return "X86_64" }
        "X64" { return "X86_64" }
        "X86_64" { return "X86_64" }
        "ARM64" { return "ARM64" }
        "AARCH64" { return "ARM64" }
        default { return $null }
    }
}

$runtimeInformation = [System.Runtime.InteropServices.RuntimeInformation]
$isWindows = $runtimeInformation::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)

if (-not $isWindows) {
    throw "VEXLIFE_WINDOWS_HOST_PROBE_REQUIRES_WINDOWS"
}

$osArchitectureRaw = $runtimeInformation::OSArchitecture.ToString()
$processArchitectureRaw = $runtimeInformation::ProcessArchitecture.ToString()
$targetArchitecture = Normalize-VexLifeArchitecture -Value $osArchitectureRaw
$processArchitecture = Normalize-VexLifeArchitecture -Value $processArchitectureRaw

if ($null -eq $targetArchitecture) {
    throw ("VEXLIFE_WINDOWS_ARCHITECTURE_UNSUPPORTED:" + $osArchitectureRaw)
}

$routeRef =
    if ($targetArchitecture -eq "X86_64") {
        "route.vexlife-prod.windows.x86-64"
    } elseif ($targetArchitecture -eq "ARM64") {
        "route.vexlife-prod.windows.arm64"
    } else {
        throw ("VEXLIFE_WINDOWS_ROUTE_UNFORMED:" + $targetArchitecture)
    }

$report = [ordered]@{
    schemaVersion = "vexlife-prod.windows-host-probe/v1"
    truthClass = "READ_ONLY_WINDOWS_HOST_ARCHITECTURE_OBSERVATION"
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString("o")
    osPlatform = "WINDOWS"
    osArchitectureRaw = $osArchitectureRaw
    processArchitectureRaw = $processArchitectureRaw
    processorArchitecture = [Environment]::GetEnvironmentVariable("PROCESSOR_ARCHITECTURE")
    processorArchitectureW6432 = [Environment]::GetEnvironmentVariable("PROCESSOR_ARCHITEW6432")
    targetPlatform = "WINDOWS"
    targetArchitecture = $targetArchitecture
    processArchitecture = $processArchitecture
    routeRef = $routeRef
    productRealizationSupport = "NOT_ASSERTED_BY_HOST_PROBE"
    mutationPerformed = $false
}

$report | ConvertTo-Json -Depth 4 -Compress
