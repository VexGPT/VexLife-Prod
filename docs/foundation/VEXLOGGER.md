# VexLogger: one chronology of cause and effect

`[VXG RealForever]`

## Human question

When something goes wrong on Victor's Mac, can any AI reconstruct what happened, in order, from one
file the Product already kept, without terminal archaeology across receipts, launchd, PIDs and logs?

## Law (ported from VexLife-Android `architecture/vexarchitecture/VEXLOGGER.md`)

~~~text
rightful owner accepts / observes a fact
-> the observing boundary reports it to VexLogger
-> VexLogger orders it
-> projections: Home chronology file, stderr, Debug & diagnostics, debug ZIP
~~~

- One root (`vexlife.logging.VexLogger`), one `Fact` vocabulary. No FamilyLogger, RuntimeLogger or
  BrowserLogger peers.
- The Logger owns no Family, Control, Effect, runtime, custody, navigation, presentation or provider
  meaning. It never decides anything from what it recorded.
- A fact is recorded when it happened (accepted or observed), never when expected:
  `EFFECT COMPLETED != PREDICTED SUCCESSOR STATE`, `PROCESS STARTED != PRESENTATION ARRIVED`.
- Failure isolation is absolute: `record` never throws; a failing sink is skipped.
- Refs are stable coordinates (state/control refs, source SHA, PID, turn ref, failure code), never
  human content. `detail` is bounded technical text.

## Where facts are recorded

| Boundary | Facts |
|---|---|
| Setup process | `ENTRY_STATE_OBSERVED`, `SETUP_CONTROL_ACCEPTED`, `SETUP_CONTROL_NOT_LAWFUL_NOW`, `SETUP_EFFECT_STARTED/COMPLETED/FAILED`, `PERSON_LANGUAGE_SAVED`, `PERSON_MODEL_CHOSEN` |
| Service process | `SERVICE_PROCESS_STARTED`, `SERVICE_LISTENING`, `SERVICE_START_FAILED`, `SERVICE_PROCESS_TERMINATING` (JVM shutdown hook; a hard kill leaves no record, which is evidence too) |
| Service lifetime owner | `SERVICE_RETIRED_VERIFIED`, `SERVICE_STARTED_CURRENT`, `SERVICE_REUSED_CURRENT` |
| Heretic runtime | `RUNTIME_CURRENTNESS_LOST`, `RUNTIME_RECOVERY_REQUESTED/COMPLETED/FAILED`, `RUNTIME_RETIRED_UNHEALTHY`, `RUNTIME_START_REQUESTED`, `RUNTIME_QUALIFIED`, `RUNTIME_MODEL_SWITCHED` (from/to handle), `RUNTIME_PROCESS_LIVE/NO_LONGER_LIVE` (PID observer, transitions only, never probes health) |
| Conversation | `SEND_ACCEPTED`, `PROVIDER_INVOCATION_REQUESTED`, `TURN_COMPLETED`, `TURN_FAILED` (with whether a failure receipt was written) |
| Web presentation (`POST /api/v1/chronology`, Browser/Presentation facts only) | `PRESENTATION_ARRIVED`, `SEND_OUTCOME_UNRESOLVED_SHOWN`, `BROWSER_PAGE_IDENTITY_OBSERVED`, `BROWSER_PAGE_UNLOCATED`, `BROWSER_CONTEXT_REQUESTED/READY/INVALIDATED` |
| Settings → Models | `MODEL_CONTROL_ACCEPTED`, `MODEL_DOWNLOAD_STARTED/COMPLETED/FAILED`, `MODEL_FILES_REMOVED` (files, freed bytes) |
| Capability bridge | `CAPABILITY_REQUEST_OBSERVED` (callRef, request number, handle, action), `CAPABILITY_RESULT_RETURNED` (state, tokens, truncated) — never the returned content |
| Diagnostics | `DEBUG_HISTORY_CLEARED`, `DEBUG_EXPORT_FORMED`, `ENHANCED_CAPTURE_STARTED/STOPPED`, `PROCESS_SNAPSHOT_OBSERVED` (periodic while enhanced and immediate on a terminal turn failure) |

## Storage

`<Home>/runtime/vexlife-prod/logs/chronology/chronology.jsonl`, one JSON record per line, appended
under an exclusive file lock (setup and service write concurrently), rotated at 2 MiB, at most 7
rotated files. Written only into an existing canonical Home; the Logger never creates one, and it is
the only thing a no-effect inspection may write.

## Settings → Debug & diagnostics (`VexLifeDiagnosticsFamily`)

~~~text
CORE CHRONOLOGY (always on) != ENHANCED CAPTURE (optional, 30 s snapshots, ends itself after 2 h)
TERMINAL TURN FAILURE
  -> immediate PROCESS_SNAPSHOT_OBSERVED
  -> runtime PID/liveness + service heap + host memory + Home-volume room at the failure boundary
  -> no human/model content
CLEAR DEBUG HISTORY removes chronology files + the enhanced-capture marker only;
  never Conversation, Memory, Home identity, service/runtime receipts or currentness evidence
EXPORT DEBUG ZIP (GET /api/v1/diagnostics/export):
  RESULT-SUMMARY.txt, manifest.json, chronology.jsonl, current/ (receipt summaries, runtime process,
    runtime-log-index), environment/, logs/ (bounded path-preserved tails), recovery/ (failure receipts)
  normal mode: no Conversation, prompts, model reasoning, page contents or tokens
  Enhanced Capture explicitly enabled:
    bounded in-memory last-30 provider stream traces
    reasoning tail <= 128 Ki chars, response tail <= 64 Ki chars per trace
    terminal turn/outcome/failure code + failure-time machine snapshot
    no human prompts, Conversation, Memory, page contents, model input, training or publication
~~~

The section's words are keyed in the `workspace` string scope
([LOCALIZATION.md](LOCALIZATION.md)); English is source, other languages come from the translation
cycle.


### Failure evidence must preserve the distinguishing surface

A debug export is useful only when it keeps the evidence that can distinguish plausible causes.

A lived 2026-11-16 repro exposed a concrete failure in the prior export shape: more than one runtime
root could contain `mlx-lm-server.err.log`, while ZIP formation used only `path.fileName`.
Two distinct logs therefore mapped to one archive entry and a later write could replace the current
runtime's stderr with an older same-named log.

Current law:

~~~text
RUNTIME LOG DISCOVERY
-> preserve Home-relative path in the export
-> preserve a runtime-log index (relative path / archive path / bytes / modifiedAt)
-> same basename in two runtime roots remains two artifacts

TURN_FAILED timestamp
-> capture machine state then
!= infer failure-time state from a later Export click
~~~

The logger still does not diagnose by itself. A low-memory snapshot is evidence of low memory at that
boundary, not proof that memory caused the provider failure. The exact runtime stderr, stream trace,
failure receipt and chronology remain separate evidence classes to correlate.

<!-- [VXG RealForever][VEXLIFE-PROD][VEXLOGGER] -->
