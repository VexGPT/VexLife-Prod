# Stage 0 — Independent analysis of the three startup roads

`[VXG RealForever]` · work notes, not authority · Claude · 2026-11-13

Sources read:

~~~text
A  vgong24/VexLife main 6946f935  setup-vexlife.command, install/vexlife-setup-window.applescript,
                                  install/vexlife-setup.sh, scripts/macos-lifecycle.mjs
B  VexGPT/VexLife-Prod PR #107    head f58890f (base main ab7b71e): Entry Family, Mac front door,
                                  setup process, LocalSetup, CurrentState, service lifetime, Home
                                  files, native window, bootstrap template
C  VexGPT/VexWork PR #302         projects/vexlife-new-beginnings/SETUP-JOURNEY.md (8a67cf7)
   Vextreme VexArchitecture pages (first stable body, live demonstration)
~~~

## What B preserved, improved, and regressed

| Behavior | A (predecessor) | B (PR #107 head) | Verdict |
|---|---|---|---|
| Home chosen before classification | text chooser, `~/.vexlife` default | chooser + exact coordinate through launchd argv | preserved |
| Empty existing folder as Home | `ABSENT` → first setup | `observeExisting` throws → stack trace dialog | **regressed** |
| Folder with other files | `HELD_NONCANONICAL_HOME`, preserved | throws → stack trace dialog | **regressed** |
| Host eligibility before first setup | `HOST_ELIGIBILITY_HELD` at inspect | observed in CurrentState, never gates; effect throws mid-way | **regressed** |
| Free space | not checked (Windows profile only) | observed + displayed, never compared | gap in both |
| Home formation vs acquisition | two consents, one bundled backend effect | two separate Effects, re-observe between | **improved** |
| State + admitted actions from backend | `VEXLIFE_CONTROLLER_STATE/ACTIONS` | typed Family + DisplayState | **improved** |
| Re-check before mutation | inspect again under setup reservation | `resolveTypedControl` re-observes | preserved |
| Unlawful-now Control | `fail` with message | `requireNotNull` → exception → stack trace | partial |
| Returning after reboot | `EXISTING_RECOVERABLE` → "can reconnect" → Open | receipt survives, launchd absent → `REPAIR_REQUIRED`; converge with `allowAcquisition=false` → runtime availability `ObservationOnly` → "runtime receipt process is not live" | **regressed** (source trace) |
| Progress + Cancel during long effects | NSPanel, live `[VexLife]` line, Cancel safely | none; window polls a single pipe | **regressed** |
| stdout/stderr | separate temp files | one pipe, read only after exit | **regressed** (deadlock / JSON corruption risk) |
| Null in controls | n/a | fixed at f58890f (`NSNull` fallback) | fixed |
| Process ownership fail-closed | exact receipts | exact receipts, `HOLD_UNKNOWN` | preserved |
| Per-Home setup coordination | `/tmp` session dir + child pid | `VexLifeProdLocalSetupLease` file lock | preserved (lease failure surfaced raw) |

## Answers to the handoff's ten questions

**1. Which A behaviors should be preserved verbatim in meaning?**
Home chosen before classification; empty folder = first setup, non-empty unknown folder = preserved;
host eligibility held before any Home/acquisition effect; backend-admitted actions only; re-observe
before every mutation; exact process ownership and fail-closed on unknown; preservation-safe recovery;
per-Home setup coordination; visible progress with a safe cancel; "stopped" is reconnectable, not
broken.

**2. Which should be re-expressed rather than copied?**
The bash controller's string states and CSV actions become the typed Entry Family. The AppleScript's
per-state `if` ladder (A) and per-field DisplayState reading (B) become one Product-formed card the
window only renders. The `/tmp` session lock is already re-expressed as the lease. Predecessor's
bundled first-setup becomes two Effects (B's improvement kept).

**3. Did B introduce an unnecessary new owner/coordinator?**
No mega-owner. The Mac front door is a thin adapter and the Family is portable. Two smells: the
native window was slowly becoming a second presentation decision tree (reading
`primaryControlRef`/`secondaryControlRef`, composing summary text), and `HereticRuntimeAvailability`
conflates "may start the runtime" with "may acquire". This branch moves all copy and button existence
into `VexLifeProdMacSetupCard` and separates runtime-start authority at the `ConvergeProduct` /
`LocalSetup` boundary without renaming the runtime Family.

**4. Is EntryFamily's vocabulary rich enough for A's lifecycle truth?**
It was not: no held-Home, no host hold, no storage hold, and no "stopped but intact" state (A's
`EXISTING_RECOVERABLE`). Added `HOME_HELD`, `HOST_HELD`, `STORAGE_HELD`, `MODEL_FILES_HELD`,
`START_REQUIRED`. `REPAIR_REQUIRED` now means loaded-but-not-answering only.

**5. Should Product-entry State map A's states more closely?**
Map meaning, not names. A is Home-centric (`EXISTING_HEALTHY` …); Prod is service-centric (`CURRENT`
…). Mapping: `ABSENT`→`HOME_FORMATION_REQUIRED`; `HELD_NONCANONICAL_HOME`→`HOME_HELD`;
`HOST_ELIGIBILITY_HELD`→`HOST_HELD`; `EXISTING_HEALTHY`→`CURRENT`; `EXISTING_RECOVERABLE`→
`START_REQUIRED`; `EXISTING_DEGRADED_REPAIRABLE`→`REPAIR_REQUIRED` / `SETUP_REQUIRED` /
`MODEL_FILES_HELD` depending on what is actually missing. A's explicit repair-with-reacquisition consent
is not yet ported (held; see foundation).

**6. Does the selected Home stay exact through every owner?**
Yes on B, and this branch keeps it: window → `--home` → front door (normalized, asserted equal after
each observation) → Home location → CurrentState (asserted) → formation (same coordinate, incl. empty
folder rename-in-place) → LocalSetup → installer → service lifetime → launchd argv `--home`. New: if two
no-effect reads of the coordinate disagree, the journey holds instead of guessing.

**7. Is one stable service identity compatible with multiple Homes?**
Only one Home can be *served* at a time (one label `com.vexlife.prod.service`, one port 18210). That is
compatible with choosing among Homes as long as another Home's live service is never retargeted
silently. B already holds (`HOLD_UNKNOWN`, receipt is per Home). This branch makes that hold
understandable and offers *Choose Another Location…*. An explicit "switch served Home" relationship is
a separate future Control.

**8. Which permissions are Product requirements vs proof artifacts?**
Core first run with a Home under the user folder: none proven. The Downloads prompt was proof carriage
(artifacts in Downloads). Real Product risk found: a *Home* inside a TCC-protected area makes the setup
app and the launchd service need Files & Folders access; now explained before formation. Browser/Vision
permissions remain optional, later, capability-scoped.

**9. Can the native experience be one predictable journey without a second truth store?**
Yes: the rail is derived from Entry State on every inspect (tested: exactly one current/held step per
State), the window keeps only the selected coordinate between steps, and every step re-observes.

**10. Smallest path from A to the clean version?**
Keep B's substrate and topology; add the missing held/start States in the Family; classify the Home
coordinate as a value; split runtime-start from acquisition; move card copy into Kotlin; port A's
progress panel and file-handle process pattern into the same window. That is this branch. Then lived
proof on a Mac (03) before any further abstraction.

## Questions I asked myself while designing

- *Is an empty folder really safe to form into?* Yes: formation stages beside it and publishes with
  `rename(2)`, which replaces only an empty directory and fails with ENOTEMPTY if anything appeared.
- *Should low space block Home formation?* No — Home formation needs almost no space and is useful on
  its own. It is previewed ("Up next" + heads-up) and enforced before acquisition.
- *Should unknown free space hold?* No — unknown is not insufficient; delivery fails closed and resumes.
- *Is starting the runtime "acquisition"?* No — it downloads nothing. Treating it as acquisition is what
  broke the after-reboot path. Acquisition still implies start.
- *Should Repair be allowed to download?* Not in this branch (keeps B's law; missing things route to
  `SETUP_REQUIRED`). Explicit repair-with-reacquisition consent is listed as held.
- *Who writes the words?* Kotlin. The window authoring copy is how presentation drifts into semantics.

<!-- [VXG RealForever][VEXLIFE-PROD][SETUP-JOURNEY][ANALYSIS] -->
