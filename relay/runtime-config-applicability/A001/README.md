# Temporary Relay carriage for PR #269 — A001

[VXG RealForever]

## Purpose

Prove that runtime configuration belongs to an exact runtime realization, not merely to modelRef.

Exact Product candidate:

~~~text
base 3a0beeeeb1031317a7e85c0aa27b5e57d1e11d32
head 6558814c20b0d0836f949f5ee876b5a3cccaa0a5
tree 62d2a276e08030909ba9e9f8e6ee7246071ee8ff
changed paths 12
~~~

## Relationship under proof

~~~text
stored Home runtime setting
  + exact HereticRuntimeRealization
  -> Applicable(effective setting)
   | NotApplicable
~~~

Current exact meaning:

~~~text
Mac ARM64 / MLX_LM_DIRECT_RUNTIME
  -> runtime-config.vexlife-prod.heretic.mlx-prompt-cache.v1
  -> prompt-cache configuration applies

Windows x64 / LLAMA_CPP_OPENAI_RUNTIME
  -> no runtime-configuration profile
  -> MLX prompt-cache configuration does not apply
~~~

The Home setting remains stored. A target switch does not delete it. Non-applicable realizations
cannot project configuration Controls and cannot write the MLX setting.

The candidate has 12 changed paths. VexRelay's selected-source/review cap remains 10 as required;
all 12 paths remain exact candidate-bound, while the review snapshot may explicitly omit two source
bodies under the contract cap.

Focused proof:
1. checkVexLifeStrings
2. runtime configuration / realization / Models Family / store / Settings / LocalSetup / VexWeb JVM tests
3. compileKotlinJs
4. exact candidate review snapshot

No GitHub Actions. This temporary PR owns proof carriage only and must remain unmerged.

<!-- [VXG RealForever][PR269][TEMP-RELAY][A001] -->
