# Temporary Relay carriage for PR #264 — A001

[VXG RealForever]

## Purpose

Prove the realization-aware Product model binding stacked on the already-proven #252 consumer slice.

Exact Product candidate:

~~~text
base 0749d8e6e725ca687fa63c2a91a5dd9ace30fc8a
head 3a0beeeeb1031317a7e85c0aa27b5e57d1e11d32
tree 4bcf2be0613c3a8eced6b14237887f0749933610
changed paths 8
~~~

## Relationship under proof

~~~text
selected model
  -> pending binding
     realizationRef = null
  -> exact qualified runtime evidence
     realizationRef = exact HereticRuntimeRealization
  -> bound Product model
     persisted realizationRef
     realization-owned artifact/runtime identity
~~~

The durable contract is explicitly v2. v1 remains a strict read-migration shape only. Legacy bound
Mac records may infer the exact current Mac realization in memory; read migration performs no Home
rewrite.

Focused JVM proof includes:
- HereticProductModelTest
- HereticRuntimeAvailabilityTest
- FileHereticModelConfigurationStoreTest
- HereticPersistentRuntimeTest
- StableVexLifeHereticRuntimeCompatibilityTest
- VexLifeWindowsHereticRuntimeTest
- VexLifeProdLocalSetupTest
- VexLifeProdSetupProcessTest

Carrier mechanics reuse the proven compact Windows path, exact Temurin Java, split Gradle commands,
and top-level digest-bound Downloads RETURN.

No GitHub Actions. This temporary PR owns proof carriage only and must remain unmerged.

<!-- [VXG RealForever][PR264][TEMP-RELAY][A001] -->
