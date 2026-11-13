# Next stages: continuity, model choice, Windows

`[VXG RealForever]` · work notes, **not authority** · 2026-11-13 · may move to VexWork
`project.vexlife.new-beginnings` once reconciled

Landed today: #120 (Vision selected scope through one Family and Control route), #121 (Memory
projection), #122 (clean chat says what it will be), #123 (VexBoard Screen Lens export), #124
(Read aloud ported from the Vextreme Spiral FAB). This note records what the owners showed while
doing that, and the decisions each next stage needs before code. Each section ends with the
smallest slice that could land once its decision is made.

## 1. Memory formation (the largest continuity gap)

What is true in Product today (see `VEXLIFE-CONTINUITY-REALIGNMENT.md`, "What a person is told
about Memory"):

- Prompt Memory is CURRENT only while the Daily Memory's Conversation frontier equals the current
  Conversation head. The predecessor holds the same way.
- Score semantic acceptance has no writer in Product or in the predecessor's source. Score therefore
  never catches up, the Daily Dream never becomes lawful, a fresh person never forms Memory, and an
  existing Home's Memory goes Behind after one Product turn.
- What Vex does carry turn to turn is recent completed Conversation turns within the token budget
  (`RECENT_COMPLETED_TURNS`), plus the working-continuity successor at rollover.

So for a long relationship, Vex currently forgets everything older than its prompt window.

Proposal, consent-first, using owners that already exist:

~~~text
completed turn
-> VexLifeScoreContinuity forms one non-authoritative candidate from exact Conversation source
   (a short attributed statement: "Victor is building a trellis for the beans")
-> Memory Family Control: Keep / Not this            <- the person decides, in the Companion
-> [NEW OWNER] consent + semantic acceptance + semantic-authority head
-> VexLifeScoreContinuity appends Score (it already requires exactly this acceptance)
-> Memory Family Control: Rest -> POST /api/v1/memory/dream (exists)
-> Living Journal pages in the next prompt (exists)
~~~

**Decisions for Victor:**

1. **Acceptance owner.** Which owner materializes semantic acceptance from the person's consent?
   The reader validates `owner.multivex.score-semantic-authority.materializer.001`. A Product owner
   claiming that ref is the simplest route; a Vextreme-SDK owner keeps the historical split.
2. **Keeping Memory current across turns.** Should a Daily stratum whose Score and semantic-authority
   heads are still current stay CURRENT across later turns? The strict Conversation-frontier gate
   protects withdrawals said in conversation but not yet processed. Once withdrawals go through the
   consent owner above, a Score head change already invalidates the stratum. Without this change,
   Memory helps only on the first turn after each Rest.
3. **Candidate cadence.** Who proposes, and how often: after every turn, at rest, or only when the
   person asks ("remember that")?

Smallest first slice once (1) is decided: the "remember that" path only. Keep → acceptance → Score →
Rest, with the Memory Family gaining Keep/Rest Controls and the Settings Continuity section showing
kept statements with a Forget control. Forget writes a withdrawal consent, which the reader already
honors.

## 2. Clean start

The predecessor's `CLEAN_START` (vgong24/VexLife #975, unmerged candidate) is a per-Send
prompt-selection boundary. Product selects *recent* turns, so a one-turn mode would expire on the
next Send. A clean chat needs a durable boundary in canonical Conversation, either a field on the
REQUEST event or a separate boundary record, which `DirectPromptContext` respects when selecting.
That is a canonical Conversation contract change (v1/v2 compatibility with lived Homes), so it waits
for an explicit decision.

## 3. Model choice ("Heretic model selection")

Facts:

| Bundle (predecessor `blueprint/model-bundle-registry.json`) | State | Runtime | Hosts |
|---|---|---|---|
| Heretic 27B MLX 4-bit `…qwen3.8-27b.mlx4.329658ee8f27.001` | RELEASE_QUALIFIED | MLX | Apple M4 Pro (only route Product implements) |
| Qwen3.5-4B Q4_K_M `…g0.qwen3.5-4b.q4-k-m.001` | RELEASE_QUALIFIED | llama.cpp b10107 | Windows x64 + NVIDIA; macOS arm64 M4 Pro Metal profile |
| Vex M4 generation-2 MLX `…m4.generation-2.mlx.001` | CANDIDATE_QUALIFICATION | MLX | not offerable |

Product's host profile requires exactly `Apple M4 Pro` with at least 12 GiB, so a person on an M3 Max
or M4 Max is held today. That is qualification, not capability: only Victor's M4 Pro was proven.

A choice is only honest where two qualified options exist on the same host. The candidate pair is
Qwen 4B (llama.cpp Metal) versus Heretic 27B on an M4 Pro. The Qwen bundle lists a macOS M4 Pro Metal
operational profile, but that profile's own qualification still needs confirming, and Product has no
llama.cpp runtime realization yet.

Proposal:

- A model catalog owner listing bundles with per-host eligibility and qualification state. It is
  effect-inert, in the shape of `HereticProductIdentity`.
- A Model Family in Settings: the current binding (exists in current-state), plus Switch only when at
  least two qualified, eligible options exist on this host.
- At onboarding, the `SETUP_REQUIRED` card offers the choice only under the same condition, with
  each option's download size and memory need from the catalog.
- Swapping is an explicit replacement route. The predecessor rule stands: existing Homes don't
  silently follow a fresh-install default.

**Prerequisite:** port the llama.cpp runtime realization (acquisition, server lifetime,
qualification) and prove it lived on the M4 Pro. Only then does a second option exist.

## 4. Windows

The predecessor already runs on Windows 10/11 x64 with an NVIDIA GPU (≥12 GiB RAM, ≥6 GiB free) through
Qwen 4B on llama.cpp and a `setup-vexlife.cmd` "Continue with Vex" window. Product is Mac-only at
every platform seam:

| Seam | Mac today | Windows needs |
|---|---|---|
| Entry semantics | `VexLifeProdEntryFamily` (platform-neutral) | reuse as is |
| Host profile | `HereticMacHostProfile` | Windows x64 + NVIDIA observation (`nvidia-smi`) |
| Runtime | MLX persistent runtime | llama.cpp b10107 CUDA realization (shared with §3) |
| Service lifetime | launchd user agent | per-user Scheduled Task or service; same fixed loopback ports |
| Front door | AppleScriptObjC card renderer | a Windows renderer of the same cards (WinForms via PowerShell, like the predecessor) |
| Home | `~/.vexlife`, symlink/ENOTDIR rules | `%USERPROFILE%\.vexlife`, junction/reparse-point rules |
| Vision | macOS window picker | held (`RouteUnreachable`) until a Windows picker owner exists |

Order: §3's llama.cpp realization first, since Windows needs it anyway. Then host profile, Home path
rules, and the front-door renderer against the existing Entry Family cards. Service lifetime comes
last. Each slice can be proven on a Windows CI runner the way #119 used a macOS runner. A lived
NVIDIA proof stays owed.

## 5. Family header

The family mark is in Product's Brand (#119). The Vextreme site header also carries the family row
(Vextreme / VexLife / VexWork). If that is what "family header" means, the honest Product version is
a family menu on the Brand that opens those pages in the embedded Browser. Product would gain no
runtime dependency, and the address suggestions already name them. The intended meaning needs
confirming before it changes the A003 composition.

<!-- [VXG RealForever][VEXLIFE-PROD][NEXT-STAGES][WORK-NOTES] -->
