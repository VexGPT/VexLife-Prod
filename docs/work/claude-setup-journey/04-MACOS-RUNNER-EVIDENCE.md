# Stage 4 — macOS runner evidence

`[VXG RealForever]` · work notes · proof route only (`PROOF_ROUTE != ORDINARY_PRODUCT_ROUTE`)

A proof-only workflow on branch `VXG-111326-claude-macos-proof-a001` (never merged) ran the exact
#119 source on a GitHub macOS runner: check run `132017748879`, 2026-11-13.

Host: macOS 26.7.2 · Apple M1 (Virtual) · 7 GiB · arm64. This host is **not** the certified Heretic
profile, so it exercises the held paths, not the happy path.

| Check | Result |
|---|---|
| `osacompile` of `packaging/private-service-image/setup/vexlife-prod-setup-window.applescript` | PASS |
| Setup / Home / Entry / packaging tests on macOS | PASS |
| `assembleVexLifeProdServiceImage` (clean exact source, jlink runtime) | PASS |
| Window compiled from the image, bootstrap metadata bound, ad-hoc signed, `codesign --verify --strict` | PASS |
| `--inspect-front-door` absent Home | `HOST_HELD`, rail `⊘ This Mac`, buttons [Close] |
| `--inspect-front-door` empty folder | `HOST_HELD` (host before formation) |
| `--inspect-front-door` nested missing folders | `HOST_HELD` |
| `--inspect-front-door` folder with a file | `HOME_HELD`, buttons [Choose Another Location…, Check Again, Close] |
| `--inspect-front-door` `/tmp/…` (macOS `/tmp` → `/private/tmp`) | `HOME_HELD` (linked parent) |
| `--activate-front-door-control FORM_HOME` on that host | `STATE_CHANGED_BEFORE_EFFECT`, `REFRESH_ENTRY`, Home not created, neighbouring file intact |
| JSON channel encoding (✓ ⊘ ○ …) | intact |

Still owed on a certified Mac (see 03): AppKit card rendering and interaction, NSOpenPanel, progress +
Cancel safely, Home formation + model acquisition, launchd service, Start after restart, readiness +
canonical Vex turn.
