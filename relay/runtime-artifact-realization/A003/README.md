# Temporary Relay carriage for PR #275 — A003

[VXG RealForever]

A002 grounded the exact corrected Product candidate, passed strings, compiled all JVM source/tests,
and ran 30 focused tests. 29 passed.

The only failure was an unchanged artifact-security fixture:

~~~text
HereticArtifactMaterializationTest
  symlinkedModelsParentCannotRedirectArtifactAcquisition

Files.createSymbolicLink(...)
-> java.nio.file.FileSystemException on the Windows proof host
~~~

That fixture failed before the intended artifact-owner assertion ran. It depends on Windows host
symlink privilege/developer-mode support and is not an owned predicate of #275.

A003 preserves the exact Product candidate and removes only that legacy host-dependent class from the
Windows proof selection.

Exact candidate remains:

~~~text
base 6558814c20b0d0836f949f5ee876b5a3cccaa0a5
head 1b664c729fe1680ac8a39f09276ec12988b1d01b
tree f70dced304f86511159d4de30ae34a4128e062da
changed paths 8
~~~

Focused JVM gate:
- VexLifeModelsFamilyTest
- VexLifeModelsTest
- HereticRealizationArtifactMaterializationTest
- VexLifeWindowsRuntimeMaterializationTest

JVM compilation still compiles the unchanged Mac artifact owner and its tests. No Product source
mutation. No GitHub Actions. No A002 replay.

<!-- [VXG RealForever][PR275][TEMP-RELAY][A003][HOST-APPROPRIATE] -->
