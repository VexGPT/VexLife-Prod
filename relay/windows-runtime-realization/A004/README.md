# Temporary Relay carriage for PR #243 — A004

[VXG RealForever]

## Why A004 exists

A003 fixed the Windows path-envelope failure and proved the exact #243 repository head/tree were
grounded before the first Product proof command.

Returned evidence:

~~~text
outer VexLocalBridge RETURN
  sha256 b41c3df1d6dd4bc7f9cb24de7e55b07cc9e80e3c88ce2debb401388d7fc12d82
  transport PASS
  cleanup PASS

inner VexRelay RETURN
  sha256 eca76bc6d3c1c79311647ed051f6a3913780c3d02cf020251e6a6c300411d8d1
  disposition SAFE_FAILURE
  exact candidate head/tree PASS
  testsExecuted false
~~~

The first proof command reached `gradlew.bat`, which stopped because the sanitized Relay child had
no admitted Java runtime:

~~~text
UNACCEPTED_EXIT_CODE:9009
JAVA_HOME is not set
no java command could be found in PATH
~~~

This is a proof-harness dependency, not Product-source evidence.

## Reused Windows Java proof dependency

R015 already proved exact Temurin JDK 21 bytes on Victor's Windows host:

~~~text
Eclipse Temurin 21.0.13+9
OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip
sha256 a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9
~~~

A004:

1. prefers the exact R015 Downloads cache when present;
2. verifies the full archive SHA-256;
3. if the cache is absent, downloads only that exact pinned archive and verifies the same digest;
4. extracts it into the short A004 bootstrap root only;
5. sets JAVA_HOME and prepends the exact Java bin for the attempt;
6. admits only JAVA_HOME and bounded GRADLE_OPTS through the Relay command environment;
7. performs no persistent/global Java installation.

## Product candidate

PR #243 remains unchanged:

~~~text
head 2c54c81f3d318163ee5b09a6d35363af75c33c9d
tree 524bb1e8112c45f0455af8f2f6b81375cea67c93
base 5f0d95f49a5a20d021d2bc25e80ee4a1cb1460b0
~~~

The A003 compact internal-path policy and one top-level Downloads outer RETURN policy remain current.

No GitHub Actions. No Product source mutation. No merge/ready/approval effect.

<!-- [VXG RealForever][VEXLIFE-PROD][PR243][TEMP-RELAY][A004][JAVA-DEPENDENCY] -->
