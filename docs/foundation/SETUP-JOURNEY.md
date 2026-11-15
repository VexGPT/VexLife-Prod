# Setup Journey — native Product-entry manifestation

`[VXG RealForever]`

## Human question

Can a non-technical person on a Mac start VexLife and, at every step, understand what VexLife found,
what it is about to do, what it needs from them and their Mac, and what stays unchanged — before any
material effect happens?

The three questions the journey must always answer from fresh observation:

~~~text
1. Do I already have VexLife?            -> Home location + Product-service ownership + model presence
2. Where does my Vex Home live?          -> human-chosen coordinate (~/.vexlife is a suggestion)
3. Can this Mac and drive run it?        -> certified host profile + free space on the Home's volume
~~~

## Lineage

This foundation composes existing roads; it is not a new semantic owner.

~~~text
vgong24/VexLife main            Home chosen before classification, state + admitted actions,
                                held noncanonical Home, host eligibility before first setup,
                                currentness re-check before effects, progress + Cancel safely

VexLife-Prod PR #107            portable Entry Family (State / Control / Effect / DisplayState),
                                Home formation separate from acquisition, selected Home carried
                                to the service, fresh observation after every effect

VexWork #302 SETUP-JOURNEY.md   journey rail is a projection, not a ledger; explain before
                                effect; permissions only when a capability proves them
~~~

Topology is unchanged (AGENTS.md startup dependency law): same bootstrap, same AppKit window, same
`bin/vexlife-prod-setup` process, same service lifetime owner, same ports. No new long-lived process.

## Causal loop

~~~text
window: --setup-language --home ~/.vexlife --system-language <Mac's preferred>   (no effect)
   language = saved in Home -> Mac's preferred -> en; window words in that language
   │ not yet chosen
   ▼
window: choose language (each in its own name) -> --choose-language <code> --home <p>
   saved only into an existing canonical Home; otherwise carried by the window
   │
   ▼
window: choose Home coordinate (suggest ~/.vexlife | type | Choose Folder…)
   │
   ▼
--inspect-front-door --home <selected> --language <code>   (no effect)
   VexLifeProdHomeFiles.observeLocation           absent | empty | canonical | HELD_*
   VexLifeProdCurrentState                        host, model selection, artifact presence, free space
   VexLifeProdMacServiceLifetime.observeExisting  running owned | stopped | not ready | unknown
   Mac adapter -> Entry State -> Family DisplayState -> VexLifeProdMacSetupCard
   │
   ▼
window renders card; returns CONTROL:<ref> | CHOOSE_HOME | CHOOSE_LANGUAGE | RECHECK | CLOSE
   │ CONTROL
   ▼
--activate-front-door-control <ref> --home <selected> --language <code>
   fresh re-observation -> Family.resolve      not lawful now -> STATE_CHANGED, nothing realized
   typed Effect -> rightful owner realizes it  (progress lines on stderr; Cancel safely = SIGTERM)
   fresh observation                           CURRENT -> OPEN_PRODUCT (+ ready notice)
                                               otherwise -> REFRESH_ENTRY (+ failure, if any)
   the chosen language is saved into the Home once it exists (after FORM_HOME)
   │
   └──► window inspects again (never predicts the successor State)
~~~

Every word on a card, failure, progress line, ready notice and in the window's own chrome comes from
the keyed `setup` string scope in the selected language; the Family's DisplayState names text by key
and the card carries each element's key next to its words
([LOCALIZATION.md](LOCALIZATION.md)). The window writes English only for the two messages it shows
when the setup program cannot run at all.

## Entry States

