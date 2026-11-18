# Windows lived release preview

[VXG RealForever]

## Why this exists

This is a pre-merge lived-proof surface for the portable Windows Product path established by PR #282.

It deliberately forms a JDK 21 `jpackage --type app-image`, not a final installer. The app image
contains a real `VexLife.exe` and a private Java runtime so Victor can experience the actual
VexLife Product service and browser presentation on the real Windows x64 machine before installer,
Windows-service, signing, or merge assumptions are accepted.

```text
exact #282 Product source
-> existing JVM jar + runtime dependencies
-> exact JDK 21 dependency/module derivation
-> private runtime image
-> jpackage Windows app image
-> VexLife.exe
-> canonical ~/.vexlife by default
-> existing VexLifeProdServiceProcess
-> portable Windows RuntimeSession
-> canonical loopback VexLife presentation
```

## Not claimed

- this is not a signed Windows installer;
- this does not install a Windows Service;
- this does not establish auto-start;
- this does not merge or supersede active Mac service-lifetime PR #281;
- this does not make a new Product/service semantic owner;
- this does not prove lived behavior until Victor runs the exact formed artifact.

A final Windows installer may later wrap an accepted app image, but lived evidence from this preview
should inform that decision rather than guessing the installer/lifetime shape first.

## Build

The source-owned builder is `FORM-WINDOWS-PREVIEW.ps1`.

It requires a clean exact checkout on Windows x64 and one JDK 21 `JAVA_HOME`. It uses the
`stage-preview.init.gradle` adapter only to ask the existing Kotlin/Gradle owners for the exact
JVM jar and runtime dependency files; the preview does not keep a second dependency list.

The output is a SHA-256-bound ZIP containing the self-contained `VexLife` app image.

<!-- [VXG RealForever][WINDOWS][RELEASE-PREVIEW][LIVED-BEFORE-MERGE] -->
