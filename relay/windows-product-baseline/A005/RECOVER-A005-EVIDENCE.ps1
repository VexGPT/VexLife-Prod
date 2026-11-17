# [VXG RealForever]
[CmdletBinding()]
param([Parameter(Mandatory=$false)][string]$OutputDirectory="$HOME\Downloads")

$ErrorActionPreference="Stop"
Set-StrictMode -Version Latest
$NL=[Environment]::NewLine
$SourceCommit="655e7c78cefba06a5e87afad285bb25a08632f05"
$Capsule=Join-Path $env:TEMP ("vexlife-prod-windows-baseline-a005-"+$SourceCommit.Substring(0,12))

function Get-LowerSha256 {
  param([Parameter(Mandatory=$true)][string]$Path)
  return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Write-Utf8NoBom {
  param([Parameter(Mandatory=$true)][string]$Path,[Parameter(Mandatory=$true)][string]$Text)
  $utf8=New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($Path,$Text,$utf8)
}

if(-not(Test-Path -LiteralPath $Capsule -PathType Container)){
  throw ("A005_CAPSULE_NOT_FOUND:"+$Capsule)
}

$logsRoot=Join-Path $Capsule "logs"
$sourceZip=Join-Path $Capsule "source.zip"
$jdkArchive=Join-Path $Capsule "OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip"
$returnRoot=Join-Path $Capsule "recovered-return"

if(Test-Path -LiteralPath $returnRoot){Remove-Item -LiteralPath $returnRoot -Recurse -Force}
New-Item -ItemType Directory -Path $returnRoot|Out-Null
New-Item -ItemType Directory -Path $OutputDirectory -Force|Out-Null

$logFiles=@()
if(Test-Path -LiteralPath $logsRoot -PathType Container){
  $logFiles=@(Get-ChildItem -LiteralPath $logsRoot -File|Sort-Object Name)
  foreach($file in $logFiles){
    Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $returnRoot $file.Name)
  }
}

$checkLog=Join-Path $logsRoot "checkVexLifeStrings.log"
$tail=@()
if(Test-Path -LiteralPath $checkLog -PathType Leaf){
  $tail=@(Get-Content -LiteralPath $checkLog -Tail 250)
}

$sourceRootName=$null
$sourceRoot=Join-Path $Capsule "source"
if(Test-Path -LiteralPath $sourceRoot -PathType Container){
  $roots=@(Get-ChildItem -LiteralPath $sourceRoot -Directory)
  if($roots.Count -eq 1){$sourceRootName=$roots[0].Name}
}

$evidence=[ordered]@{
  schemaVersion="vexlife-prod.windows-baseline-a005-recovery/v1"
  continuity="[VXG RealForever]"
  recoveryOnly=$true
  replayPerformed=$false
  sourceCommit=$SourceCommit
  capsule=$Capsule
  sourceZipPresent=(Test-Path -LiteralPath $sourceZip -PathType Leaf)
  sourceZipSha256=if(Test-Path -LiteralPath $sourceZip -PathType Leaf){Get-LowerSha256 $sourceZip}else{$null}
  sourceRootName=$sourceRootName
  jdkArchivePresent=(Test-Path -LiteralPath $jdkArchive -PathType Leaf)
  jdkArchiveSha256=if(Test-Path -LiteralPath $jdkArchive -PathType Leaf){Get-LowerSha256 $jdkArchive}else{$null}
  recoveredLogs=@($logFiles|ForEach-Object{$_.Name})
  checkVexLifeStringsLogPresent=(Test-Path -LiteralPath $checkLog -PathType Leaf)
  effects=[ordered]@{
    gradleReplay=$false
    productMutation=$false
    vexHomeMutation=$false
    modelWeightDownload=$false
    modelRuntimeLoad=$false
    productRuntimeStart=$false
    productServiceStart=$false
    installation=$false
    releasePublication=$false
  }
}

Write-Utf8NoBom -Path (Join-Path $returnRoot "recovery.json") -Text (($evidence|ConvertTo-Json -Depth 8)+$NL)

$summary=@(
  "VexLife-Prod Windows baseline A005 recovery"
  "replayPerformed=false"
  "capsule=$Capsule"
  "sourceZipSha256=$($evidence.sourceZipSha256)"
  "jdkArchiveSha256=$($evidence.jdkArchiveSha256)"
  "sourceRootName=$sourceRootName"
  "recoveredLogs=$($evidence.recoveredLogs -join ',')"
  ""
  "=== checkVexLifeStrings.log tail ==="
  ($tail -join $NL)
) -join $NL
Write-Utf8NoBom -Path (Join-Path $returnRoot "RESULT-SUMMARY.txt") -Text ($summary+$NL)

$inventory=@()
Get-ChildItem -LiteralPath $returnRoot -File|Sort-Object Name|ForEach-Object{
  $inventory+=[ordered]@{path=$_.Name;bytes=$_.Length;sha256=(Get-LowerSha256 $_.FullName)}
}
Write-Utf8NoBom -Path (Join-Path $returnRoot "inventory.json") -Text (($inventory|ConvertTo-Json -Depth 5)+$NL)

$tempZip=Join-Path $OutputDirectory "RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A005-RECOVERY.forming.zip"
if(Test-Path -LiteralPath $tempZip){Remove-Item -LiteralPath $tempZip -Force}
Compress-Archive -Path (Join-Path $returnRoot "*") -DestinationPath $tempZip -CompressionLevel Optimal
$returnSha=Get-LowerSha256 $tempZip
$finalZip=Join-Path $OutputDirectory ("RETURN-VEXLIFE-PROD-WINDOWS-BASELINE-A005-RECOVERY--sha256-"+$returnSha+".zip")
if(Test-Path -LiteralPath $finalZip){Remove-Item -LiteralPath $finalZip -Force}
Move-Item -LiteralPath $tempZip -Destination $finalZip

Write-Host ""
Write-Host "VEXLIFE_PROD_WINDOWS_BASELINE_A005_RECOVERY_RETURN"
Write-Host ("path="+$finalZip)
Write-Host ("sha256="+$returnSha)
Write-Host "replayPerformed=false"
Write-Host ""
Write-Host "Recovered checkVexLifeStrings tail:"
$tail|Select-Object -Last 80|ForEach-Object{Write-Host $_}
