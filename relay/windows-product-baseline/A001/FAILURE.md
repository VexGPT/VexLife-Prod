[VXG RealForever]

# Windows Product source baseline — A001 failure

## Offered intent

Run current accepted VexLife-Prod source on Victor's real Windows host without Product effects:

```text
source = VexGPT/VexLife-Prod main @ 655e7c78cefba06a5e87afad285bb25a08632f05
checks:
  jvmTest
  compileKotlinJs
  compileTestKotlinJs
  checkVexLifeStrings
```

## Observed result

The attempt stopped inside the harness before source download/build execution.

```text
failureFamily = HARNESS_WINDOWS_HOST_OBSERVATION
message = The property 'OSArchitecture' cannot be found on this object.
Product source execution started = false
Product effects performed = false
Vex Home touched = false
model/runtime/service effects = false
```

## Root cause

The runner assigned the .NET `RuntimeInformation` type to a PowerShell variable and then used
variable-static-member syntax for `OSArchitecture`. That form is not a portable Windows PowerShell
contract and failed under Victor's host.

A second latent harness defect was found during review before replay:

```text
gradlew.bat
-> was about to be launched with native ProcessStartInfo semantics
-> Windows .bat/.cmd must cross the command-script boundary
-> accepted VexLocalBridge Windows source uses ComSpec/cmd.exe for this distinction
```

A002 corrects both harness defects. No Product code is changed to repair a harness failure.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS-BASELINE][A001][SAFE-FAILURE] -->
