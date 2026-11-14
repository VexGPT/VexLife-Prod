package vexlife.web

/**
 * Canonical product presentation measurements for the A003-faithful VexLife-Prod workspace.
 *
 * These bytes own presentation consequences only. Blueprint/VexWeb remain the source of stable
 * product identity and relationships.
 */
internal object VexLifeWebP1ExperienceStyle {
    fun render(): String =
        """
/* VexLife-Prod canonical nested-browser product presentation.
   Perceptual baseline: A003 "VexLife Nested Browser Experience".
   Palette: quiet neutral chrome in light and dark; the one loud element is the paper context
   ticket, the place where what the human sees becomes what Vex is given.
   Meter colors follow the design legend: core blue, used yellow, remaining green.
   [VXG RealForever] */

:root {
  --ground: #eef0ee;
  --surface: #ffffff;
  --sunken: #e8eae7;
  --raised: #f6f7f5;
  --rule: #d4d7d2;
  --rule-strong: #b4b9b2;
  --ink: #1b1e22;
  --ink-2: #555c66;
  --ink-3: #868d97;
  --you: #e6edf6;
  --you-rule: #c9d6e6;
  --vex: #ffffff;
  --vex-rule: #cfd3cd;
  --core: #a7c4ea;
  --used: #f2d877;
  --remaining: #a6dbb1;
  --switch-on: #4bb05c;
  --ready: #2f9e55;
  --pending: #b8860b;
  --down: #c8432f;
  --danger: #d6372f;
  --danger-ink: #ffffff;
  --paper: #efe5cd;
  --paper-ink: #2c271e;
  --paper-rule: #c8b993;
  --focus: #3c78c8;
  --shadow: 0 10px 30px rgba(20, 24, 30, 0.12);

  --sans: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Inter", system-ui, sans-serif;
  --voice: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
  --mono: ui-monospace, "SF Mono", Menlo, Consolas, monospace;

  --step--1: 12px;
  --step-0: 13.5px;
  --step-1: 15px;
  --step-2: 17px;
  --radius: 10px;
  --chrome-height: 64px;
  --ease: cubic-bezier(0.2, 0.75, 0.2, 1);
  color-scheme: light;
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --ground: #101317;
    --surface: #171b20;
    --sunken: #1f242b;
    --raised: #1c2026;
    --rule: #2b313a;
    --rule-strong: #3f4651;
    --ink: #e6e9ed;
    --ink-2: #a2aab5;
    --ink-3: #6f7783;
    --you: #22303f;
    --you-rule: #334459;
    --vex: #1d2228;
    --vex-rule: #323944;
    --core: #5f8ed0;
    --used: #cdab43;
    --remaining: #57a96d;
    --switch-on: #4bb05c;
    --ready: #6fcb8d;
    --pending: #e0be6a;
    --down: #e57f6c;
    --danger: #d9443b;
    --focus: #8fb4e8;
    --shadow: 0 14px 40px rgba(0, 0, 0, 0.45);
    color-scheme: dark;
  }
}

:root[data-theme="dark"] {
  --ground: #101317;
  --surface: #171b20;
  --sunken: #1f242b;
  --raised: #1c2026;
  --rule: #2b313a;
  --rule-strong: #3f4651;
  --ink: #e6e9ed;
  --ink-2: #a2aab5;
  --ink-3: #6f7783;
  --you: #22303f;
  --you-rule: #334459;
  --vex: #1d2228;
  --vex-rule: #323944;
  --core: #5f8ed0;
  --used: #cdab43;
  --remaining: #57a96d;
  --switch-on: #4bb05c;
  --ready: #6fcb8d;
  --pending: #e0be6a;
  --down: #e57f6c;
  --danger: #d9443b;
  --focus: #8fb4e8;
  --shadow: 0 14px 40px rgba(0, 0, 0, 0.45);
  color-scheme: dark;
}

* { box-sizing: border-box; }
html, body { margin: 0; height: 100%; }
body {
  background: var(--ground);
  color: var(--ink);
  font: 400 var(--step-0)/1.45 var(--sans);
  -webkit-font-smoothing: antialiased;
}
button, input, select, textarea { font: inherit; color: inherit; }
button { cursor: pointer; }
button:disabled { cursor: default; }
svg { display: block; }
[hidden] { display: none !important; }
:focus-visible { outline: 2px solid var(--focus); outline-offset: 2px; }

.visually-hidden {
  position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0;
  overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0;
}

.workspace {
  display: grid;
  grid-template-rows: var(--chrome-height) minmax(0, 1fr);
  height: 100dvh;
}

/* ------------------------------------------------------------ chrome */

.chrome {
  display: grid;
  grid-template-columns: auto minmax(220px, 520px) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  padding: 0 0 0 14px;
  background: var(--surface);
  border-bottom: 1px solid var(--rule);
}

.brand {
  display: flex; align-items: center; gap: 10px;
  color: var(--ink); text-decoration: none;
}
.brand-mark {
  display: grid; place-items: center;
  width: 30px; height: 30px; flex: 0 0 30px;
  color: var(--ink);
}
.brand-mark svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 5; stroke-linecap: round; stroke-linejoin: round; }
.brand-name { font-weight: 650; font-size: var(--step-2); letter-spacing: -0.01em; }

.address { position: relative; display: flex; align-items: center; min-width: 0; }
.address input {
  width: 100%; height: 36px; padding: 0 40px 0 12px;
  background: var(--raised); border: 1px solid var(--rule-strong); border-radius: 8px;
  text-overflow: ellipsis;
}
.address input::placeholder { color: var(--ink-3); }
.address .icon-button { position: absolute; right: 4px; }
.address-error {
  position: absolute; top: calc(100% + 6px); left: 0; z-index: 5; margin: 0;
  padding: 7px 10px; border-radius: 8px; background: var(--ink); color: var(--surface);
  font-size: var(--step--1); box-shadow: var(--shadow);
}

.icon-button {
  display: grid; place-items: center; width: 30px; height: 30px; padding: 0;
  background: transparent; border: 0; border-radius: 6px; color: var(--ink-2);
}
.icon-button svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.icon-button:hover:not(:disabled) { background: var(--sunken); color: var(--ink); }
.icon-button:disabled { opacity: 0.4; }

.app-selector select {
  width: 172px; height: 36px; padding: 0 30px 0 14px;
  background: var(--surface); border: 1px solid var(--rule-strong); border-radius: 8px;
  font-weight: 600;
}

.utilities {
  justify-self: end; align-self: stretch;
  display: flex; align-items: center; padding: 0 14px;
  background: var(--sunken); border-left: 1px solid var(--rule);
}
.quick-actions { display: flex; gap: 10px; margin: 0; padding: 0; list-style: none; }
.quick-action {
  display: grid; place-items: center; width: 42px; height: 42px; padding: 0;
  background: var(--surface); border: 1px solid var(--rule); border-radius: 50%;
  color: var(--ink-2);
}
.quick-action svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.quick-action:hover { color: var(--ink); border-color: var(--rule-strong); }
.quick-action[data-bound="false"] { color: var(--ink-3); }
.quick-action[aria-expanded="true"], .quick-action[data-active="true"] { color: var(--ink); border-color: var(--ink-2); }

.utilities { position: relative; }
.speech {
  position: absolute; top: calc(100% + 8px); right: 12px; z-index: 30; width: 320px;
  display: grid; gap: 10px; padding: 12px 14px;
  background: var(--surface); border: 1px solid var(--rule-strong); border-radius: 12px; box-shadow: var(--shadow);
}
.speech[hidden] { display: none; }
.language-panel {
  position: absolute; top: calc(100% + 8px); right: 12px; z-index: 30; width: 260px;
  display: grid; gap: 8px; padding: 12px 14px;
  background: var(--surface); border: 1px solid var(--rule-strong); border-radius: 12px; box-shadow: var(--shadow);
}
.language-panel[hidden] { display: none; }
.language-title { margin: 0; font-weight: 650; }
.language-list { margin: 0; padding: 0; list-style: none; display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
.language-list button { width: 100%; padding: 6px 10px; text-align: left; border: 1px solid transparent; border-radius: 8px; background: none; color: var(--ink); }
.language-list button:hover { background: var(--sunken); }
.language-list button[aria-selected="true"] { border-color: var(--rule-strong); background: var(--sunken); font-weight: 650; }
.language-note { margin: 0; color: var(--ink-2); font-size: var(--step--1); }
.speech-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.speech-title { font-weight: 650; }
.speech-status { margin: 0; color: var(--ink-2); font-size: var(--step--1); }
.speech-field { display: grid; grid-template-columns: 48px minmax(0, 1fr) auto; align-items: center; gap: 8px; font-size: var(--step--1); color: var(--ink-2); }
.speech-field select { grid-column: 2 / -1; min-width: 0; height: 30px; background: var(--surface); border: 1px solid var(--rule-strong); border-radius: 6px; color: var(--ink); }
.speech-check { display: flex; align-items: center; gap: 8px; font-size: var(--step--1); color: var(--ink); }
.speech-controls { display: flex; align-items: center; gap: 6px; }
.speech-controls .quiet-button { margin-left: auto; }
.speech-controls .icon-button svg path, .speech-controls .icon-button svg rect { fill: currentColor; stroke: none; }
.speech-controls .icon-button svg path.glyph-line, .speech-controls .icon-button svg path.glyph-pause { fill: none; stroke: currentColor; stroke-width: 2; }
.speech-play .glyph-pause, .speech[data-mode="playing"] .speech-play .glyph-play { display: none; }
.speech[data-mode="playing"] .speech-play .glyph-pause { display: inline; }
.message.reading .bubble { outline: 2px solid var(--ink-2); outline-offset: 2px; }
.timeline[data-choosing="true"] .message { cursor: pointer; }
.timeline[data-choosing="true"] .message:hover .bubble { outline: 1px dashed var(--ink-2); outline-offset: 2px; }

/* ------------------------------------------------------------ frame + seam */

.frame {
  --before: 50%;
  display: grid;
  grid-template-columns: minmax(0, var(--before)) 12px minmax(0, 1fr);
  min-height: 0;
}
.frame[data-collapsed="before"] { grid-template-columns: 0 12px minmax(0, 1fr); }
.frame[data-collapsed="after"] { grid-template-columns: minmax(0, 1fr) 12px 0; }
.frame[data-collapsed="before"] .region-browser,
.frame[data-collapsed="after"] .region-companion { visibility: hidden; }
.frame.resizing { cursor: col-resize; user-select: none; }
.frame.resizing iframe { pointer-events: none; }

.region { min-width: 0; min-height: 0; }

.region-browser { display: flex; flex-direction: column; background: var(--ground); }
.frame-status {
  margin: 0; padding: 8px 14px;
  background: var(--paper); color: var(--paper-ink);
  border-bottom: 1px solid var(--paper-rule);
  font-size: var(--step--1);
}
.frame-status[data-frame="Loading"] { background: var(--sunken); color: var(--ink-2); border-bottom-color: var(--rule); }
.viewport { position: relative; flex: 1; min-height: 0; }
.viewport #page-frame { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; background: #fff; }
.context-frame {
  position: absolute; width: 1px; height: 1px; left: -10000px; top: -10000px;
  border: 0; opacity: 0; pointer-events: none;
}
.viewport-empty { position: absolute; inset: 0; display: grid; place-items: center; color: var(--ink-3); background: var(--ground); }
.viewport[hidden] { display: none; }

.family { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border-bottom: 1px solid var(--rule); background: var(--surface); }
.family[hidden] { display: none; }
.family-member { padding: 5px 12px; border: 1px solid transparent; border-radius: 999px; background: none; color: var(--ink-2); font-weight: 600; }
.family-member:hover { color: var(--ink); background: var(--sunken); }
.family-member[aria-current="page"] { color: var(--ink); border-color: var(--rule-strong); background: var(--sunken); }
.family-note { margin-left: auto; color: var(--ink-3); font-size: var(--step--1); }

.archives { flex: 1; min-height: 0; overflow-y: auto; padding: 18px 22px 28px; background: var(--ground); }
.archives[hidden] { display: none; }
.archives-head h2 { margin: 0; font-size: var(--step-1); }
.archives-summary { margin: 4px 0 16px; color: var(--ink-2); font-size: var(--step--1); }
.archives-timeline { margin: 0; padding: 0; list-style: none; display: grid; gap: 18px; }
.archives-day h3 { margin: 0 0 8px; font-size: var(--step-0); }
.archives-day h3 span { margin-left: 8px; color: var(--ink-3); font-weight: 500; font-size: var(--step--1); }
.archives-entries { margin: 0; padding: 0; list-style: none; display: grid; gap: 6px; border-left: 2px solid var(--rule); }
.archives-entry { display: grid; grid-template-columns: 56px 40px minmax(0, 1fr); gap: 8px; padding: 2px 0 2px 12px; font-size: var(--step--1); }
.archives-entry time { color: var(--ink-3); font-variant-numeric: tabular-nums; }
.archives-entry .who { color: var(--ink-2); font-weight: 600; }
.archives-entry p { margin: 0; color: var(--ink); white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; cursor: pointer; }
.archives-entry p[data-open="true"] { display: block; -webkit-line-clamp: unset; }
.archives-empty { color: var(--ink-3); }
.address[data-app="archives"] { opacity: 0.5; pointer-events: none; }

.seam {
  position: relative;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px;
  background: var(--surface);
  border-inline: 1px solid var(--rule);
  cursor: col-resize; touch-action: none;
}
.seam:focus-visible { outline-offset: -2px; }
.seam-grip { color: var(--ink-3); }
.seam-grip svg { width: 22px; height: 12px; fill: none; stroke: currentColor; stroke-width: 1.4; stroke-linecap: round; stroke-linejoin: round; transform: scale(0.8); }
.seam:hover .seam-grip, .seam:focus-visible .seam-grip { color: var(--ink); }
.seam-collapse {
  display: grid; place-items: center; width: 18px; height: 26px; padding: 0;
  background: var(--sunken); border: 1px solid var(--rule); border-radius: 5px; color: var(--ink-2);
  opacity: 0; transition: opacity 140ms linear;
}
.seam:hover .seam-collapse, .seam:focus-within .seam-collapse { opacity: 1; }
.seam-collapse svg { width: 10px; height: 10px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }

/* ------------------------------------------------------------ companion */

.region-companion {
  display: flex; flex-direction: column;
  background: var(--surface);
}

.companion-head {
  display: flex; align-items: center; gap: 10px;
  min-height: 46px; padding: 6px 14px;
  border-bottom: 1px solid var(--rule);
}
.companion-title { margin: 0; font-size: var(--step-1); font-weight: 600; white-space: nowrap; }
.companion-status {
  display: flex; align-items: baseline; gap: 6px; margin: 0; min-width: 0;
  color: var(--ink-2); font-size: var(--step--1);
}
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--ink-3); align-self: center; flex: none; }
.companion-status[data-state="Ready"] .status-dot { background: var(--ready); }
.companion-status[data-state="Checking"] .status-dot { background: var(--pending); }
.companion-status[data-state="Unavailable"] .status-dot { background: var(--down); }
.status-model { color: var(--ink-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vision-control {
  margin-left: auto;
  display: inline-flex; align-items: center; gap: 6px;
  min-width: 112px; justify-content: center;
}
.vision-control:disabled {
  color: var(--ink-2);
  background: var(--raised);
  opacity: 1;
}
.vision-mark {
  width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 1.5;
  stroke-linecap: round; stroke-linejoin: round;
}
.vision-state {
  color: var(--ink-3);
  font-size: 11px; font-weight: 650;
  text-transform: uppercase; letter-spacing: 0.04em;
}
.view-switch { margin-left: 0; }
.private-mode { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; flex: none; }
.companion-status { overflow: hidden; }
.vision-control { white-space: nowrap; flex: none; }
.companion-status .status-model { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.private-dot { width: 8px; height: 8px; border-radius: 50%; background: #3f9b5b; }
.private-mode[data-private="true"] { color: var(--surface); background: var(--ink); border-color: var(--ink); }
.private-mode[data-private="true"] .private-dot { background: #c9a227; }

.private-banner { display: grid; gap: 6px; margin: 10px 16px 0; padding: 10px 12px; border: 1px dashed var(--rule-strong); border-radius: 10px; background: var(--sunken); font-size: var(--step--1); }
.private-banner[hidden] { display: none; }
.private-banner { grid-column: 1; grid-row: 1; }
.workspace[data-vex-private="true"] .conversation { grid-template-rows: auto minmax(0, 1fr) auto; }
.workspace[data-vex-private="true"] .timeline { grid-row: 2; }
.workspace[data-vex-private="true"] .composer { grid-row: 3; }
.workspace[data-vex-private="true"] .jump-latest { grid-row: 2; }
.private-banner p { margin: 0; color: var(--ink-2); }
.private-notes { margin: 0; padding: 0 0 0 16px; color: var(--ink); }
.private-proposal { display: grid; gap: 6px; }
.private-proposal[hidden] { display: none; }
.private-proposal-text { color: var(--ink) !important; }
.private-proposal-actions { display: flex; gap: 8px; }
.private-banner > .quiet-button { justify-self: start; }
.timeline[data-private="true"] .bubble { border-style: dashed; }
.archives-private p { color: var(--ink-2); font-style: italic; }

.outline-button {
  height: 30px; padding: 0 12px;
  background: var(--surface); border: 1px solid var(--rule-strong); border-radius: 7px;
  font-size: var(--step--1); font-weight: 600;
}
.outline-button:hover:not(:disabled) { background: var(--sunken); }
.outline-button:disabled { color: var(--ink-3); border-color: var(--rule); }
.quiet-button {
  height: 28px; padding: 0 10px;
  background: transparent; border: 0; border-radius: 6px;
  color: var(--ink-2); font-size: var(--step--1); font-weight: 600;
}
.quiet-button:hover:not(:disabled) { background: var(--sunken); color: var(--ink); }

.conversation {
  display: grid; grid-template-rows: minmax(0, 1fr) auto;
  flex: 1; min-height: 0; position: relative;
}

/* ------------------------------------------------------------ timeline */

.timeline {
  grid-column: 1; grid-row: 1;
  min-height: 0; height: 100%; overflow-y: auto; overscroll-behavior: contain;
  overflow-anchor: none; scrollbar-gutter: stable;
  margin: 0; padding: 18px 10px 12px 16px; list-style: none;
}
.jump-latest {
  grid-column: 1; grid-row: 1;
  align-self: end; justify-self: center; z-index: 2;
  margin: 0 0 12px;
}

.message {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  grid-template-areas: "thought index" "bubble index" "speaker index" "receipt index";
  margin: 0 0 18px;
}
.message-index {
  grid-area: index; align-self: center; justify-self: end; margin: 0;
  color: var(--ink-3); font-size: var(--step--1); font-variant-numeric: tabular-nums;
}
.bubble {
  grid-area: bubble; position: relative;
  max-width: min(92%, 640px); padding: 10px 14px 11px;
  border-radius: 12px; border: 1px solid;
  overflow-wrap: anywhere;
}
.bubble::after {
  content: ""; position: absolute; bottom: -7px; width: 12px; height: 12px;
  background: inherit; border: inherit; border-top: 0; border-left: 0;
  transform: rotate(45deg);
}
.message-you .bubble {
  justify-self: end; background: var(--you); border-color: var(--you-rule);
  border-bottom-right-radius: 4px;
}
.message-you .bubble::after { right: 14px; }
.message-vex .bubble {
  justify-self: start; background: var(--vex); border-color: var(--vex-rule);
  border-bottom-left-radius: 4px;
  font-family: var(--voice); font-size: var(--step-1); line-height: 1.55;
}
.message-vex .bubble::after { left: 14px; }

.speaker { grid-area: speaker; margin: 9px 2px 0; font-size: var(--step--1); color: var(--ink-2); font-weight: 600; }
.message-you .speaker { justify-self: end; }
.message-vex .speaker { justify-self: start; }

.bubble p { margin: 0 0 0.6em; }
.bubble p:last-child { margin-bottom: 0; }
.bubble ul, .bubble ol { margin: 0.2em 0 0.6em; padding-left: 1.3em; }
.bubble li { margin: 0.15em 0; }
.bubble code { font-family: var(--mono); font-size: 0.86em; padding: 1px 4px; border-radius: 4px; background: var(--sunken); }
.bubble pre { margin: 0.4em 0 0.7em; padding: 10px 12px; border-radius: 8px; background: var(--sunken); overflow-x: auto; }
.bubble pre code { padding: 0; background: none; }
.prose-heading { font-weight: 650; }

.thought { grid-area: thought; justify-self: start; margin: 0 0 7px; }
.thought-toggle {
  display: inline-flex; align-items: center; gap: 6px; padding: 2px 4px; margin-left: -4px;
  background: transparent; border: 0; border-radius: 5px;
  color: var(--ink-2); font-size: var(--step--1);
}
.thought-toggle:hover { color: var(--ink); background: var(--sunken); }
.thought-mark { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 1.4; }
.thinking-now { display: flex; align-items: center; gap: 6px; color: var(--ink-2); font-size: var(--step--1); }
.thinking-now .speaker-inline { font-weight: 600; color: var(--ink-2); }
.thinking-now .thought-mark { animation: turn 2.4s linear infinite; }
.reasoning {
  max-width: min(92%, 640px); max-height: 220px; overflow-y: auto; margin-top: 7px;
  overflow-anchor: none; scrollbar-gutter: stable;
  padding: 10px 12px; border: 1px solid var(--rule-strong); border-radius: 8px;
  background: var(--raised); color: var(--ink-2);
  font-size: var(--step--1); line-height: 1.5; white-space: pre-wrap;
}
.message.waiting[data-has-content="false"] .bubble { display: none; }
.message.waiting[data-has-content="true"] .bubble { opacity: 0.92; }
.live-reasoning { margin-left: 0; }

.receipt-line {
  grid-area: receipt; margin: 6px 2px 0; color: var(--ink-3); font-size: var(--step--1);
}
.receipt-link,
.prompt-receipt-link {
  padding: 0; background: none; border: 0; color: var(--ink-2);
  text-decoration: underline; text-decoration-color: var(--rule-strong); text-underline-offset: 2px;
}
.receipt-link:hover, .prompt-receipt-link:hover { color: var(--ink); text-decoration-color: currentColor; }

.timeline-empty { margin: 18% 6px 0; color: var(--ink-2); max-width: 42ch; }
.timeline-empty-heading { margin: 0 0 6px; color: var(--ink); font-family: var(--voice); font-size: 20px; line-height: 1.3; }
.timeline-empty p { margin: 0 0 6px; }

/* ------------------------------------------------------------ composer */

.composer {
  grid-column: 1; grid-row: 2;
  padding: 8px 14px 14px; border-top: 1px solid var(--rule); background: var(--surface);
}
.response-progress { margin: 0 0 6px; color: var(--ink-2); font-size: var(--step--1); }
.response-progress[data-state="Failed"] { color: var(--down); }

.ticket {
  position: relative;
  display: flex; align-items: flex-end; gap: 12px;
  margin: 2px 0 10px; padding: 11px 14px 12px 18px;
  background: var(--paper); color: var(--paper-ink);
  border-radius: 6px 10px 10px 6px;
  box-shadow: 0 1px 0 var(--paper-rule), 0 6px 18px rgba(44, 39, 30, 0.12);
}
.ticket::before {
  content: ""; position: absolute; left: 6px; top: 8px; bottom: 8px;
  border-left: 2px dotted var(--paper-rule);
}
.ticket.arrive { animation: hand-over 220ms var(--ease); }
.ticket[data-state="Preparing"] .ticket-body { opacity: 0.7; }
.ticket-body { flex: 1; min-width: 0; }
.ticket p { margin: 0; }
.ticket-title { font-family: var(--voice); font-size: var(--step-1); font-weight: 600; line-height: 1.3; overflow-wrap: anywhere; }
.ticket-line { margin-top: 2px !important; font-size: var(--step--1); }
.ticket-source { margin-top: 4px !important; font-size: 11.5px; color: #6b604b; overflow-wrap: anywhere; }
.ticket-hash { font-family: var(--mono); white-space: nowrap; }
.ticket-diverged { margin-top: 6px !important; font-size: var(--step--1); font-weight: 600; }
.ticket-view {
  flex: none; height: 28px; padding: 0 10px;
  background: transparent; border: 1px solid var(--paper-rule); border-radius: 6px;
  color: var(--paper-ink); font-size: var(--step--1); font-weight: 600;
}
.ticket-view:hover:not(:disabled) { background: rgba(255, 255, 255, 0.4); }
.ticket-view:disabled { opacity: 0.5; }

.composer-controls { display: flex; align-items: center; gap: 12px; min-height: 30px; margin-bottom: 8px; }
.share {
  display: inline-flex; align-items: center; gap: 8px; flex: none;
  height: 28px; padding: 0 10px 0 8px;
  background: transparent; border: 1px solid var(--rule-strong); border-radius: 14px;
  font-size: var(--step--1); font-weight: 600; color: var(--ink-2);
}
.share-box { width: 13px; height: 13px; border: 1.5px solid currentColor; border-radius: 3px; }
.share[aria-pressed="true"] { color: var(--paper-ink); background: var(--paper); border-color: var(--paper-rule); }
.share[aria-pressed="true"] .share-box { background: currentColor; box-shadow: inset 0 0 0 2px var(--paper); }
.share[data-state="Unavailable"] { color: var(--down); border-color: currentColor; }
.share:disabled { opacity: 0.45; }
.composer-hold { margin: 0; color: var(--ink-2); font-size: var(--step--1); }

.compose-row { display: flex; align-items: flex-end; gap: 10px; }
.compose-row textarea {
  flex: 1; min-height: 44px; max-height: 220px; resize: none;
  padding: 11px 18px; line-height: 1.45;
  background: var(--raised); border: 1px solid var(--rule-strong); border-radius: 22px;
}
.compose-row textarea::placeholder { color: var(--ink-3); }
.send, .stop {
  flex: none; height: 44px; padding: 0 20px;
  border-radius: 9px; font-weight: 700;
}
.send { background: var(--ink); color: var(--surface); border: 1px solid var(--ink); }
.send:disabled { background: var(--sunken); color: var(--ink-3); border-color: var(--rule-strong); }
.stop { background: var(--surface); color: var(--ink); border: 1px solid var(--rule-strong); }

/* ------------------------------------------------------------ settings */

.settings { flex: 1; min-height: 0; overflow-y: auto; padding: 4px 16px 18px; }
.setting { padding: 14px 0; border-bottom: 1px solid var(--rule); }
.setting:last-child { border-bottom: 0; }
.setting h2 { margin: 0 0 9px; font-size: var(--step-0); font-weight: 650; }
.setting-row { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.setting-note-line { margin: 8px 0 0; color: var(--ink-2); font-size: var(--step--1); }
.memory-state { margin: 0 0 12px; }
.memory-label { margin: 0; font-weight: 600; }
.memory-state .setting-note-line { margin-top: 4px; }
.path { font-family: var(--mono); font-size: 11.5px; overflow-wrap: anywhere; }

.legend { display: flex; gap: 12px; margin: 0 0 9px; padding: 0; list-style: none; font-size: var(--step--1); color: var(--ink-2); }
.legend li { display: inline-flex; align-items: center; gap: 5px; }
.legend li::before { content: ""; width: 10px; height: 10px; border-radius: 3px; border: 1px solid var(--rule-strong); }
.legend-core::before { background: var(--core); }
.legend-used::before { background: var(--used); }
.legend-remaining::before { background: var(--remaining); }

.meter {
  position: relative; display: flex; height: 26px; overflow: hidden;
  border: 1px solid var(--rule-strong); border-radius: 6px; background: var(--sunken);
}
.meter > span:not(.meter-text) { flex: 0 0 auto; width: 0; transition: width 320ms var(--ease); }
.meter-core { background: var(--core); }
.meter-used { background: var(--used); }
.meter-remaining { background: var(--remaining); flex: 1 1 auto !important; }
.meter[data-known="false"] .meter-remaining { background: transparent; }
.meter-text {
  position: absolute; inset: 0; display: grid; place-items: center;
  font-size: var(--step--1); font-weight: 600; color: #1b1e22;
  font-variant-numeric: tabular-nums; text-shadow: 0 0 6px rgba(255, 255, 255, 0.6);
}
.meter[data-known="false"] .meter-text { color: var(--ink-2); text-shadow: none; }

.toggle-row { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.toggle-label {
  flex: 1; display: flex; align-items: center; height: 34px; padding: 0 12px;
  border: 1px solid var(--rule-strong); border-radius: 7px; font-weight: 600; font-size: var(--step--1);
}
.toggle-detail {
  display: flex; align-items: center; height: 34px; padding: 0 12px;
  border: 1px solid var(--rule-strong); border-radius: 7px; font-size: var(--step--1); font-variant-numeric: tabular-nums;
}
.switch {
  position: relative; flex: none; width: 74px; height: 34px; padding: 0;
  background: var(--sunken); border: 1px solid var(--rule-strong); border-radius: 7px;
}
.switch span {
  position: absolute; top: 3px; left: 3px; width: 34px; height: 26px; border-radius: 5px;
  background: var(--surface); border: 1px solid var(--rule-strong);
  transition: transform 160ms var(--ease), background 160ms linear;
}
.switch[aria-checked="true"] span { transform: translateX(32px); background: var(--switch-on); border-color: var(--switch-on); }

.home-row { display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: center; margin-top: 10px; }
.home-row[data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}"] { grid-template-columns: 1fr auto; row-gap: 10px; }
.home-label {
  display: flex; align-items: center; height: 34px; padding: 0 12px;
  border: 1px solid var(--rule); border-radius: 7px; color: var(--ink-2); font-weight: 600; font-size: var(--step--1);
}
.outline-button.wide { height: 34px; text-align: left; }
.outline-button.state { height: 34px; }

.setting-reset { padding-top: 18px; }
.danger-button {
  width: 100%; height: 42px;
  background: var(--danger); color: var(--danger-ink); border: 1px solid var(--danger); border-radius: 7px;
  font-weight: 700;
}
.danger-button:hover:not(:disabled) { filter: brightness(1.06); }
.danger-button:disabled { opacity: 0.5; }

/* ------------------------------------------------------------ sheets + overlay */

.sheet {
  width: min(720px, calc(100vw - 32px)); max-height: min(80vh, 760px);
  padding: 0; border: 1px solid var(--rule-strong); border-radius: 12px;
  background: var(--surface); color: var(--ink); box-shadow: var(--shadow);
}
.sheet::backdrop { background: rgba(10, 12, 15, 0.45); }
.sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px 10px; border-bottom: 1px solid var(--rule); }
.sheet h2 { margin: 0; font-size: var(--step-1); }
.facts { display: grid; grid-template-columns: max-content 1fr; gap: 4px 14px; margin: 12px 16px; font-size: var(--step--1); }
.facts dt { color: var(--ink-3); }
.facts dd { margin: 0; overflow-wrap: anywhere; }
.inspector-text {
  margin: 0 16px 16px; padding: 12px 14px; max-height: 52vh; overflow: auto;
  background: var(--paper); color: var(--paper-ink); border-radius: 8px;
  font-family: var(--voice); font-size: var(--step-0); line-height: 1.55; white-space: pre-wrap;
}
.confirm { width: min(440px, calc(100vw - 32px)); padding: 18px 18px 16px; }
.confirm p { margin: 8px 0 16px; color: var(--ink-2); }
.confirm-actions { display: flex; justify-content: flex-end; gap: 10px; }
.confirm .danger-button { width: auto; padding: 0 16px; height: 36px; }

.overlay { position: fixed; inset: auto 0 22px 0; display: flex; justify-content: center; pointer-events: none; z-index: 20; }
.snackbar {
  margin: 0; padding: 10px 16px; max-width: min(520px, calc(100vw - 32px));
  background: var(--ink); color: var(--surface); border-radius: 9px; box-shadow: var(--shadow);
  font-size: var(--step--1);
}
.binding-failure {
  position: fixed; inset: 16px 16px auto; margin: 0; padding: 14px 16px;
  background: var(--danger); color: #fff; border-radius: 9px; font-weight: 600; z-index: 30;
}

@keyframes hand-over {
  from { transform: translateX(-14px); opacity: 0; }
  to { transform: none; opacity: 1; }
}
@keyframes turn { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; transition: none !important; }
}

/* ------------------------------------------------------------ narrow screens */

@media (max-width: 860px) {
  .workspace { grid-template-rows: auto minmax(0, 1fr); }
  .chrome {
    grid-template-columns: auto minmax(0, 1fr) auto;
    padding: 10px 12px; row-gap: 10px;
  }
  .brand-name { display: none; }
  .utilities { grid-column: 1 / -1; justify-self: stretch; padding: 0; background: none; border: 0; overflow-x: auto; }
  .frame, .frame[data-collapsed] {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: 34vh 0 minmax(0, 1fr);
  }
  .seam { display: none; }
  .frame .region-browser, .frame .region-companion { visibility: visible; grid-column: 1; }
  .frame .region-browser { grid-row: 1; }
  .frame .region-companion { grid-row: 3; }
  .region-browser { border-bottom: 1px solid var(--rule); }
}

.message.waiting .speaker { display: none; }
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