| State | Observed when | Lawful Control → Effect | Rail (Mac · Home · Model · Start · Ready) |
|---|---|---|---|
| `HOME_FORMATION_REQUIRED` | selected coordinate absent or an empty folder; host eligible | Create Vex Home → `FORM_CANONICAL_HOME` | ✓ ● ○ ○ ○ |
| `HOME_HELD` | coordinate exists but is not a usable Home (other files, invalid records, file, link, not writable, unreadable) | none | ✓ ⊘ ○ ○ ○ |
| `HOST_HELD` | certified host profile not met and no owned service is running | none | ⊘ ✓/○ ○ ○ ○ |
| `SETUP_REQUIRED` | Home canonical; model unconfigured, partially/not downloaded, or runtime environment absent; space sufficient or unknown | Set Up VexLife → `CONVERGE_PRODUCT(acquire, start)` | ✓ ✓ ● ○ ○ |
| `STORAGE_HELD` | acquisition needed and observed free space < requirement | none | ✓ ✓ ⊘ ○ ○ |
| `MODEL_FILES_HELD` | an existing model file fails exact size | none | ✓ ✓ ⊘ ○ ○ |
| `START_REQUIRED` | everything present; service absent, or owned receipt with launchd absent (e.g. after restart) | Start VexLife → `CONVERGE_PRODUCT(start only)` | ✓ ✓ ✓ ● ○ |
| `REPAIR_REQUIRED` / `UPDATE_REPAIR_REQUIRED` | owned service loaded but not ready; everything present | Repair/Update & Restart → `CONVERGE_PRODUCT(start only)` | ✓ ✓ ✓ ● ○ |
| `UPDATE_AVAILABLE` | owned service running an earlier exact source | Update & Restart → `CONVERGE_PRODUCT(start only)`; Open Current → `OPEN_PRESENTATION` | ✓ ✓ ✓ ● ○ |
| `CURRENT` | owned service running the desired source | Open VexLife → `OPEN_PRESENTATION` | ✓ ✓ ✓ ✓ ● |
| `HOLD_UNKNOWN` | service ownership cannot be proven (incl. another Home's service), or the Home changed between two reads | none | ✓ ✓ ○ ⊘ ○ |

Ordering is ownership-first: unknown service ownership is held before host/model questions; a running
owned service is openable regardless of host observation.

## Permanent distinctions

~~~text
ACQUISITION AUTHORITY   = may download / create missing model + runtime prerequisites
RUNTIME-START AUTHORITY = may start the exact local runtime from prerequisites already present
ACQUISITION -> START;  START -/-> ACQUISITION
SETUP_REQUIRED is the only Entry State that grants acquisition.

HOME SUGGESTION != FORCED HOME
EMPTY FOLDER     = formable (atomic rename onto the empty directory; ENOTEMPTY fails closed)
FOLDER WITH FILES = preserved; never overwritten, repaired or deleted

STOPPED (launchd absent) != BROKEN (loaded, not answering)

UNKNOWN FREE SPACE != INSUFFICIENT FREE SPACE
PROGRESS LINE      != COMPLETION
EFFECT EXCEPTION   -> bounded explanation + fresh observation, never a predicted State
OFFERED CONTROL    != LAWFUL NOW   (re-observed inside activation)
~~~

## Per-screen ownership (handoff §31)

| Question | Answer for every card |
|---|---|
| Who supplied the State? | Mac adapter (`VexLifeProdMacFrontDoor.observeSemantic`) from Home, host, model, service owners; Family names it |
| Facts observed | location disposition, chip/memory, free space on the Home's volume, model selection + presence by size, runtime-environment directory presence, service receipt/launchd/endpoint |
| Presentation only | rail, "What VexLife found", "What setup will do", "Up next", heads-up notices |
| Lawful Controls | exactly `Family.act(state, control) != null` (tested for every State) |
| Control → Effect | `VexLifeProdEntryFamily.act` only |
| What changes outside | Home formation: one folder; Setup: model files, private Python/MLX, Product image, launchd service; Start/Repair: runtime + service processes |
| Who realizes it | `VexLifeProdSetupProcess` → `VexLifeProdLocalSetup` / installer / `VexLifeProdMacServiceLifetime` |
| What proves it | fresh `observe` after the Effect; `OPEN_PRODUCT` only when that observation is `CURRENT` |
| If reality is not the hoped-for successor | `REFRESH_ENTRY` renders the actual State |
| What is preserved | completed Home, verified model files, `.partial` downloads (resume), existing folders |
| What Cancel means | before an Effect: nothing happens. During an Effect: *Cancel safely* terminates only the task this window started; finished work stays |
| Accidental authorization? | none: Home formation grants no acquisition; Start/Repair/Update never acquire; held States admit nothing |

## Model step (2026-11-14)

~~~text
language -> Home -> model -> card (Set Up / Start / ...)
~~~

`VexLifeProdSetupModel` mirrors the language step. Before a Home exists the window holds the model
handle and passes `--model <handle>` to every inspection and activation; once the Home exists the
choice is saved into the Home's model configuration (`--choose-model`, and again around any
activation so a Home formed by Set Up holds it before the Product converges). A Home that already
chose is not asked again; the card offers **Model…** (`CHOOSE_MODEL`, renderer-local) whenever this
Mac can run VexLife, and its "Found" section names the model the setup is about. The plan's
download size and memory need are that model's own.

