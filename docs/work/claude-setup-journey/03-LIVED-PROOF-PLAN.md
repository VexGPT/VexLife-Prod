# Stage 3 — Lived proof plan (needs a Mac)

`[VXG RealForever]` · work notes · status: **not run**

Nothing in this branch is a front-door completion claim until these pass on a real Mac
(AGENTS.md Mac startup integrity §7). Every step uses **disposable Homes** under
`~/VexLifeProof/…`; none targets `~/.vexlife`.

## 0. Compile the window (seconds, no effects)

~~~bash
cd VexLife-Prod && git checkout VXG-111326-claude-setup-journey-a001
/usr/bin/osacompile -o /tmp/vexlife-journey-check.app \
  packaging/private-service-image/setup/vexlife-prod-setup-window.applescript && echo COMPILED
~~~

Fail here → fix syntax before anything else (Stage 2 risk 1).

## 1. Build the exact image and a runnable window

~~~bash
./gradlew jvmTest assembleVexLifeProdServiceImage          # clean HEAD, JDK 21
IMAGE="$(ls -d build/vexlife-prod-service-image/images/*/ | head -1)"
SHA="$(git rev-parse HEAD)"
APP="/tmp/VexLife Journey Proof.app"
rm -rf "$APP" && /usr/bin/osacompile -o "$APP" "$IMAGE/setup/vexlife-prod-setup-window.applescript"
/usr/bin/plutil -insert VexLifeProdExpectedSourceSha -string "$SHA" "$APP/Contents/Info.plist"
/usr/bin/plutil -insert VexLifeProdSetupLauncher -string "$IMAGE/bin/vexlife-prod-setup" "$APP/Contents/Info.plist"
/usr/bin/codesign --force --sign - "$APP" && /usr/bin/open -W "$APP"
~~~

(This mirrors the bootstrap's metadata binding; it is a proof route, not the product route.)

## 2. Tier A — observation only (safe with Victor's real Product running)

| # | Type in the chooser | Expect | Check |
|---|---|---|---|
| A1 | `~/VexLifeProof/a` (absent) | Create your Vex Home · rail `✓ ● ○ ○ ○` · "Up next" shows ≈14.1 GiB / ≈17.1 GiB | nothing created on disk |
| A2 | make `~/VexLifeProof/empty` empty, choose it via **Choose Folder…** | same card, "an empty folder VexLife can set up" | panel's New Folder works |
| A3 | a folder with any file in it | This folder is being preserved · `⊘` Home · buttons Choose / Check Again / Close | file untouched |
| A4 | a file path | preserved, "is a file" | — |
| A5 | `~/.vexlife` (real Home, real service running) | VexLife is running · Open VexLife | **do not** press other buttons |
| A6 | any disposable *canonical* Home while the real service runs | VexLife will not replace the current process · Choose Another Location | no process signalled |
| A7 | press **Choose Another Location…** then Cancel in the chooser | returns to the previous Home's card | — |

## 3. Tier B — Home formation on disposable coordinates

| # | Action | Expect after the refresh |
|---|---|---|
| B1 | A1 → Create Vex Home | progress panel briefly; then `SETUP_REQUIRED` (or `HOLD_UNKNOWN` if the real service runs — that is the observed truth, not an error) |
| B2 | A2 → Create Vex Home | the *same* folder becomes the Home (`ls -la ~/VexLifeProof/empty` shows `config/ devices/ runtime/ recovery/`), no `.empty.bootstrap-*` sibling |
| B3 | `~/VexLifeProof/x/y/z` → Create | two folders created, Home at `z` |
| B4 | `~/Documents/VexLifeProof` | heads-up names "your Documents folder"; note whether macOS prompts on Create |

## 4. Tier C — storage hold without filling the real disk

~~~bash
hdiutil create -size 2g -fs APFS -volname VexTiny /tmp/VexTiny.dmg && hdiutil attach /tmp/VexTiny.dmg
~~~

C1 choose `/Volumes/VexTiny/Home` → formation card with both heads-ups (external drive + space) →
Create → `STORAGE_HELD` with ≈17 GiB needed / ≈2 GiB free, buttons Choose / Check Again / Close.
Detach afterwards.

## 5. Tier D — effects that need the service label free (Victor decides when)

Only when no VexLife-Prod service is running for any Home (`launchctl print gui/$(id -u)/com.vexlife.prod.service` fails).

| # | Action | Expect |
|---|---|---|
| D1 | on a disposable Home in `SETUP_REQUIRED`, press Set Up VexLife | progress panel lines: Checking this copy… → Preparing private Python… → Downloading the local model — x of 14.1 GiB (n%) |
| D2 | press **Cancel safely** mid-download | "Setup paused." → refreshed card `SETUP_REQUIRED` with "x of 14.1 GiB downloaded"; `.partial` file present |
| D3 | Set Up again | download resumes from x; ends with "VexLife is ready." and the browser at 127.0.0.1:18210 |
| D4 | restart the Mac (the real test), then rerun the window | **Start VexLife** card (not "needs repair"), "Nothing will be downloaded" → Start → ready; no download lines |
| D5 | after D4, a fresh readiness check + one canonical Vex turn still succeed (AGENTS.md §7) | — |
| D6 | disconnect network during D1 | failure "VexLife couldn't download what it needed" → Continue → `SETUP_REQUIRED` with progress kept |

## Record for each run

Exact branch head, image payload SHA, macOS version, chip/memory, screenshots of each card, the
stderr/stdout of any failure, and whether any macOS permission prompt appeared (and for which step).

<!-- [VXG RealForever][VEXLIFE-PROD][SETUP-JOURNEY][STAGE-3-PROOF] -->
