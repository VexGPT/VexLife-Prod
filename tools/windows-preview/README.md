# Windows lived release preview

[VXG RealForever]

## Why this exists

This is the pre-merge Windows counterpart to the current Mac DMG experience.

It forms a JDK 21 `jpackage --type app-image`, not a final installer. The app image contains a real
`VexLife.exe` and a private Java runtime so Victor can experience the actual Product-entry journey,
Windows local-model realization and browser VexLife on the real Windows x64 machine before final
installer, Windows-service, signing or merge assumptions are accepted.

```text
exact Product source
-> self-contained Windows app image
-> VexLife.exe
-> native Windows Setup Journey
     inspect/preserve selected Vex Home
     language + target-supported model choice
     Create Vex Home (identity only, when absent)
     re-observe
     Set Up VexLife (separate acquisition authority)
     exact GGUF + llama.cpp acquisition/reuse
     portable Windows RuntimeSession
-> shared VexLife Product service
-> canonical loopback VexLife presentation
```

The default selected Home is `%USERPROFILE%\.vexlife`. An existing canonical Home is preserved and
observed as it actually is; the preview does not fake a first-run state merely to show every card.

## Preview lifetime

For this lived preview, the human-facing EXE itself retains custody of the Product service and local
model runtime. Keep the VexLife window open while using the browser Product; closing it after setup
stops this preview's service/runtime.

This is intentional evidence gathering before choosing the final Windows background-service lifetime.

## Not claimed

- this is not a signed Windows installer;
- this does not install a Windows Service;
- this does not establish auto-start;
- this does not merge or supersede active Mac service-lifetime work;
- active Windows setup effects cannot yet be cancelled through a proved safe-cancel path; the window
  stays open while a setup step is running;
- Windows-specific setup wording is currently English while the existing shared setup vocabulary
  remains localized;
- this does not prove lived behavior until Victor runs the exact formed artifact.

A final Windows installer may later wrap an accepted app image, but lived evidence from this preview
should determine that lifetime/installer shape rather than guessing first.

## Build

The source-owned builder is `FORM-WINDOWS-PREVIEW.ps1`.

It requires a clean exact checkout on Windows x64 and one JDK 21 `JAVA_HOME`. It uses the
`stage-preview.init.gradle` adapter only to ask the existing Kotlin/Gradle owners for the exact JVM
jar and runtime dependency files; the preview does not keep a second dependency list.

The output is a SHA-256-bound ZIP containing the self-contained `VexLife` app image.

<!-- [VXG RealForever][WINDOWS][SETUP-EXPERIENCE][LIVED-BEFORE-MERGE] -->
