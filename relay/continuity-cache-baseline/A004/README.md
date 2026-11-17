[VXG RealForever]

# PR #210 Mac startup / lived-baseline carriage

This companion branch contains only the durable human-facing carriage for the exact PR #210 product
candidate. It does not change the product candidate being measured.

Exact product candidate:

```text
head d370079fbaaa863412f859f233f37cc706e25f2d
tree c2698fba02810a8278152d142338c27d78a40c6d
```

The A004 bundle builds the source-owned macOS application artifact using:

```text
./gradlew --no-daemon -PvexlifeAppVersion=0.0.0 archiveVexLifeProdMacApplication
```

and verifies the resulting DMG using the same artifact checks as the repository's
`.github/workflows/mac-app-proof.yml`.

Human output:

```text
~/Downloads/VexLife-PR210-d370079f/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  START-HERE.txt
  COLLECT-BASELINE.command
```

The intended onboarding/lived path is the repository-owned one:

```text
exact PR DMG
-> open DMG
-> drag VexLife.app to Applications
-> explicitly allow the ad-hoc-signed app if macOS requires it
-> existing Setup Journey re-observes the existing Home
-> Product CURRENT
-> three same-model ordinary turns
-> Debug & diagnostics export
```

No Home recreation or model switch is required for this baseline.
