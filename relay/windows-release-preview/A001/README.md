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
4. the actual source-owned `FORM-WINDOWS-PREVIEW.ps1` against the exact candidate;
5. exact five-path review.

After canonical SourceOwner PASS, the outer bootstrap reuses the same exact candidate/JDK and copies a
digest-bound preview ZIP to Downloads, then extracts/reveals the exact `VexLife.exe` for Victor's lived
test.

No GitHub Actions. This temporary PR is proof carriage only and must remain unmerged.

<!-- [VXG RealForever][PR286][TEMP-RELAY][A001] -->
