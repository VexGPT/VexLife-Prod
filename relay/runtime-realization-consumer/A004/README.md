# Temporary Relay carriage for corrected PR #252 — A004

[VXG RealForever]

A003 reached the real candidate and stopped in test compilation:

~~~text
HereticRuntimeRealizationTest.kt:290
HereticRuntimeRealizationTest.kt:302
Cannot access private HereticModelCatalog.GiB
~~~

The repair changed only the test: the GiB unit is now test-owned. No Product API was widened.

Exact corrected candidate:

~~~text
base 0ebc8a768b2bc356c1912b87d10fae70b4a3c9e6
head 0749d8e6e725ca687fa63c2a91a5dd9ace30fc8a
tree c49666de26832ba2e205c2131e432ecf01f30508
changed paths 10
~~~

A004 preserves:
- maxSelectedSourceEntries = 10
- stage.maxFiles = 10
- compact Windows path policy
- exact Temurin 21.0.13+9
- split Gradle proof commands
- one complete digest-bound outer RETURN ZIP in Downloads

No GitHub Actions. No replay of A003.

<!-- [VXG RealForever][PR252][TEMP-RELAY][A004][TEST-COMPILE-REPAIR] -->
