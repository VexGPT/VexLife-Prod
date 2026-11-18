# Temporary Relay carriage for PR #275 — A001

[VXG RealForever]

## Purpose

Prove that Settings model-file truth follows the exact resolved runtime realization.

Exact Product candidate:

~~~text
base 6558814c20b0d0836f949f5ee876b5a3cccaa0a5
head 3ea6e27d87773399d37b765895366e933937abdf
tree c7b4b4cc0e7637e31506eeb5c7219f118b9713b9
changed paths 7
~~~

## Relationship under proof

~~~text
selected model
  -> exact HereticRuntimeRealization
  -> exact model-artifact owner
       observe
       acquire model bytes
       remove model bytes
~~~

Current target behavior:

~~~text
Mac ARM64
  -> existing HereticArtifactMaterialization

Windows x64
  -> Windows GGUF model artifact
  -> model-only acquire/remove
  -> llama.cpp runtime archive/image untouched by Models Controls
~~~

Focused proof:
1. checkVexLifeStrings
2. Models Family / Models / Mac artifact regression / realization artifact seam / Windows materialization JVM tests
3. compileKotlinJs
4. exact seven-path review snapshot

No GitHub Actions. This temporary PR owns proof carriage only and must remain unmerged.

<!-- [VXG RealForever][PR275][TEMP-RELAY][A001] -->