Settings → Models (web workspace, `VexLifeModelsFamily` + `VexLifeModels`, `GET /api/v1/models`,
`POST /api/v1/models/control`) lists every offered model with its size, memory need and fit, and
offers Use / Download / Download again / Remove by the Family's lawfulness. Downloads run in the
background under the Home's setup lease, one at a time. Use saves the choice; the switch happens
at the next Setup Start, because the service process holds no runtime-start authority.

## Requirements shown before effects

- **This Mac:** certified Heretic host profile (`HereticMacHostProfile`: darwin/arm64, Apple M4 Pro,
  ≥ 12 GiB). Shown with the observed chip and memory.
- **Free space:** `HereticSetupStorage` = missing model bytes + 3 GiB runtime allowance (managed
  Python, mlx-lm environment, uv cache, Product image, headroom). The allowance is a conservative source
  estimate pending lived measurement. Previewed at Home formation ("Up next"), enforced before
  acquisition.
- **Internet:** stated in the plan; failures resume from verified partial bytes.

## Permissions

Source-proven today: the core first-run path with a Home under the user folder (default `~/.vexlife`)
requests no macOS privacy permission.

Explained risk: a Home inside a macOS-protected area (Desktop, Documents, Downloads, iCloud Drive,
cloud-storage folders, `/Volumes`) can trigger Files & Folders prompts for the setup app and can block
the launchd background service later. The card names the area before Home formation. Lived evidence:
the Downloads prompt seen during PR #107 proof came from proof artifacts in Downloads, not from a
Product requirement.

Browser / Vision permissions stay out of first run (VexWork #302 step 6).

## Held / not yet earned

- exact identification of *which* other Home owns a running service (needs a no-effect owner fact);
- runtime environment present but invalid (presence is by directory; convergence fails with a bounded
  explanation);
- explicit repair-with-reacquisition consent (predecessor parity) for damaged model files / environment;
- hash-invalid but size-valid model files are found only during convergence;
- host profile memory minimum (12 GiB) is below the 14.1 GiB weight size; the chip gate (M4 Pro ships
  ≥ 24 GB) dominates today, but widening the chip gate requires recertifying memory;
- start-at-login / return path other than re-running the bootstrap.

## Proof boundary

Source: Family + adapter + card + activation + Home-location + progress tests (JVM), common/JS compile;
every State resolves in all ten languages with distinct button words; element keys map to the
Controls and actions they carry.
Not proven here: AppleScript compilation, AppKit rendering, real NSTask/launchd/model effects on a Mac.
See `docs/work/claude-setup-journey/03-LIVED-PROOF-PLAN.md`.

<!-- [VXG RealForever][VEXLIFE-PROD][SETUP-JOURNEY] -->
