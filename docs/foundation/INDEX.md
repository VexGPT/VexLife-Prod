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
| VexWeb relational authoring | Can humans/AI author the accepted Frame / Seam / Region relationships readably without introducing another spatial ontology or renderer meaning? | `docs/foundation/VEXWEB-SPATIAL-FOUNDATION.md`, `src/commonMain/kotlin/vexlife/web/VexWebAuthoring.kt`, `src/commonTest/kotlin/vexlife/web/VexWebAuthoringTest.kt` | SourceOwner A003 exact Kotlin/JVM + Kotlin/JS regression PASS; accepted tree `9d6a6096e065c4d6582b2da086237ddc5ed19d67` | **ACCEPTED** |
| VexWeb deterministic artifact generation | Can accepted VexWeb relationships deterministically produce source-addressable HTML/CSS/JS + a semantic manifest without generated syntax becoming source authority? | `docs/foundation/VEXWEB-ARTIFACT-GENERATION.md`, `src/commonMain/kotlin/vexlife/web/VexWebArtifacts.kt`, `src/commonTest/kotlin/vexlife/web/VexWebArtifactsTest.kt` | A002 full regression PASS + A003 compatibility-delta JVM/JS compile PASS; accepted tree `c20e92d48aab1296da52596691e898cad9cd6c21` | **ACCEPTED** |
| Generated P1 workspace | Can the intended inert VexLife workspace manifest every current Blueprint coordinate and materialize exact deterministic browser artifacts without gaining provider/runtime authority? | `docs/foundation/VEXWEB-GENERATED-P1-WORKSPACE.md`, `src/commonMain/kotlin/vexlife/web/VexLifeWebP1.kt`, `src/jvmMain/kotlin/vexlife/web/VexLifeWebP1ArtifactMaterializer.kt` | A001 production/materialization path PASS + A002 corrected test path PASS; accepted tree `4536d195b4b5156f7fbd6c922bac28a9c16b8f2a` | **ACCEPTED** |
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
| Private image install + bundled setup | Can a staged source-bound image perform explicitly-authorized first-run LocalSetup, install/reuse itself, and converge the exact service before readiness without making the setup process the lifecycle owner? | `docs/foundation/PRIVATE-IMAGE-INSTALL-SETUP.md`, `packaging/private-service-image/setup/`, `src/jvmMain/kotlin/vexlife/setup/`, `src/jvmMain/kotlin/vexlife/packaging/VexLifeProdPrivateServiceImageInstaller.kt` | v3 image + native setup-window source + Home-gated install + service-before-ready source/tests formed; exact Gradle/Kotlin + lived extracted-image/window proof pending | **SOURCE CANDIDATE · PROVIDER PUBLICATION / PRESENTATION HELD** |
| macOS product service lifetime | Given an exact installed private image, how does one stable launchd service reconcile exact source/image/process/HTTP currentness across reuse, restart and source rotation? | `docs/foundation/MAC-SERVICE-LIFETIME.md`, `src/jvmMain/kotlin/vexlife/service/VexLifeProdMacServiceLifetime.kt`, `src/jvmMain/kotlin/vexlife/platform/macos/MacUserLaunchdProcess.kt` | source/PID-bound HTTP currentness + launchd receipt v2 environment/plist currentness + start/reuse/rotation/unknown/timeout/receipt-failure tests source-formed; exact Kotlin + real macOS launchd/sustained proof pending | **SOURCE CANDIDATE · PRESENTATION / LIVED HOST HELD** |
| Native source-bound setup bootstrap | How can a fresh supported Mac obtain one exact private image and reach its native setup window without requiring Java/Kotlin/Gradle or making bootstrap the product installer/lifecycle owner? | `packaging/private-service-image/release-contract.json`, `packaging/bootstrap/`, `build.gradle.kts`, bootstrap section in `docs/foundation/PRIVATE-IMAGE-INSTALL-SETUP.md` | exact release-contract input + archive/bootstrap digest binding + private staging + AppKit handoff source-formed; exact Gradle generation + provider release + lived macOS proof pending | **SOURCE CANDIDATE · PROVIDER PUBLICATION / LIVED MAC HELD** |
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

accepted
-> F1 relational authoring over accepted spatial source
-> exact Kotlin/JVM + Kotlin/JS regression PASS
-> merge tree exact at c2ed37471eab9f163acd2b0cfaf979d7cdeead73

accepted
-> F2 deterministic HTML/CSS/JS + semantic-manifest generation
-> merge tree exact at cf7de23e2979cc49133e2de4c5eee841d5bcf584

accepted
-> generated P1 workspace + exact build artifact materialization
-> merge tree exact at 9615aab32c6e4427f3b538550284c391471a7eb0

current construction edge
-> reconcile the cumulative local-runtime/service/setup body onto accepted P1
-> keep runtime source byte-identical where ownership does not conflict
-> establish one integrated source tree before presentation/service binding

held
-> generated presentation -> local service binding
-> nested-browser provider / browser-context admission
~~~

## Architecture reference

Accepted VexArchitecture source lives on VexLife-Android `main` at
`66c866dea6a8dd78e2946da88715c4deb78c1847`.

PR #209 remains provenance for its pressure/reasoning history.

## Work truth

VexWork Project `project.vexlife.new-beginnings` owns work navigation. This repository owns product
source meaning.

<!-- [VXG RealForever][VEXLIFE-PROD][FOUNDATION-INDEX] -->
