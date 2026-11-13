# Stage 1 — Semantic changes (Kotlin)

`[VXG RealForever]` · work notes · status: **source + tests green** (jvmTest 361/361, common/JS test compile)

## Files

| File | Change | Why |
|---|---|---|
| `commonMain/home/VexLifeProdHome.kt` | `LocationDisposition` vocabulary | Home owner names absent / empty / canonical / `HELD_*` as values |
| `jvmMain/home/VexLifeProdHomeFiles.kt` | `observeLocation()`; `bootstrap()` admits an empty folder | predecessor parity; no stack traces for ordinary folders |
| `commonMain/service/VexLifeProdServiceContract.kt` | identity-less dispositions accepted by `CurrentStateProjection` | held/empty Homes carry no identity; wire shape unchanged |
| `jvmMain/service/VexLifeProdCurrentState.kt` | uses `observeLocation`; free space from nearest existing ancestor | alternate Home under not-yet-created folders |
| `commonMain/entry/VexLifeProdEntryFamily.kt` | `START_REQUIRED`, `HOME_HELD`, `HOST_HELD`, `STORAGE_HELD`, `MODEL_FILES_HELD`; `Start` Control; `ConvergeProduct.allowRuntimeStart`; journey rail; home-choice / recheck labels | see foundation |
| `jvmMain/model/heretic/runtime/HereticSetupStorage.kt` | new, tiny | "may acquisition begin here without predictably running out of space?" |
| `jvmMain/local/VexLifeProdLocalSetup.kt` | `allowRuntimeStart`; progress phases; model byte progress | runtime start ≠ acquisition; progress chronology |
| `jvmMain/local/VexLifeProdSetupProgress.kt` | new | `[VexLife] ` lines, throttled; `Silent` for the service |
| `jvmMain/setup/VexLifeProdSetupProcess.kt` | carries runtime-start + progress | composition only |
| `jvmMain/setup/VexLifeProdMacFrontDoor.kt` | mapping order, `lawful` resolution, failure-safe activation, inspection-failure card, UTF-8 JSON channel | see below |
| `jvmMain/setup/VexLifeProdMacSetupCard.kt` | new | concrete presentation meaning for the native card |
| `jvmMain/setup/VexLifeProdSetupFailure.kt` | new | bounded human projection of an Effect that did not complete |

## Earned nouns (AGENTS.md "before creating a new abstraction")

- **`HereticSetupStorage`** — question no existing owner answered (free-space sufficiency before
  acquisition); 1 object, pure arithmetic; removing it puts the comparison in the adapter or renderer.
- **`VexLifeProdSetupProgress`** — chronology, not State. Needed because the predecessor's lived
  progress UX had no Prod source of facts; existing `onProgress` callbacks are reused.
- **`VexLifeProdMacSetupCard`** — "concrete presentation meaning" layer of the VexArchitecture
  display chain. Without it the AppleScript authors copy and decides button existence (the drift that
  produced the `msng` fake control).
- **`VexLifeProdSetupFailure`** — replaces stack-trace dialogs; never decides State.

No new Family, Store, Partner, Relay or Service.

## Authority split (the after-reboot fix)

~~~text
before:  allowAcquisition=false  -> HereticRuntimeAvailability ObservationOnly
         runtime not running after reboot -> "receipt process is not live" -> failure

after:   ConvergeProduct(allowAcquisition, allowRuntimeStart)
         Start / Repair / Update          -> (false, true)  start from present files, no download
         Set Up (SETUP_REQUIRED only)     -> (true,  true)
         service process (unchanged)      -> (false, false)
         acquisition without start        -> rejected at construction
~~~

`LocalSetup` maps `allowRuntimeStart` to the runtime availability product-owned mode; managed Python,
artifact materialization and environment creation still follow `allowAcquisition`.

## Mapping order (adapter)

1. Two no-effect reads of the coordinate (current-state disposition and location) must agree, else
   `HOLD_UNKNOWN` ("changed while VexLife was looking").
2. Formable → host eligible ? `HOME_FORMATION_REQUIRED` : `HOST_HELD`.
3. Held → `HOME_HELD`.
4. Canonical → service: unknown → `HOLD_UNKNOWN`; running owned → `CURRENT`/`UPDATE_AVAILABLE`.
5. Host ineligible → `HOST_HELD` (Home preserved).
6. `INVALID_SIZE` → `MODEL_FILES_HELD`.
7. Model not ready (unconfigured / not complete by size) or runtime environment absent →
   storage shortfall ? `STORAGE_HELD` : `SETUP_REQUIRED`.
8. Otherwise: service absent or receipt with launchd absent → `START_REQUIRED`; loaded but not ready →
   `REPAIR_REQUIRED` / `UPDATE_REPAIR_REQUIRED`.

## Tests added

`VexLifeProdSetupJourneyTest` (28): empty folder formable; folder with files held + untouched; file in
the way; nested missing folders counted and not created; alternate canonical Home recognized;
protected-location notice; low-space preview without blocking Home formation; unsupported Mac held
before any Home; unsupported Mac keeps existing Home; running service openable regardless of host
observation; storage hold numbers; unknown space not held; interrupted download resumes with remaining
bytes; invalid files held; stopped-after-restart = Start (start, no acquire); stopped prior version
says "newer version"; loaded-not-ready = Repair; not-ready + missing runtime = Setup; pending binding
with complete files = Start; other Home's service held with Choose Another Location; Home changed
between reads held; unlawful-now Control realizes nothing; failed Effect re-observed + explained;
Start opens only when re-observed CURRENT; non-CURRENT result refreshes; inspection failure card;
failure classification; progress prefix + bounded volume.

`VexLifeProdEntryFamilyTest` (+12): every State declared; exactly one current/held rail step;
displayed Controls == admitted Controls for every State; held States admit nothing; Start/Repair/
Update never acquire; acquisition without start rejected; only `SETUP_REQUIRED` acquires; home-choice
offered where it helps; host and storage copy carry the observed numbers; GiB formatting.

`VexLifeProdHomeFilesTest` (+3): every location case as a value, no folders created; empty folder
becomes the Home at the same coordinate with no staging sibling left; folder with files never formed
over.

<!-- [VXG RealForever][VEXLIFE-PROD][SETUP-JOURNEY][STAGE-1] -->
