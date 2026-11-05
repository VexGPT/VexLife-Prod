# VexLife Prod foundation index

`[VXG RealForever]`

This is the current architecture map. A foundation is added only when a recurring semantic owner,
construction invariant, or proof burden is earned.

| Foundation | Human question | Current source | Proof | State |
|---|---|---|---|---|
| Repository front door | Can a fresh worker understand why this repository exists and where to descend? | `README.md`, `AGENTS.md`, `docs/CULTURE.md`, this index | source review | SOURCE ENTRY |
| Semantic contract / Blueprint authoring | Can one VexLife relationship be compiler-visible instead of copied from examples? | `docs/foundation/SEMANTIC-CONTRACT-AUTHORING.md`, `src/commonMain/kotlin/vexlife/semantic/`, `src/commonMain/kotlin/vexlife/blueprint/` | accepted JVM/JS proof | **ACCEPTED** |
| Compose reference realization | Can the semantic contract render through Compose without Compose becoming semantic authority? | `docs/foundation/COMPOSE-REFERENCE-REALIZATION.md`, `reference/compose/` | accepted root JVM/JS + Compose JVM proof | **ACCEPTED** |
| VexWeb relational spatial foundation | Can browser manifestation be authored through stable relational geometry that scales without new vocabulary as region count/shape changes? | `docs/foundation/VEXWEB-COMPLETED-VIEW-BLUEPRINT.md`, `docs/foundation/VEXWEB-SPATIAL-FOUNDATION.md`, `src/commonMain/kotlin/vexlife/web/` | A002 JVM/JS compile + tests PASS; accepted tree `d109f0d648e1adeba35796b0dd0dc766d1dea4c1` | **ACCEPTED** |
| Durable contract JSON | Can durable product records share one strict JSON transport without every domain inventing parsers or losing unknown/default fields? | `docs/foundation/CONTRACT-JSON.md`, `src/commonMain/kotlin/vexlife/contract/`, `src/jvmMain/kotlin/vexlife/contract/`, `src/jvmMain/kotlin/vexlife/filesystem/` | strict JSON + atomic file + owned-path ancestry source/tests formed; exact Kotlin 2.4.20 JVM/JS proof pending | **INTEGRATED SOURCE CANDIDATE** |
| Isolated VexLife-Prod Home | Can fresh installs own one preservation-safe Home identity without depending on or silently mutating legacy `~/.vexlife`? | `docs/foundation/VEXLIFE-PROD-HOME.md`, `src/commonMain/kotlin/vexlife/home/` | common source/tests formed; exact Kotlin 2.4.20 JVM/JS + persistence/host proof pending | **INTEGRATED SOURCE CANDIDATE** |
| Heretic product model contract | Can the fresh product name its exact certified local model, artifact composition and Home-scoped selection without setup/runtime code owning that identity? | `docs/foundation/HERETIC-PRODUCT-MODEL.md`, `src/commonMain/kotlin/vexlife/model/heretic/` | disposable JVM/JS formation proof PASS; exact Kotlin 2.4.20 proof pending | **INTEGRATED SOURCE CANDIDATE** |
| Heretic artifact materialization | Can the certified Heretic catalog become exact local model bytes without setup/runtime layers owning integrity or acquisition/repair authority? | `docs/foundation/HERETIC-ARTIFACT-MATERIALIZATION.md`, `src/jvmMain/kotlin/vexlife/artifact/`, `src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticArtifactMaterialization.kt`, `HereticArtifactDelivery.kt` | resumable exact-delivery + partial recovery + invalid-final hold source/tests formed; exact Kotlin 2.4.20 + real acquisition proof pending | **INTEGRATED SOURCE CANDIDATE** |
| Heretic Python + MLX environment | Can the exact model runtime software be reused/created without setup or process code owning Python/MLX identity or caller-supplied bootstrap paths? | `docs/foundation/HERETIC-PYTHON-ENVIRONMENT.md`, `src/jvmMain/kotlin/vexlife/platform/python/ManagedPython.kt`, `src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPythonEnvironment.kt` | managed-Python + Heretic-environment source/tests formed; exact Kotlin 2.4.20 + real macOS Python/mlx-lm proof pending | **INTEGRATED SOURCE CANDIDATE** |
| macOS user process substrate | Can isolated Prod/Heretic processes have one exact construction/lifetime owner instead of copying launchctl/PID logic? | `docs/foundation/MACOS-USER-LAUNCHD-PROCESS.md`, `src/jvmMain/kotlin/vexlife/platform/macos/` | disposable JVM start/retire proof PASS; exact Kotlin 2.4.20 + real macOS launchd proof pending | **INTEGRATED SOURCE CANDIDATE** |
| Heretic persistent local runtime | Can exact model bytes + exact environment become one implementation-bound isolated runtime without hidden config or PID/port ownership shortcuts? | `docs/foundation/HERETIC-PERSISTENT-RUNTIME.md`, `src/jvmMain/kotlin/vexlife/model/heretic/runtime/`, `src/jvmTest/kotlin/vexlife/model/heretic/runtime/` | STARTING/QUALIFIED, packaged-class currentness, receipt/config transition source/tests formed; exact Kotlin 2.4.20 + real macOS/model proof pending | **INTEGRATED SOURCE CANDIDATE** |
| Isolated local setup composition | Can one ordinary fresh-user backend effect compose host eligibility -> Home -> model selection -> prerequisites -> qualified runtime without exposing hidden topology/config parameters? | `src/jvmMain/kotlin/vexlife/local/`, `src/jvmTest/kotlin/vexlife/local/` | source composition/idempotency/authorization + per-Home cross-process serialization tests formed; exact Kotlin 2.4.20 + real fresh Mac lived proof pending | **SOURCE CANDIDATE · HUMAN ENTRY HELD** |
| Direct personal Companion circuit | Can the isolated fresh product durably complete a canonical personal human↔Vex turn through its own qualified Heretic runtime while preserving bounded prompt and invocation evidence? | `docs/foundation/DIRECT-PERSONAL-COMPANION.md`, `src/commonMain/kotlin/vexlife/{companion,conversation}/`, `src/jvmMain/kotlin/vexlife/{companion,conversation}/` | source two-turn / duplicate / failure / privacy tests formed; exact Kotlin 2.4.20 + real Mac two-turn lived proof pending | **SOURCE CANDIDATE · MEMORY / PRESENTATION HELD** |
| Local product service | Can one fixed loopback service expose source currentness + canonical Conversation/Send to the eventual renderer without becoming a second Conversation/model/runtime owner? | `docs/foundation/LOCAL-PRODUCT-SERVICE.md`, `src/commonMain/kotlin/vexlife/service/`, `src/jvmMain/kotlin/vexlife/service/` | common contract + HTTP projection/Send/duplicate/runtime-currentness + no-acquisition service-process tests formed; exact Kotlin 2.4.20 + packaged/sustained Mac proof pending | **SOURCE CANDIDATE · PACKAGING / PRESENTATION / MAC LIFETIME HELD** |
| Private macOS service image | Can the source-bound service run on a fresh supported Mac without a preinstalled Java/Kotlin/Gradle environment or a second visible app? | `docs/foundation/PRIVATE-SERVICE-IMAGE.md`, `packaging/private-service-image/`, `src/jvmMain/kotlin/vexlife/packaging/`, `build.gradle.kts` | clean-Git source admission + derived KMP runtime classpath + jdeps/jlink image formation + strict image verifier/tamper tests source-formed; exact Gradle 9.6/Kotlin 2.4.20 + real macOS arm64 JDK21 image proof pending | **SOURCE CANDIDATE · DELIVERY / INSTALL / MAC LIFETIME HELD** |
| Private image install + bundled setup | Can a staged source-bound image perform explicitly-authorized first-run LocalSetup, then install/reuse itself under the established Prod Home without making shell bootstrap the installer? | `docs/foundation/PRIVATE-IMAGE-INSTALL-SETUP.md`, `src/jvmMain/kotlin/vexlife/setup/`, `src/jvmMain/kotlin/vexlife/packaging/VexLifeProdPrivateServiceImageInstaller.kt` | v2 dual-launcher image + setup sequencing + Home-gated atomic install/reuse/invalid-image hold + symlink-escape proofs source-formed; exact Gradle/Kotlin + lived extracted-image setup proof pending | **SOURCE CANDIDATE · BOOTSTRAP / MAC LIFETIME / PRESENTATION HELD** |
| Lived canonical Companion exploration threshold | Can the intended nested-browser shell converse through the canonical VexLife Conversation without becoming Conversation/model authority? | `exploration/web-workspace/` | 17/17 bounded machine proof through prompt-receipt projection + Victor lived Mac checkpoint | **EXPLORATION THRESHOLD** |
| Web-native VexLife workspace | Can browser-native output realize the same accepted relationships? | **not yet formed** | **not yet formed** | HELD |
| Public GitHub nested-browser practicum | Can bounded public Vex content render/navigate while preserving current identity? | **not yet formed** | **not yet formed** | HELD |
| Browser context / DOM projection | What current browser context may Vex perceive without gaining effect authority? | **not yet formed** | **not yet formed** | HELD |
| Conversation + Vex reintegration | How do canonical Conversation, model availability and browser context compose? | **not yet formed** | **not yet formed** | HELD |

