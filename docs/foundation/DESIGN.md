# Design foundation

`[VXG RealForever]`

How VexLife looks is presentation, and presentation never owns meaning (AGENTS.md: "a renderer may
consume semantic truth; it does not become that truth owner"). This foundation owns the visual
vocabulary every VexLife surface draws with, the laws that keep it coherent, and how VexLife shows
the parts of Vex a person may want to understand: its thoughts, its configuration, and what shaped a
reply.

## Lineage

The laws come from Vextreme's institutional foundation family (`vgong24/Vextreme`
`docs/architecture/12-design-system.md`, `styles/design-system.css` `[data-theme="foundation"]`),
which earned them on public pages:

| Vextreme law | Here |
|---|---|
| components reference semantic tokens only, never ramp stops or literals | colors, radii and type sizes go through tokens (`VexLifeWebP1DesignTokenTest`) |
| dark is the identity home; light is re-authored, not inverted | both themes are authored token by token (`:root`, `prefers-color-scheme: dark`, `[data-theme="dark"]`) |
| status pairs meet 4.5:1, enforced by test | every token used as text meets 4.5:1 on the backgrounds it sits on, in both themes |
| three type voices: editorial, product, technical | `--voice` (Vex speaking, empty-state headings), `--sans` (product chrome), `--mono` (refs, paths, exact evidence) |
| a token the CSS uses but never declares is a silent bug (Session 015) | every `var(--x)` resolves to a declared token; a fallback does not count |
| provenance lines name sources in the technical voice | exact refs, receipts and paths are shown in `--mono`, quietly, never instead of plain words |

VexLife keeps its own token names. They already carried this meaning (`--ink`, `--surface`,
`--paper` …); renaming them to Vextreme's would move nothing a person sees.

## Token families

| Family | Tokens | Means |
|---|---|---|
| Ground | `--ground` `--surface` `--sunken` `--raised` | where things sit: the page, a card, a well, a lifted control |
| Rule | `--rule` `--rule-strong` | separation, quiet then firm |
| Ink | `--ink` `--ink-2` `--ink-3` | text: primary, secondary, tertiary (all ≥ 4.5:1) |
| Speakers | `--you` `--you-rule` `--vex` `--vex-rule` | who said it |
| Paper | `--paper` `--paper-ink` `--paper-ink-2` `--paper-rule` `--paper-shadow` `--paper-hover` | the context ticket: the one loud object, where what the person sees becomes what Vex is given |
| Status | `--ready` `--pending` `--down` `--danger` `--danger-ink` `--switch-on` | a live state, never decoration |
| Meter | `--core` `--used` `--remaining` `--meter-ink` `--meter-halo` | the context-window legend: core blue, used yellow, remaining green |
| Keeping | `--kept` `--unkept` | whether this conversation is kept in the person's archives |
| Relationship | `--relation-*` | how something relates to Vex (below) |
| Type | `--sans` `--voice` `--mono`; `--step--2` … `--step-3` | three voices; one size scale |
| Shape | `--radius-xs` `--radius-sm` `--radius-md` `--radius-lg` `--radius-xl` `--radius-pill` | one corner scale; `50%` only for dots |
| Motion | `--ease` | one curve |
| Canvas | `--page-canvas` `--scrim` `--shadow` `--focus` | a browsed page's own white, a modal's backdrop, lift, focus |

## The relationship legend

Vex Context taught this first: each part of what Vex received has a color, and the color says how
that part relates to Vex. A person who learns it once should be able to read it everywhere, so the
colors are tokens and every surface that shows the same relationship uses the same one.

| Token | Relationship | Shown on |
|---|---|---|
| `--relation-foundation` | standing context Vex always starts from | Vex Context "Foundation" |
| `--relation-memory` | what Vex remembers about the person | Vex Context "Structured Memory" |
| `--relation-continuity` | what carries over from earlier turns | Vex Context "Conversation continuity" |
| `--relation-conversation` | the conversation so far | Vex Context "Conversation" |
| `--relation-reach` | things Vex reached for: pages, searches, Home | Vex Context "Browser / things Vex reached" |
| `--relation-person` | what the person asked, in their words | Vex Context "Your message", exact human messages |
| `--relation-process` | Vex's own working while it answers | Vex Context "Vex's working process", **thoughts in the conversation** |
| `--relation-output` | what Vex said back | Vex Context "Vex's reply", exact Vex messages |
| `--relation-after` | what stayed after the reply | Vex Context "After the turn" |
| `--relation-exact` | the exact order and bytes, for checking | Vex Context "How Vex put it together" |
| `--relation-runtime` | the local model and how it is configured | **model state, configuration and import cards** |

A left rail in a relationship color means: this is part of Vex's inside, shown to you on purpose.
A color never carries the meaning alone: every rail sits beside the name of what it marks, so the
legend teaches, and a person who cannot tell the colors apart loses nothing.

## The disclosure ladder

VexLife does not hide Vex's thoughts, its configuration, or what shaped a reply. It reveals them in
an order a person can follow:

~~~text
1. plain words     what this is and what it means for you      (sans, ink / ink-2)
2. relationship    how it connects to Vex                       (relationship rail + its name)
3. exact           the refs, receipts, sizes and paths behind   (mono, ink-3, on demand)
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

- every `var(--x)` names a declared token, and every declared token is used;
- no color literal outside the token blocks;
- every `border-radius` is a radius token (or `50%`), every `font-size` a step token (or `em`);
- the three token blocks agree: both dark blocks declare the same dark values;
- every token used as text color meets 4.5:1 on `--surface`, `--ground` and `--raised` in light and
  dark, and each inverse pair (`--surface` on `--ink`, `--danger-ink` on `--danger`, the paper inks
  on `--paper`, `--meter-ink` on the meter colors) meets it too;
- every Vex Context section kind, thought box and model card draws its rail from `--relation-*`.

## Not yet aligned

| Surface | Today | Next |
|---|---|---|
| Spacing | raw `px` padding / margin / gap throughout | a `--space-*` scale; the test ratchets the raw count down, never up |
| Setup window (native) | macOS system controls | already themed by the OS; only its words are VexLife's |
| Vision / Memory / Private / Vex Context DisplayStates | English `String` text | `VexLifeText` (LOCALIZATION.md) |

<!-- [VXG RealForever][VEXLIFE-PROD][DESIGN] -->
