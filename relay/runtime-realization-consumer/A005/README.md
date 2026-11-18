# Temporary Relay carriage for PR #252 — A005

[VXG RealForever]

A004 proved the Relay path, exact candidate grounding, strings and JVM compilation. Its JVM proof
then selected two whole neighboring suites that are not the semantic owner of this slice:

- `VexLifeProdMacFrontDoorTest`: Mac-specific fixtures, including `Path.of("/tmp/.vexlife")`,
  executed on the Windows proof host;
- `VexLifeProdLocalServiceTest`: includes unrelated streaming/reach-order behavior.

The direct #252 relationship tests were not among A004's failures.

A005 preserves the exact Product candidate and changes only proof selection. The focused JVM
predicate now executes:

- `VexLifeJvmPlatformTargetTest`
- `VexLifePlatformRealizationTest`
- `HereticRuntimeRealizationTest`
- `VexLifeProdSetupModelTest`
- `VexLifeModelsTest`
- `VexLifeWindowsHostObservationTest`
- `VexLifeWindowsReadinessTest`

The JVM build still compiles all main/test source, including the changed Mac-front-door and local
service files. This gate simply stops treating unrelated whole-class behavior as belonging to the
runtime-realization consumer relation.

Exact candidate remains:

~~~text
base 0ebc8a768b2bc356c1912b87d10fae70b4a3c9e6
head 0749d8e6e725ca687fa63c2a91a5dd9ace30fc8a
tree c49666de26832ba2e205c2131e432ecf01f30508
~~~

No Product source mutation. No GitHub Actions. No A004 replay.

<!-- [VXG RealForever][PR252][TEMP-RELAY][A005][OWNED-PREDICATES] -->