## Current descent

~~~text
accepted
-> repository / semantic / Compose foundations

historical threshold retained
-> first F0 structural invariants
-> its binary spatial noun was later superseded by human scalability pressure

accepted foundation
-> Frame / Seam / Region / Allocation relational spatial vocabulary
-> same nouns proven at 2 / 3 / 4 regions
-> partial Seam proven for irregular three-region layout
-> geometry validity proven before merge

lived exploration threshold retained
-> hand-built nested browser + canonical Companion Conversation circuit
-> canonical prompt-materialization receipt is projected read-only for human inspection
-> page-context prompt admission + canonical CLEAN_START remain held at their rightful owners
-> does not replace accepted authoring/compiler sequence

current construction edge
-> authoring ergonomics over accepted relational spatial source

parallel fresh-local foundation candidate
-> isolated VexLife-Prod Home
-> one strict durable JSON + owned-path boundary
-> Heretic identity / certified artifact catalog / explicit PENDING -> BOUND configuration
-> private exact managed Python prerequisite
-> exact resumable artifact delivery/materialization
-> exact Python + mlx-lm environment
-> generic macOS process substrate
-> packaged-implementation-bound persistent Heretic runtime
-> thin VexLifeProdLocalSetup composition
-> human entry / presentation still held
-> all runtime effects remain held from acceptance until exact Kotlin 2.4.20 + real macOS proof

held
-> deterministic HTML/CSS/JS generator
~~~

## Architecture reference

VexLife-Android PR #209 / merge lineage around
`66c866dea6a8dd78e2946da88715c4deb78c1847` remains the accepted VexArchitecture origin/stability
provenance.

Current VexLife-Android `main` was re-grounded at
`305a09eac1fabebcf2241bd207386950f41121f2`; current source/tests own present behavior.

The Vextreme Builder Field Guide + Live Demonstration preserve the ownership/navigation law while
linked current source owns byte-exact implementation.

## Work truth

VexWork Project `project.vexlife.new-beginnings` owns work navigation. This repository owns product
source meaning.

<!-- [VXG RealForever][VEXLIFE-PROD][FOUNDATION-INDEX] -->
