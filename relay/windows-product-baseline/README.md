[VXG RealForever]

# Windows Product source baseline

This companion surface preserves the attempts used to answer one question before Windows Product
implementation expands:

> How much of current accepted VexLife-Prod already executes correctly on a real Windows x64 host?

## Attempts

- `A001/FAILURE.md` — harness SAFE_FAILURE before Product source execution.
- `A002/RUN-VEXLIFE-PROD-WINDOWS-BASELINE.ps1` — corrected source-managed runner.
- `A002/baseline-manifest.json` — exact source/check/effect boundary.

A002 runs only source/build checks against accepted main
`655e7c78cefba06a5e87afad285bb25a08632f05`.

It does not attempt the currently Mac-owned runtime/service/native-UI/package leaves.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][SOURCE-BASELINE] -->
