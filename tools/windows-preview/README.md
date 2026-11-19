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

For this lived preview, the JVM application process still retains exact custody of the Product
service and local-model runtime, while the Product presentation is a separate native WebView2 child
window.

```text
Open VexLife
-> WebView2 Product presentation

close WebView2 window
-> CLOSE_PRESENTATION
-> return to the VexLife application window
-> Product service/runtime remain live

Quit VexLife
-> QUIT_CURRENT / QUIT_PRODUCT
-> exact RunningProduct.close()
-> Product service/runtime release
-> application exits
```

This intentionally proves the semantic separation before choosing whether a later Windows release
earns a separate background-service/tray lifetime. The WebView2 helper never owns Home, Product
state, model/runtime custody or Product Quit meaning.

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

It requires a clean exact checkout on Windows x64, one JDK 21 `JAVA_HOME`, and a .NET 10 SDK
for source formation. The .NET SDK publishes the WebView2 helper self-contained into the app image;
the lived app therefore does not require a separately installed .NET runtime.

The WebView2 SDK is pinned to stable `Microsoft.Web.WebView2 1.0.4314.35`. The first lived proof
uses Microsoft's Evergreen WebView2 runtime and holds if that browser runtime is absent; this source
does not pre-commit to bundling the much larger Fixed Version runtime before the real Windows
machine earns that requirement.

The `stage-preview.init.gradle` adapter still asks the existing Kotlin/Gradle owners for the exact
JVM jar and runtime dependency files; the preview does not keep a second Product dependency list.

The output is a SHA-256-bound ZIP containing the self-contained `VexLife` app image, private Java
runtime, and self-contained .NET WebView2 presentation helper.

<!-- [VXG RealForever][WINDOWS][SETUP-EXPERIENCE][LIVED-BEFORE-MERGE] -->
