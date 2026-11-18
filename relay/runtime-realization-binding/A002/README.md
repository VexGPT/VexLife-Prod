# Temporary Relay carriage for PR #264 — A002

[VXG RealForever]

A001 grounded and compiled the exact Product candidate but selected three legacy Mac-oriented test
suites whose fixtures use Unix absolute paths such as `/tmp/.vexlife`. On the Windows proof JVM
those coordinates are not absolute, so the runtime/setup owners correctly rejected the fixtures
before their intended assertions.

The binding-specific suites were not among the failures.

A002 preserves the exact Product candidate and changes only proof selection.

Exact candidate:

~~~text
base 0749d8e6e725ca687fa63c2a91a5dd9ace30fc8a
head 3a0beeeeb1031317a7e85c0aa27b5e57d1e11d32
tree 4bcf2be0613c3a8eced6b14237887f0749933610
~~~

Focused JVM predicate:

- HereticProductModelTest
- HereticRuntimeAvailabilityTest
- FileHereticModelConfigurationStoreTest
- VexLifeWindowsHereticRuntimeTest
- VexLifeProdLocalSetupTest

JVM compilation still compiles all changed runtime sources. Strings, Kotlin/JS compile and exact
eight-path review remain unchanged.

No Product source mutation. No GitHub Actions. No A001 replay.

<!-- [VXG RealForever][PR264][TEMP-RELAY][A002][HOST-APPROPRIATE] -->
