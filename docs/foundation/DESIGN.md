# Design foundation

`[VXG RealForever]`

How VexLife looks is presentation, and presentation never owns meaning (AGENTS.md: "a renderer may
consume semantic truth; it does not become that truth owner"). This foundation owns the visual
vocabulary every VexLife surface draws with, the laws that keep it coherent, and how VexLife shows
the parts of Vex a person may want to understand: its thoughts, its configuration, and what shaped a
reply.

## Lineage

VexLife draws with the **Vextreme Design Foundation v2** (the `Vextreme_Design_Foundation_v2`
package: `tokens/colors.css`, `tokens/typography.css`, `tokens/spacing.css`, `components/`,
`AI-GUIDE.md`, and the R001 VexLife packet). The package is not yet checked into `vgong24/Vextreme`;
until it is, this file and `VexLifeWebP1ExperienceStyle` are where VexLife records what it took.

| Vextreme v2 law | Here |
|---|---|
| black and white are identity; a cool gray ramp is structure; no accent hue | `--gray-0` … `--gray-9`, `--black`, `--white`; the inverse fill (`--accent-bg`) is the primary action |
| components use semantic tokens only, never ramp stops or literals | ramp stops and color literals appear only in the token blocks (`VexLifeWebP1DesignTokenTest`) |
| status hues are for state, never decoration | `--status-success` `--status-caution` `--status-critical` mark live state only |
| dark is the emotional home; light is authored, not inverted | both themes are authored token by token; the person's system theme is followed unless they choose |
| three voices: editorial, product, technical | `--font-serif` (Vex speaking), `--font-sans` (product chrome), `--font-mono` (refs, figures, evidence) |
| sharp vs fluid: structure is low-radius, the Switch is the one pill | `--radius-sm` 4 / `--radius-md` 6 / `--radius-lg` 10; `--radius-full` only for the switch, dots and badges |
| one spacing scale (4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 · 96) | `--space-1` … `--space-9` |
| restrained motion | `--ease-standard` with `--duration-fast` (120 ms) / `--duration-base` (180 ms) |

Fonts are named, not fetched. VexLife is local-first, so it does not import Google Fonts: IBM Plex
Sans / Mono and Source Serif 4 are used when the person has them, and the system faces otherwise.

From the R001 VexLife packet, three principles carry straight into this workspace:

- **Invisible selection requires inspectable accountability.** VexLife may choose context for Vex
  quietly, but the person can always open what was chosen (Vex Context, the ticket, thoughts).
- **No architecture IDs by default.** Refs, receipts and paths sit on the last rung of the
  disclosure ladder (below), never instead of plain words.
- **Meaning before machinery.** A state is told as meaning → consequence → available action →
  explanation → technical evidence.

## Token families

| Family | Tokens | Means |
|---|---|---|
| Ramp | `--gray-0` … `--gray-9` `--black` `--white` | raw structure; read only by the token blocks |
| Background | `--bg-canvas` `--bg-surface` `--bg-surface-raised` `--bg-overlay` `--bg-inverse` | the page, a card, a lifted control, a well, the one inverse object |
| Text | `--text-primary` `--text-secondary` `--text-tertiary` `--text-inverse` | all ≥ 4.5:1 on every background they sit on |
| Border | `--border-subtle` `--border-default` `--border-strong` `--border-focus` | separation, quiet to firm; focus is the identity color |
| Accent | `--accent-bg` `--accent-text-on` | the primary action and the switch when on: inverse, never a hue |
| Status | `--status-success` `--status-caution` `--status-critical` `--critical-bg` `--critical-text` | live state only |
| Speakers | `--speaker-you-*` `--speaker-vex-*` | who said it, drawn from the ramp |
| Ticket | `--ticket-bg` `--ticket-text` `--ticket-text-secondary` `--ticket-border` `--ticket-shadow` `--ticket-hover` | the context ticket: where what the person sees becomes what Vex is given |
| Relationship | `--relation-*` | how something relates to Vex (below) |
| Type | `--font-sans` `--font-serif` `--font-mono`; `--step--2` … `--step-3` (11 · 12 · 14 · 16 · 18 · 22) | three voices; Vextreme's size scale |
| Space | `--space-1` … `--space-9` | one spacing scale |
| Shape | `--radius-sm` `--radius-md` `--radius-lg` `--radius-full` | one corner scale; `50%` only for round marks |
| Motion | `--ease-standard` `--duration-fast` `--duration-base` | one curve, two speeds |
| Canvas | `--page-canvas` `--scrim` `--shadow-2` | a browsed page's own white, a modal's backdrop, lift |

The context meter is the relationship legend in miniature: the foundation segment in
`--relation-foundation`, the conversation in `--relation-conversation`, what is left as an empty
well. Its figure sits on a surface chip in the technical voice, so it reads on any fill.

## The relationship legend

Vex Context taught this first: each part of what Vex received has a color, and the color says how
that part relates to Vex. A person who learns it once should be able to read it everywhere, so the
colors are tokens and every surface that shows the same relationship uses the same one.

Under v2 the legend is quiet: one low-chroma hue per relationship, all at one lightness per theme,
used only as a left rail or a dot beside the name it marks, with at most a 4% wash. One rule draws
every Vex Context section (`--relation` is set per kind), so no relationship is drawn differently
from another.

| Token | Hue | Relationship | Shown on |
|---|---|---|---|
| `--relation-foundation` | violet | standing context Vex always starts from | Vex Context "Foundation", the meter |
| `--relation-memory` | ochre | what Vex remembers about the person | Vex Context "Structured Memory" |
| `--relation-continuity` | orchid | what carries over from earlier turns | Vex Context "Conversation continuity" |
| `--relation-conversation` | indigo | the conversation so far | Vex Context "Conversation", the meter |
| `--relation-reach` | teal | things Vex reached for: pages, searches, Home | Vex Context "Browser / things Vex reached" |
| `--relation-person` | blue | what the person asked, in their words | Vex Context "Your message", exact human messages |
| `--relation-process` | rose | Vex's own working while it answers | Vex Context "Vex's working process", **thoughts in the conversation** |
| `--relation-output` | green | what Vex said back | Vex Context "Vex's reply", exact Vex messages |
| `--relation-after` | gray | what stayed after the reply | Vex Context "After the turn" |
| `--relation-exact` | `--text-tertiary` | the exact order and bytes, for checking | Vex Context "How Vex put it together" |
| `--relation-runtime` | slate | the local model and how it is configured | **model state, configuration and import cards** |

A left rail in a relationship color means: this is part of Vex's inside, shown to you on purpose.
A color never carries the meaning alone: every rail sits beside the name of what it marks, so the
legend teaches, and a person who cannot tell the colors apart loses nothing.

## The disclosure ladder

VexLife does not hide Vex's thoughts, its configuration, or what shaped a reply. It reveals them in
an order a person can follow:

~~~text
1. plain words     what this is and what it means for you      (sans, text-primary / -secondary)
2. relationship    how it connects to Vex                       (relationship rail + its name)
3. exact           the refs, receipts, sizes and paths behind   (mono, text-tertiary, on demand)
~~~

- **Thoughts.** The toggle's mark and the opened thought carry the process color, the same rail as
  "Vex's working process" in Vex Context, where the same thinking is kept for the session. A person
  can follow one thought from the conversation to the turn it shaped.
- **Model configuration.** Each model card says in plain words what a setting changes and what it
  costs (memory, waiting), carries the runtime rail, and shows exact sizes and revisions in figures.
- **Vex Context.** Already follows the ladder: section name, relationship rail, then the exact
  component list and messages.

Nothing on the ladder is a second source of truth: every rung is a projection of a Family's
DisplayState or a receipt. The ladder also never writes words: text comes from catalog keys
(LOCALIZATION.md), so it can be read in the person's language.

## Enforcement

`src/jvmTest/kotlin/vexlife/web/VexLifeWebP1DesignTokenTest.kt` reads
`VexLifeWebP1ExperienceStyle` and holds:

- every `var(--x)` names a declared token, and every declared token is used (the ramp and the
  spacing scale are declared whole);
- no color literal and no ramp stop outside the token blocks;
- every `border-radius` is a radius token (or `50%`), every `font-size` a step token (or `em`);
- the three token blocks agree: both dark blocks declare the same dark values;
- every token used as text meets 4.5:1 on `--bg-surface`, `--bg-canvas` and `--bg-surface-raised`
  in light and dark (colors are resolved from `oklch()` through `var()` chains), and each inverse
  pair (ticket text on the ticket, `--critical-text` on `--critical-bg`, `--accent-text-on` on
  `--accent-bg`, `--text-primary` on `--bg-overlay`) meets it too;
- every Vex Context section kind, thought box and model card draws its rail from `--relation-*`,
  and one rule draws them all;
- raw `px` spacing only shrinks (the ceiling is the count after the `--space-*` pass).

## Not yet aligned

| Surface | Today | Next |
|---|---|---|
| Type roles | `font-size` steps plus separate weights | v2's whole `font:` roles (`--text-h2`, `--text-label` …) |
| Spacing | 147 raw `px` values that fall between scale steps (5, 6, 7, 9, 10, 11, 14 …) | snap each to the scale with a render check; the ceiling only goes down |
| Setup window (native) | macOS system controls | already themed by the OS; only its words are VexLife's |
| Vision / Memory / Private / Vex Context DisplayStates | English `String` text | `VexLifeText` (LOCALIZATION.md) |
| Health-style states (`Unavailable`, `Checking`) | implementation words first | meaning → consequence → action, per R001 |

<!-- [VXG RealForever][VEXLIFE-PROD][DESIGN] -->
