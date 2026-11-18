# Temporary Relay carriage for PR #286 — A001

[VXG RealForever]

## Purpose

Prove the source-bound Windows lived-preview candidate and its real Windows app-image formation before
Victor launches the resulting `VexLife.exe`.

Exact Product binding:

~~~text
base 470c02c9ac268118b0a5f5cf8b34261f096c9899
head 058514545ee55e18f5665d05effa70e293faa399
tree derived independently from the exact Git object on Victor's host before task formation
changed paths 5
~~~

## Relationship under proof

~~~text
#282 portable RuntimeSession / shared Product service
  -> Windows lived-preview entry
  -> exact VexLifeProdServiceProcess readiness
  -> source-owned JVM/dependency staging
  -> JDK 21 jdeps + jlink
  -> jpackage app-image
  -> VexLife.exe
~~~

The app image is intentionally a pre-installer artifact. It does not install a Windows Service,
configure auto-start, claim signing, or choose final EXE/MSI installer semantics.

A001 runs:
1. `checkVexLifeStrings`;
2. focused Windows preview / RuntimeSession / service-process JVM tests;
3. `compileKotlinJs`;
4. exact five-path review.

The accepted Windows bridge keeps a bounded outer payload window, so app-image formation is not
misclassified as a source predicate merely because packaging is slow. After canonical SourceOwner
PASS, the same one-command outer bootstrap reuses the exact candidate/JDK and executes the
source-owned `FORM-WINDOWS-PREVIEW.ps1`. It copies a digest-bound preview ZIP to Downloads, then
extracts/reveals the exact `VexLife.exe` for Victor's lived test.

No GitHub Actions. This temporary PR is proof carriage only and must remain unmerged.

<!-- [VXG RealForever][PR286][TEMP-RELAY][A001] -->
