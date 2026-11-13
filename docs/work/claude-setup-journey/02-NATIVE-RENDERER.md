# Stage 2 — Native renderer (AppKit via AppleScriptObjC)

`[VXG RealForever]` · work notes · status: **source written, structure-checked, NOT compiled** (no macOS here)

## Shape

~~~text
run
  chooseHome(~/.vexlife)                  NSAlert + text field; Continue | Choose Folder… | Cancel
  loop
    showEntry
      runProductEntry --inspect-front-door      (no progress panel; stdout/stderr to temp files)
      presentCard(card)                          NSAlert: title, rail + body, buttons in order
      CONTROL:<ref> -> activateControl(ref)
         runProductEntry --activate-front-door-control  (progress panel + Cancel safely)
         failure     -> explain -> Continue (re-inspect) | Close
         OPEN_PRODUCT-> open productOrigin -> ready notice -> done
         REFRESH     -> re-inspect
      CHOOSE_HOME -> chooseHome(current) -> re-inspect
      RECHECK     -> re-inspect
      CLOSE       -> quit
~~~

## What the window may decide

Only renderer-local navigation (choose another Home coordinate, check again, close) and how waiting is
shown. It does **not** read `displayState`, `primaryControlRef` or State refs, does not compose copy,
does not decide which buttons exist, and never maps a choice to an Effect (enforced by
`nativeWindowConsumesManifestationWithoutSemanticEffectRouting`).

## Ported from the predecessor (lived on Apple M4 Pro)

- `beginProgress` NSPanel: indeterminate indicator, selectable live line, **Cancel safely** wired to
  `performClose:`; closing the panel terminates only the task this window launched.
- `latestProgressLine`: last line starting with `[VexLife] ` from the stderr file.
- Process pattern: stdout and stderr each to their own temp file via `NSFileHandle`; run loop pumped
  while running; files read after exit and removed.
- `LSMultipleInstancesProhibited` + stable bundle id (bootstrap, unchanged).

Kept from PR #107: `launchAndReturnError:(reference)` / `JSONObjectWithData … |error|:(reference)`
bridge shapes, `NSNull` fallback in `dictionaryText` (plus `"msng"`), path standardization.

## New

- **Choose Folder…** opens `NSOpenPanel` (directories only, New Folder allowed, starts at the nearest
  existing ancestor) and returns to the chooser with the path filled in, so the person still confirms.
- Escape selects the last (Close/Cancel) button when it is not also the default.
- One completion notice after an Effect reaches `CURRENT` ("VexLife is ready …").

## Risks only a Mac can retire

1. `osacompile` acceptance of the new handlers (`|buttons|()`, `|count|()`, `|length|()`,
   `NSOpenPanel` URL `|path|()`, `character id 27`).
2. The card body is rendered as a wrapping-label **accessory view** (520 pt) with the rail as the
   informative text, so NSAlert keeps its wide left-aligned layout instead of the narrow centered
   column it uses for text-only alerts on macOS 11+. Confirm the layout and that a long body (setup
   plan + two notices) still fits a 13" display; if not, swap the label for a scroll view.
3. `task's terminate()` on the `exec`'d JVM leaves leases/partials as expected; re-inspect after cancel
   lands on `SETUP_REQUIRED` with fewer missing bytes.
4. Unicode (✓ ● ⊘ ○ …) survives: JSON is written as explicit UTF-8 and decoded as UTF-8.
5. JVM startup latency for `--inspect-front-door` (~1 s) with no spinner; acceptable or add a small
   "Checking…" panel.

<!-- [VXG RealForever][VEXLIFE-PROD][SETUP-JOURNEY][STAGE-2] -->
