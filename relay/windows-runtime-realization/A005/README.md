# Temporary Relay carriage for PR #243 — A005

[VXG RealForever]

## Why A005 exists

A004 successfully crossed the prior host boundaries:

~~~text
compact Windows path envelope  PASS
exact accepted SDK source      PASS
exact #243 repository grounding PASS
Temurin Java 21 admitted       PASS
Gradle launched                PASS
outer cleanup                  PASS
top-level Downloads RETURN     PASS
~~~

The inner Relay stopped only because the first Gradle command combined:

~~~text
jvmTest checkVexLifeStrings --tests ...
~~~

Gradle therefore applied the `--tests` option to `checkVexLifeStrings`, which does not own that
option:

~~~text
Problem configuring task :checkVexLifeStrings from command line.
Unknown command-line option '--tests'.
~~~

No Product test executed. This is classified as proof-harness Gradle argument routing, not candidate
source evidence.

## A005 proof decomposition

A005 preserves the same exact #243 candidate and splits the proof into three independent commands:

~~~text
1. checkVexLifeStrings

2. jvmTest
   --tests VexLifePlatformRealizationTest
   --tests VexLifeWindowsHostObservationTest
   --tests VexLifeWindowsReadinessTest
   --tests HereticRuntimeRealizationTest
   --tests VexLifeWindowsRuntimeMaterializationTest
   --tests VexLifeWindowsHereticRuntimeTest

3. compileKotlinJs
~~~

Each command independently receives the already-admitted exact Java 21 environment. A failure in one
command therefore names the owning proof boundary instead of leaking another task's arguments.

## Product candidate

~~~text
PR #243
head 2c54c81f3d318163ee5b09a6d35363af75c33c9d
tree 524bb1e8112c45f0455af8f2f6b81375cea67c93
base 5f0d95f49a5a20d021d2bc25e80ee4a1cb1460b0
~~~

The compact internal-path policy and one top-level Downloads outer RETURN policy remain current.

No GitHub Actions. No Product source mutation. No merge/ready/approval effect.

<!-- [VXG RealForever][VEXLIFE-PROD][PR243][TEMP-RELAY][A005][SPLIT-GRADLE-PROOF] -->
