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
/* VexLife-Prod workspace presentation, aligned with the Vextreme Design Foundation v2
   (docs/foundation/DESIGN.md). Tokens only; dark is authored, light is authored; the person's
   system theme is followed unless they choose one. The context ticket is the one inverse object:
   where what the person sees becomes what Vex is given.
   [VXG RealForever] */

:root {
  /* Vextreme Design Foundation v2 (tokens/colors.css): black and white are identity, a cool gray
     ramp is structure, status hues mark state only. Light is authored, not inverted. */
  --gray-0: oklch(99% 0.002 258);
  --gray-1: oklch(97.5% 0.003 258);
  --gray-2: oklch(95.5% 0.004 258);
  --gray-3: oklch(92.5% 0.005 258);
  --gray-4: oklch(88% 0.006 258);
  --gray-5: oklch(80% 0.007 258);
  --gray-6: oklch(66% 0.008 258);
  --gray-7: oklch(48% 0.008 258);
  --gray-8: oklch(34% 0.006 258);
  --gray-9: oklch(15% 0.004 258);
  --black: oklch(9% 0.003 258);
  --white: oklch(99% 0.002 258);

  --bg-canvas: var(--gray-0);
  --bg-surface: var(--white);
  --bg-surface-raised: var(--white);
  --bg-overlay: var(--gray-2);
  --bg-inverse: var(--black);
  --text-primary: var(--gray-9);
  --text-secondary: var(--gray-8);
  --text-tertiary: var(--gray-7);
  --text-inverse: var(--white);
  --border-subtle: var(--gray-4);
  --border-default: var(--gray-5);
  --border-strong: var(--gray-6);
  --border-focus: var(--gray-9);
  --accent-bg: var(--black);
  --accent-text-on: var(--white);

  /* Evidentiary state only: never decoration. */
  --status-success: oklch(46% 0.11 155);
  --status-caution: oklch(52% 0.11 75);
  --status-critical: oklch(50% 0.16 25);
  --critical-bg: oklch(50% 0.17 25);
  --critical-text: var(--white);

  /* VexLife extensions, drawn from the same ramp. */
  --speaker-you-bg: var(--gray-2);
  --speaker-you-border: var(--gray-5);
  --speaker-vex-bg: var(--white);
  --speaker-vex-border: var(--gray-4);
  --ticket-bg: var(--bg-inverse);
  --ticket-text: var(--text-inverse);
  --ticket-text-secondary: oklch(80% 0.006 258);
  --ticket-border: oklch(34% 0.006 258);
  --ticket-shadow: oklch(15% 0.004 258 / 0.18);
  --ticket-hover: oklch(24% 0.006 258);
  --page-canvas: oklch(100% 0 0);
  --scrim: oklch(9% 0.003 258 / 0.5);
  --shadow-2: 0 8px 24px oklch(60% 0.02 258 / 0.18);

  /* Relationship legend (docs/foundation/DESIGN.md): how something relates to Vex. One quiet,
     low-chroma hue per relationship at one lightness, used only as a rail or dot beside the name
     it marks, the same wherever that relationship is shown. The context meter is the legend in
     miniature: foundation, conversation, and what is left. */
  --relation-foundation: oklch(50% 0.11 300);
  --relation-memory: oklch(55% 0.11 75);
  --relation-continuity: oklch(50% 0.11 330);
  --relation-conversation: oklch(50% 0.1 265);
  --relation-reach: oklch(52% 0.08 195);
  --relation-person: oklch(50% 0.1 230);
  --relation-process: oklch(52% 0.1 0);
  --relation-output: oklch(52% 0.09 150);
  --relation-after: oklch(52% 0.012 258);
  --relation-runtime: oklch(50% 0.04 250);
  --relation-exact: var(--text-tertiary);

  --font-sans: "IBM Plex Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif;
  --font-serif: "Source Serif 4", "Iowan Old Style", Georgia, serif;
  --font-mono: "IBM Plex Mono", ui-monospace, "SF Mono", Menlo, monospace;
  --step--2: 0.6875rem;
  --step--1: 0.75rem;
  --step-0: 0.875rem;
  --step-1: 1rem;
  --step-2: 1.125rem;
  --step-3: 1.375rem;

  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 24px;
  --space-6: 32px;
  --space-7: 48px;
  --space-8: 64px;
  --space-9: 96px;
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 10px;
  --radius-full: 999px;
  --chrome-height: 64px;
  --ease-standard: cubic-bezier(0.2, 0.6, 0.2, 1);
  --duration-fast: 120ms;
  --duration-base: 180ms;
  color-scheme: light;
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --gray-0: oklch(13.5% 0.004 258);
    --gray-1: oklch(16.5% 0.004 258);
    --gray-2: oklch(20% 0.005 258);
    --gray-3: oklch(24.5% 0.006 258);
    --gray-4: oklch(30% 0.007 258);
    --gray-5: oklch(38% 0.008 258);
    --gray-6: oklch(48% 0.008 258);
    --gray-7: oklch(66% 0.007 258);
    --gray-8: oklch(80% 0.005 258);
    --gray-9: oklch(97% 0.002 258);
    --bg-canvas: var(--gray-0);
    --bg-surface: var(--gray-1);
    --bg-surface-raised: var(--gray-2);
    --bg-overlay: var(--gray-3);
    --bg-inverse: var(--white);
    --text-inverse: var(--black);
    --accent-bg: var(--white);
    --accent-text-on: var(--black);
    --status-success: oklch(72% 0.12 155);
    --status-caution: oklch(78% 0.13 85);
    --status-critical: oklch(70% 0.15 25);
    --critical-bg: oklch(52% 0.17 25);
    --speaker-you-bg: var(--gray-3);
    --speaker-you-border: var(--gray-5);
    --speaker-vex-bg: var(--gray-1);
    --speaker-vex-border: var(--gray-4);
    --ticket-text-secondary: oklch(42% 0.008 258);
    --ticket-border: oklch(80% 0.005 258);
    --ticket-shadow: oklch(0% 0 0 / 0.5);
    --ticket-hover: oklch(92% 0.004 258);
    --shadow-2: 0 4px 16px oklch(4% 0.003 258 / 0.45);
    --relation-foundation: oklch(74% 0.075 300);
    --relation-memory: oklch(78% 0.085 80);
    --relation-continuity: oklch(74% 0.075 330);
    --relation-conversation: oklch(74% 0.07 265);
    --relation-reach: oklch(76% 0.07 195);
    --relation-person: oklch(74% 0.07 230);
    --relation-process: oklch(74% 0.075 0);
    --relation-output: oklch(75% 0.07 150);
    --relation-after: oklch(70% 0.012 258);
    --relation-runtime: oklch(72% 0.035 250);
    color-scheme: dark;
  }
}

:root[data-theme="dark"] {
  --gray-0: oklch(13.5% 0.004 258);
  --gray-1: oklch(16.5% 0.004 258);
  --gray-2: oklch(20% 0.005 258);
  --gray-3: oklch(24.5% 0.006 258);
  --gray-4: oklch(30% 0.007 258);
  --gray-5: oklch(38% 0.008 258);
  --gray-6: oklch(48% 0.008 258);
  --gray-7: oklch(66% 0.007 258);
  --gray-8: oklch(80% 0.005 258);
  --gray-9: oklch(97% 0.002 258);
  --bg-canvas: var(--gray-0);
  --bg-surface: var(--gray-1);
  --bg-surface-raised: var(--gray-2);
  --bg-overlay: var(--gray-3);
  --bg-inverse: var(--white);
  --text-inverse: var(--black);
  --accent-bg: var(--white);
  --accent-text-on: var(--black);
  --status-success: oklch(72% 0.12 155);
  --status-caution: oklch(78% 0.13 85);
  --status-critical: oklch(70% 0.15 25);
  --critical-bg: oklch(52% 0.17 25);
  --speaker-you-bg: var(--gray-3);
  --speaker-you-border: var(--gray-5);
  --speaker-vex-bg: var(--gray-1);
  --speaker-vex-border: var(--gray-4);
  --ticket-text-secondary: oklch(42% 0.008 258);
  --ticket-border: oklch(80% 0.005 258);
  --ticket-shadow: oklch(0% 0 0 / 0.5);
  --ticket-hover: oklch(92% 0.004 258);
  --shadow-2: 0 4px 16px oklch(4% 0.003 258 / 0.45);
  --relation-foundation: oklch(74% 0.075 300);
  --relation-memory: oklch(78% 0.085 80);
  --relation-continuity: oklch(74% 0.075 330);
  --relation-conversation: oklch(74% 0.07 265);
  --relation-reach: oklch(76% 0.07 195);
  --relation-person: oklch(74% 0.07 230);
  --relation-process: oklch(74% 0.075 0);
  --relation-output: oklch(75% 0.07 150);
  --relation-after: oklch(70% 0.012 258);
  --relation-runtime: oklch(72% 0.035 250);
  color-scheme: dark;
}

* { box-sizing: border-box; }
html, body { margin: 0; height: 100%; }
body {
  background: var(--bg-canvas);
  color: var(--text-primary);
  font: 400 var(--step-0)/1.45 var(--font-sans);
  -webkit-font-smoothing: antialiased;
}
button, input, select, textarea { font: inherit; color: inherit; }
button { cursor: pointer; }
button:disabled { cursor: default; }
svg { display: block; }
[hidden] { display: none !important; }
:focus-visible { outline: 2px solid var(--border-focus); outline-offset: 2px; }

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
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-subtle);
}

.brand {
  display: flex; align-items: center; gap: 10px;
  color: var(--text-primary); text-decoration: none;
}
.brand-mark {
  display: grid; place-items: center;
  width: 30px; height: 30px; flex: 0 0 30px;
  color: var(--text-primary);
}
.brand-mark svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 5; stroke-linecap: round; stroke-linejoin: round; }
.brand-name { font-weight: 650; font-size: var(--step-2); letter-spacing: -0.01em; }

.address { position: relative; display: flex; align-items: center; min-width: 0; }
.address input {
  width: 100%; height: 36px; padding: 0 40px 0 var(--space-3);
  background: var(--bg-surface-raised); border: 1px solid var(--border-default); border-radius: var(--radius-md);
  text-overflow: ellipsis;
}
.address input::placeholder { color: var(--text-tertiary); }
.address .icon-button { position: absolute; right: 4px; }
.address-error {
  position: absolute; top: calc(100% + 6px); left: 0; z-index: 5; margin: 0;
  padding: 7px 10px; border-radius: var(--radius-md); background: var(--bg-inverse); color: var(--text-inverse);
  font-size: var(--step--1); box-shadow: var(--shadow-2);
}

.icon-button {
  display: grid; place-items: center; width: 30px; height: 30px; padding: 0;
  background: transparent; border: 0; border-radius: var(--radius-md); color: var(--text-secondary);
}
.icon-button svg { width: 17px; height: 17px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.icon-button:hover:not(:disabled) { background: var(--bg-overlay); color: var(--text-primary); }
.icon-button:disabled { opacity: 0.4; }

.app-selector select {
  width: 172px; height: 36px; padding: 0 30px 0 14px;
  background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-md);
  font-weight: 600;
}

.utilities {
  justify-self: end; align-self: stretch;
  display: flex; align-items: center; padding: 0 14px;
  background: var(--bg-overlay); border-left: 1px solid var(--border-subtle);
}
.quick-actions { display: flex; align-items: center; gap: 10px; margin: 0; padding: 0; list-style: none; }
.quick-actions > li { display: block; flex: none; }
.quick-actions > li[hidden] { display: none; }
.quick-actions[data-open="false"] > .quick-action-item { display: none; }
.quick-action {
  display: grid; place-items: center; width: 42px; height: 42px; padding: 0;
  background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 50%;
  color: var(--text-secondary);
}
.quick-action svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.quick-action:hover { color: var(--text-primary); border-color: var(--border-default); }
.quick-action[data-bound="false"] { color: var(--text-tertiary); }
.quick-action[aria-expanded="true"], .quick-action[data-active="true"] { color: var(--text-primary); border-color: var(--text-secondary); }
.quick-action-glyph { font-size: var(--step-2); line-height: 1; }
.quick-action-trigger {
  font-size: var(--step-3);
  transition: background var(--duration-base) var(--ease-standard), border-color var(--duration-base) var(--ease-standard),
    color var(--duration-base) var(--ease-standard), transform var(--duration-base) var(--ease-standard);
}
.quick-action-trigger[aria-expanded="true"] { transform: rotate(90deg); }

.utilities { position: relative; }
.quick-action[data-attention="true"] { position: relative; }
.quick-action[data-attention="true"]::after {
  content: ""; position: absolute; top: 3px; right: 3px; width: 8px; height: 8px;
  border: 2px solid var(--bg-surface); border-radius: 50%; background: var(--critical-bg);
}

.release-alerts-panel {
  position: absolute; top: calc(100% + 8px); right: 12px; z-index: 30; width: min(330px, calc(100vw - 24px));
  display: grid; gap: var(--space-2); padding: var(--space-3) 14px;
  background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-lg); box-shadow: var(--shadow-2);
}
.release-alerts-panel[hidden] { display: none; }
.release-alerts-title { margin: 0; font-weight: 650; }
.release-alerts-empty { margin: 0; color: var(--text-secondary); font-size: var(--step--1); }
.release-alerts-list { margin: 0; padding: 0; list-style: none; display: grid; }
.release-alerts-item {
  display: grid; gap: var(--space-2);
  padding: 10px 0; border-top: 1px solid var(--border-subtle);
}
.release-alerts-item:first-child { border-top: 0; }
.release-alerts-item[data-unread="true"] .release-alerts-version { font-weight: 750; }
.release-alerts-version { color: var(--text-primary); }
.release-alerts-status { color: var(--text-secondary); font-size: var(--step--1); }
.release-alerts-date { color: var(--text-tertiary); font-size: var(--step--1); white-space: nowrap; }
.release-alerts-item-head {
  display: grid; grid-template-columns: auto 1fr auto; gap: var(--space-1) 10px; align-items: baseline;
}
.release-alerts-controls { display: flex; gap: var(--space-2); justify-content: flex-end; }
.release-alerts-view, .release-alerts-dismiss {
  min-height: 30px; padding: 0 10px; border: 1px solid var(--border-default); border-radius: var(--radius-md);
  background: var(--bg-surface); color: var(--text-primary); font-weight: 600; font-size: var(--step--1);
}
.release-alerts-view:hover, .release-alerts-dismiss:hover { background: var(--bg-overlay); }


.speech {
  position: absolute; top: calc(100% + 8px); right: 12px; z-index: 30; width: 320px;
  display: grid; gap: 10px; padding: var(--space-3) 14px;
  background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-lg); box-shadow: var(--shadow-2);
}
.speech[hidden] { display: none; }
.language-panel {
  position: absolute; top: calc(100% + 8px); right: 12px; z-index: 30; width: 260px;
  display: grid; gap: var(--space-2); padding: var(--space-3) 14px;
  background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-lg); box-shadow: var(--shadow-2);
}
.language-panel[hidden] { display: none; }
.language-title { margin: 0; font-weight: 650; }
.language-list { margin: 0; padding: 0; list-style: none; display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-1); }
.language-list button { width: 100%; padding: 6px 10px; text-align: left; border: 1px solid transparent; border-radius: var(--radius-md); background: none; color: var(--text-primary); }
.language-list button:hover { background: var(--bg-overlay); }
.language-list button[aria-selected="true"] { border-color: var(--border-default); background: var(--bg-overlay); font-weight: 650; }
.language-note { margin: 0; color: var(--text-secondary); font-size: var(--step--1); }
.speech-head { display: flex; align-items: center; justify-content: space-between; gap: var(--space-2); }
.speech-title { font-weight: 650; }
.speech-status { margin: 0; color: var(--text-secondary); font-size: var(--step--1); }
.speech-field { display: grid; grid-template-columns: 48px minmax(0, 1fr) auto; align-items: center; gap: var(--space-2); font-size: var(--step--1); color: var(--text-secondary); }
.speech-field select { grid-column: 2 / -1; min-width: 0; height: 30px; background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-md); color: var(--text-primary); }
.speech-check { display: flex; align-items: center; gap: var(--space-2); font-size: var(--step--1); color: var(--text-primary); }
.speech-controls { display: flex; align-items: center; gap: 6px; }
.speech-controls .quiet-button { margin-left: auto; }
.speech-controls .icon-button svg path, .speech-controls .icon-button svg rect { fill: currentColor; stroke: none; }
.speech-controls .icon-button svg path.glyph-line, .speech-controls .icon-button svg path.glyph-pause { fill: none; stroke: currentColor; stroke-width: 2; }
.speech-play .glyph-pause, .speech[data-mode="playing"] .speech-play .glyph-play { display: none; }
.speech[data-mode="playing"] .speech-play .glyph-pause { display: inline; }
.message.reading .bubble { outline: 2px solid var(--text-secondary); outline-offset: 2px; }
.timeline[data-choosing="true"] .message { cursor: pointer; }
.timeline[data-choosing="true"] .message:hover .bubble { outline: 1px dashed var(--text-secondary); outline-offset: 2px; }

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

.region-browser { display: flex; flex-direction: column; background: var(--bg-canvas); }
.frame-status {
  margin: 0; padding: var(--space-2) 14px;
  background: var(--ticket-bg); color: var(--ticket-text);
  border-bottom: 1px solid var(--ticket-border);
  font-size: var(--step--1);
}
.frame-status[data-frame="Loading"] { background: var(--bg-overlay); color: var(--text-secondary); border-bottom-color: var(--border-subtle); }
.viewport { position: relative; flex: 1; min-height: 0; }
.viewport #page-frame { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; background: var(--page-canvas); }
.context-frame {
  position: absolute; width: 1px; height: 1px; left: -10000px; top: -10000px;
  border: 0; opacity: 0; pointer-events: none;
}
.viewport-empty { position: absolute; inset: 0; display: grid; place-items: center; color: var(--text-tertiary); background: var(--bg-canvas); }
.viewport[hidden] { display: none; }

.family { display: flex; align-items: center; gap: var(--space-1); padding: 6px var(--space-3); border-bottom: 1px solid var(--border-subtle); background: var(--bg-surface); }
.family[hidden] { display: none; }
.family-member { padding: 5px var(--space-3); border: 1px solid transparent; border-radius: var(--radius-full); background: none; color: var(--text-secondary); font-weight: 600; }
.family-member:hover { color: var(--text-primary); background: var(--bg-overlay); }
.family-member[aria-current="page"] { color: var(--text-primary); border-color: var(--border-default); background: var(--bg-overlay); }
.family-note { margin-left: auto; color: var(--text-tertiary); font-size: var(--step--1); }

.archives { flex: 1; min-height: 0; overflow-y: auto; padding: 18px 22px 28px; background: var(--bg-canvas); }
.archives[hidden] { display: none; }
.archives-head h2 { margin: 0; font-size: var(--step-1); }
.archives-summary { margin: var(--space-1) 0 var(--space-4); color: var(--text-secondary); font-size: var(--step--1); }
.archives-timeline { margin: 0; padding: 0; list-style: none; display: grid; gap: 18px; }
.archives-day h3 { margin: 0 0 var(--space-2); font-size: var(--step-0); }
.archives-day h3 span { margin-left: var(--space-2); color: var(--text-tertiary); font-weight: 500; font-size: var(--step--1); }
.archives-entries { margin: 0; padding: 0; list-style: none; display: grid; gap: 6px; border-left: 2px solid var(--border-subtle); }
.archives-entry { display: grid; grid-template-columns: 56px 40px minmax(0, 1fr); gap: var(--space-2); padding: 2px 0 2px var(--space-3); font-size: var(--step--1); }
.archives-entry time { color: var(--text-tertiary); font-variant-numeric: tabular-nums; }
.archives-entry .who { color: var(--text-secondary); font-weight: 600; }
.archives-entry p { margin: 0; color: var(--text-primary); white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; cursor: pointer; }
.archives-entry p[data-open="true"] { display: block; -webkit-line-clamp: unset; }
.archives-empty { color: var(--text-tertiary); }
.address[data-app="archives"],
.address[data-app="vex-context"] { opacity: 0.5; pointer-events: none; }

/* ------------------------------------------------------------ Vex context */

.vex-context {
  flex: 1; min-height: 0; overflow-y: auto;
  padding: 18px 22px 36px; background: var(--bg-canvas);
}
.vex-context[hidden] { display: none; }
.vex-context h2, .vex-context h3 { margin: 0; }
.vex-context-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-4); margin-bottom: 14px;
}
.vex-context-head h2 { font-size: var(--step-2); }
.vex-context-head p, .context-causal-heading p {
  margin: var(--space-1) 0 0; color: var(--text-secondary); font-size: var(--step--1); line-height: 1.45;
}
.context-profile-badge, .context-budget {
  flex: none; padding: 5px 9px; border: 1px solid var(--border-default); border-radius: var(--radius-full);
  background: var(--bg-surface); color: var(--text-secondary); font-size: var(--step--1); font-weight: 650;
}
.context-profile-card, .context-changes, .context-causal {
  margin-bottom: 14px; padding: 14px;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); background: var(--bg-surface);
}
.context-profile-title, .context-causal-heading {
  display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-3);
}
.context-profile-title h3, .context-changes h3, .context-causal-heading h3 { font-size: var(--step-1); }
.context-profile-title p, .context-profile-note {
  margin: 3px 0 0; color: var(--text-secondary); font-size: var(--step--1); line-height: 1.4;
}
.context-profile-field {
  display: grid; grid-template-columns: 132px minmax(0, 1fr); align-items: center; gap: 10px;
  margin-top: var(--space-2); color: var(--text-secondary); font-size: var(--step--1);
}
.context-profile-field input, .context-profile-field select, .context-profile-field textarea {
  min-width: 0; padding: 7px 9px; border: 1px solid var(--border-default); border-radius: var(--radius-md);
  background: var(--bg-surface); color: var(--text-primary); font: inherit;
}
.context-profile-field textarea { resize: vertical; line-height: 1.45; }
.context-profile-foundation-text { align-items: start; }
.context-profile-foundation-text small { grid-column: 2; color: var(--text-tertiary); }
.context-profile-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: var(--space-3); }
.context-profile-actions a, .context-profile-actions label { text-decoration: none; cursor: pointer; box-sizing: border-box; display: inline-flex; align-items: center; }
.context-profile-note:empty { display: none; }

.context-change-list { display: grid; gap: 7px; margin-top: 9px; }
.context-change {
  display: grid; grid-template-columns: 8px minmax(0, 1fr); gap: 9px; padding: var(--space-2) 10px;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-md); background: var(--bg-surface-raised);
}
.context-change::before { content: ""; width: 8px; height: 8px; margin-top: var(--space-1); border-radius: 50%; background: var(--text-tertiary); }
.context-change[data-state="changed"]::before { background: var(--status-caution); }
.context-change[data-state="current"]::before { background: var(--status-success); }
.context-change p { margin: 0; color: var(--text-secondary); font-size: var(--step--1); line-height: 1.4; }
.context-change strong { color: var(--text-primary); }
.context-empty { margin: var(--space-2) 0 0; color: var(--text-tertiary); font-size: var(--step--1); }

.context-stack { display: grid; gap: 7px; }
.context-section {
  border: 1px solid var(--border-subtle); border-left-width: 4px; border-radius: var(--radius-md);
  background: var(--bg-surface); overflow: clip;
}
.context-section summary {
  display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-3);
  padding: 10px var(--space-3); cursor: pointer; font-weight: 650; list-style-position: inside;
}
.context-section summary small { margin-left: auto; color: var(--text-secondary); font-weight: 500; font-size: var(--step--1); }
.context-section-body { padding: 0 var(--space-3) var(--space-3); color: var(--text-secondary); font-size: var(--step--1); line-height: 1.5; }
.context-process-setting {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  margin: 0 var(--space-3) var(--space-2); padding: var(--space-2) 10px;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-md); background: var(--bg-surface);
  color: var(--text-secondary); font-size: var(--step--1);
}
.context-process-setting select {
  min-width: 190px; padding: 6px var(--space-2); border: 1px solid var(--border-default); border-radius: var(--radius-md);
  background: var(--bg-surface); color: var(--text-primary); font: inherit;
}
.context-section-body p { margin: 6px 0 0; }
.context-section-body pre {
  max-height: 300px; overflow: auto; margin: var(--space-2) 0 0; padding: 10px;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-md); background: var(--bg-canvas);
  color: var(--text-primary); font: 11.5px/1.45 var(--font-mono); white-space: pre-wrap; overflow-wrap: anywhere;
}
.context-evidence-disclosure {
  margin: var(--space-2) 0 0; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); background: var(--bg-surface);
}
.context-evidence-disclosure > summary {
  display: list-item; padding: var(--space-2) 10px; cursor: pointer;
  color: var(--text-secondary); font-weight: 600; font-size: var(--step--1);
}
.context-evidence-disclosure > pre { margin: 0 var(--space-2) var(--space-2); }
/* One rule draws every relationship: a rail in its hue and the faintest wash of it. */
.context-section[data-context-kind="foundation"] { --relation: var(--relation-foundation); }
.context-section[data-context-kind="memory"] { --relation: var(--relation-memory); }
.context-section[data-context-kind="continuity"] { --relation: var(--relation-continuity); }
.context-section[data-context-kind="conversation"] { --relation: var(--relation-conversation); }
.context-section[data-context-kind="browser"] { --relation: var(--relation-reach); }
.context-section[data-context-kind="request"] { --relation: var(--relation-person); }
.context-section[data-context-kind="process"] { --relation: var(--relation-process); }
.context-section[data-context-kind="output"] { --relation: var(--relation-output); }
.context-section[data-context-kind="after"] { --relation: var(--relation-after); }
.context-section[data-context-kind="exact"] { --relation: var(--relation-exact); }
.context-section[data-context-kind] { border-left-color: var(--relation); background: color-mix(in oklch, var(--relation) 4%, var(--bg-surface)); }
.context-flow-arrow { padding: 5px 0; text-align: center; color: var(--text-tertiary); font-size: var(--step-2); }
.context-component-list { margin: var(--space-1) 0 0; padding: 0 0 0 22px; font-size: var(--step--1); }
.context-component-list li { margin: 6px 0; color: var(--text-secondary); }
.context-component-list code { color: var(--text-primary); font-family: var(--font-mono); font-size: var(--step--2); }
.context-chip {
  display: inline-flex; align-items: center; margin: 2px var(--space-1) 2px 0; padding: 2px 6px;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-full); background: var(--bg-surface);
  color: var(--text-secondary); font-size: var(--step--2);
}
.context-exact-message { padding: var(--space-2) 10px; margin-top: 7px; border-radius: var(--radius-md); background: var(--bg-surface); border: 1px solid var(--border-subtle); }
.context-exact-message[data-role="human"] { border-left: 3px solid var(--relation-person); }
.context-exact-message[data-role="vex"] { border-left: 3px solid var(--relation-output); }
.context-exact-message-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.context-exact-message p { white-space: pre-wrap; overflow-wrap: anywhere; color: var(--text-primary); }
.context-exact-message p[hidden] { display: none; }
.context-exact-message small { color: var(--text-tertiary); }
.context-message-toggle {
  flex: none; padding: 2px 5px; border: 0; border-radius: var(--radius-sm); background: transparent;
  color: var(--text-secondary); font: 600 11px/1.2 var(--font-sans); cursor: pointer;
}
.context-message-toggle:hover, .context-message-toggle:focus-visible { background: var(--bg-surface-raised); color: var(--text-primary); }
.context-profile-card[data-dirty="true"] { border-color: var(--status-caution); }

.seam {
  position: relative;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px;
  background: var(--bg-surface);
  border-inline: 1px solid var(--border-subtle);
  cursor: col-resize; touch-action: none;
}
.seam:focus-visible { outline-offset: -2px; }
.seam-grip { color: var(--text-tertiary); }
.seam-grip svg { width: 22px; height: 12px; fill: none; stroke: currentColor; stroke-width: 1.4; stroke-linecap: round; stroke-linejoin: round; transform: scale(0.8); }
.seam:hover .seam-grip, .seam:focus-visible .seam-grip { color: var(--text-primary); }
.seam-collapse {
  display: grid; place-items: center; width: 18px; height: 26px; padding: 0;
  background: var(--bg-overlay); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); color: var(--text-secondary);
  opacity: 0; transition: opacity var(--duration-fast) linear;
}
.seam:hover .seam-collapse, .seam:focus-within .seam-collapse { opacity: 1; }
.seam-collapse svg { width: 10px; height: 10px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }

/* ------------------------------------------------------------ companion */

.region-companion {
  display: flex; flex-direction: column;
  background: var(--bg-surface);
}

.companion-head {
  display: flex; align-items: center; gap: 10px;
  min-height: 46px; padding: 6px 14px;
  border-bottom: 1px solid var(--border-subtle);
}
.companion-title { margin: 0; font-size: var(--step-1); font-weight: 600; white-space: nowrap; }
.companion-status {
  display: flex; align-items: baseline; gap: 6px; margin: 0; min-width: 0;
  color: var(--text-secondary); font-size: var(--step--1);
}
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--text-tertiary); align-self: center; flex: none; }
.companion-status[data-state="Ready"] .status-dot { background: var(--status-success); }
.companion-status[data-state="Checking"] .status-dot { background: var(--status-caution); }
.companion-status[data-state="Unavailable"] .status-dot { background: var(--status-critical); }
.status-model { color: var(--text-tertiary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vision-control {
  margin-left: auto;
  display: inline-flex; align-items: center; gap: 6px;
  min-width: 112px; justify-content: center;
}
.vision-control:disabled {
  color: var(--text-secondary);
  background: var(--bg-surface-raised);
  opacity: 1;
}
.vision-mark {
  width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 1.5;
  stroke-linecap: round; stroke-linejoin: round;
}
.vision-state {
  color: var(--text-tertiary);
  font-size: var(--step--2); font-weight: 650;
  text-transform: uppercase; letter-spacing: 0.04em;
}
.view-switch { margin-left: 0; }
.private-mode { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; flex: none; }
.companion-status { overflow: hidden; }
.vision-control { white-space: nowrap; flex: none; }
.companion-status .status-model { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.private-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--status-success); }
.private-mode[data-private="true"] { color: var(--accent-text-on); background: var(--accent-bg); border-color: var(--accent-bg); }
.private-mode[data-private="true"] .private-dot { background: var(--status-caution); }

.private-banner { display: grid; gap: 6px; margin: 10px var(--space-4) 0; padding: 10px var(--space-3); border: 1px dashed var(--border-default); border-radius: var(--radius-md); background: var(--bg-overlay); font-size: var(--step--1); }
.private-banner[hidden] { display: none; }
.private-banner { grid-column: 1; grid-row: 1; }
.workspace[data-vex-private="true"] .conversation { grid-template-rows: auto minmax(0, 1fr) auto; }
.workspace[data-vex-private="true"] .timeline { grid-row: 2; }
.workspace[data-vex-private="true"] .composer { grid-row: 3; }
.workspace[data-vex-private="true"] .jump-latest { grid-row: 2; }
.private-banner p { margin: 0; color: var(--text-secondary); }
.private-notes { margin: 0; padding: 0 0 0 var(--space-4); color: var(--text-primary); }
.private-proposal { display: grid; gap: 6px; }
.private-proposal[hidden] { display: none; }
.private-proposal-text { color: var(--text-primary) !important; }
.private-proposal-actions { display: flex; gap: var(--space-2); }
.private-banner > .quiet-button { justify-self: start; }
.timeline[data-private="true"] .bubble { border-style: dashed; }
.archives-private p { color: var(--text-secondary); font-style: italic; }

.outline-button {
  height: 30px; padding: 0 var(--space-3);
  background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-md);
  font-size: var(--step--1); font-weight: 600;
}
.outline-button:hover:not(:disabled) { background: var(--bg-overlay); }
.outline-button:disabled { color: var(--text-tertiary); border-color: var(--border-subtle); }
.quiet-button {
  height: 28px; padding: 0 10px;
  background: transparent; border: 0; border-radius: var(--radius-md);
  color: var(--text-secondary); font-size: var(--step--1); font-weight: 600;
}
.quiet-button:hover:not(:disabled) { background: var(--bg-overlay); color: var(--text-primary); }

.conversation {
  display: grid; grid-template-rows: minmax(0, 1fr) auto;
  flex: 1; min-height: 0; position: relative;
}

/* ------------------------------------------------------------ timeline */

.timeline {
  grid-column: 1; grid-row: 1;
  min-height: 0; height: 100%; overflow-y: auto; overscroll-behavior: contain;
  overflow-anchor: none; scrollbar-gutter: stable;
  margin: 0; padding: 18px 10px var(--space-3) var(--space-4); list-style: none;
}
.jump-latest {
  grid-column: 1; grid-row: 1;
  align-self: end; justify-self: center; z-index: 2;
  margin: 0 0 var(--space-3);
}

.message {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  grid-template-areas: "thought index" "bubble index" "speaker index" "receipt index";
  margin: 0 0 18px;
}
.message-index {
  grid-area: index; align-self: center; justify-self: end; margin: 0;
  color: var(--text-tertiary); font-size: var(--step--1); font-variant-numeric: tabular-nums;
}
.bubble {
  grid-area: bubble; position: relative;
  max-width: 96%; padding: 10px 14px 11px;
  border-radius: var(--radius-lg); border: 1px solid;
  overflow-wrap: anywhere;
}
.bubble::after {
  content: ""; position: absolute; bottom: -7px; width: 12px; height: 12px;
  background: inherit; border: inherit; border-top: 0; border-left: 0;
  transform: rotate(45deg);
}
.message-you .bubble {
  justify-self: end; background: var(--speaker-you-bg); border-color: var(--speaker-you-border);
  border-bottom-right-radius: 4px;
}
.message-you .bubble::after { right: 14px; }
.message-vex .bubble {
  justify-self: start; background: var(--speaker-vex-bg); border-color: var(--speaker-vex-border);
  border-bottom-left-radius: 4px;
  font-family: var(--font-serif); font-size: var(--step-1); line-height: 1.55;
}
.message-vex .bubble::after { left: 14px; }

.speaker { grid-area: speaker; margin: 9px 2px 0; font-size: var(--step--1); color: var(--text-secondary); font-weight: 600; }
.message-you .speaker { justify-self: end; }
.message-vex .speaker { justify-self: start; }

.bubble > p { margin: 0 0 0.6em; }
.bubble > p:last-child { margin-bottom: 0; }

/* TextPresentation is shared by Vex replies and human-visible reasoning. Markdown is the current
   interpretation grammar; these rules are Web manifestation only. The host surface keeps scroll
   ownership: timeline for replies, .reasoning for thinking. Tables/code therefore wrap and never
   introduce another presentation-owned scroller. */
.text-presentation { min-width: 0; white-space: normal; }
.text-presentation p { margin: 0 0 0.6em; }
.text-presentation p:last-child { margin-bottom: 0; }
.text-presentation ul, .text-presentation ol { margin: 0.2em 0 0.6em; padding-left: 1.3em; }
.text-presentation li { margin: 0.15em 0; }
.text-presentation code { font-family: var(--font-mono); font-size: 0.86em; padding: 1px var(--space-1); border-radius: var(--radius-sm); background: var(--bg-overlay); }
.text-presentation pre { margin: 0.4em 0 0.7em; padding: 10px var(--space-3); border-radius: var(--radius-md); background: var(--bg-overlay); white-space: pre-wrap; overflow-wrap: anywhere; }
.text-presentation pre code { padding: 0; background: none; }
.prose-heading { font-weight: 650; }
.text-presentation > :first-child { margin-top: 0; }
.text-presentation > :last-child { margin-bottom: 0; }
.text-presentation .prose-heading { margin: 0.7em 0 0.35em; line-height: 1.3; }
.text-presentation .md-h1 { font-size: 1.25em; }
.text-presentation .md-h2 { font-size: 1.15em; }
.text-presentation .md-h3 { font-size: 1.05em; }
.text-presentation a { color: inherit; text-decoration: underline; text-underline-offset: 2px; }
.text-presentation blockquote { margin: 0.4em 0 0.7em; padding: 0 0 0 10px; border-left: 3px solid var(--speaker-vex-border); color: var(--text-secondary); }
.text-presentation hr { border: 0; border-top: 1px solid var(--speaker-vex-border); margin: 0.8em 0; }
.text-presentation table { width: 100%; border-collapse: collapse; margin: 0.4em 0 0.7em; font-size: 0.92em; line-height: 1.4; }
.text-presentation th, .text-presentation td { border: 1px solid var(--speaker-vex-border); padding: var(--space-1) var(--space-2); text-align: left; vertical-align: top; overflow-wrap: anywhere; }
.text-presentation th { font-weight: 650; background: var(--bg-overlay); }

.thought { grid-area: thought; justify-self: start; margin: 0 0 7px; }
.thought-toggle {
  display: inline-flex; align-items: center; gap: 6px; padding: 2px var(--space-1); margin-left: -4px;
  background: transparent; border: 0; border-radius: var(--radius-sm);
  color: var(--text-secondary); font-size: var(--step--1);
}
.thought-toggle:hover { color: var(--text-primary); background: var(--bg-overlay); }
.thought-mark { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 1.4; }
/* The mark names the relationship: a thought is Vex's process, the same rail as "Vex's working
   process" in Vex Context (docs/foundation/DESIGN.md, relationship legend). */
.thought-toggle .thought-mark { stroke: var(--relation-process); }
.thinking-now { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; color: var(--text-secondary); font-size: var(--step--1); }
.thinking-now .speaker-inline { font-weight: 600; color: var(--text-secondary); }
.thinking-now .thought-mark { animation: turn 2.4s linear infinite; }
.live-process-panel { flex: 0 0 100%; width: 100%; min-width: 0; }
.process-history { display: grid; gap: var(--space-2); margin-top: 7px; }
.process-history > * + * { margin-top: 2px; }
.process-action {
  display: flex; align-items: flex-start; gap: 7px;
  color: var(--text-secondary); font-size: var(--step--1); line-height: 1.45;
  overflow-wrap: anywhere;
}
.process-action-mark { flex: 0 0 auto; width: 13px; color: var(--text-tertiary); text-align: center; }
.process-action[data-state="RETURNED"] .process-action-mark { color: var(--status-success); }
.process-action:not([data-state="ACTIVE"]):not([data-state="RETURNED"]) .process-action-mark { color: var(--status-critical); }
.reasoning {
  max-width: 96%; max-height: 220px; overflow-y: auto; margin-top: 7px;
  overflow-anchor: none; scrollbar-gutter: stable;
  padding: 10px var(--space-3); border: 1px solid var(--border-default); border-left: 3px solid var(--relation-process);
  border-radius: var(--radius-md); background: var(--bg-surface-raised); color: var(--text-secondary);
  font-size: var(--step--1); line-height: 1.5; white-space: pre-wrap;
}
.process-history .reasoning { margin-top: 0; max-width: 98%; }
.message.waiting[data-has-content="false"] .bubble { display: none; }
.message.waiting[data-has-content="true"] .bubble { opacity: 0.92; }
.live-reasoning { margin-left: 0; }
.reaches { list-style: none; margin: 6px 0 0; padding: 0; color: var(--text-secondary); font-size: var(--step--1); }
.reaches li { margin: 2px 0; overflow-wrap: anywhere; }
.reaches li::before { content: "↳ "; color: var(--text-tertiary); }
.reached-line { overflow-wrap: anywhere; }
.model-state-card, .model-configuration, .model-import-card {
  margin: var(--space-2) 0; padding: 10px var(--space-3); border: 1px solid var(--border-default); border-left: 3px solid var(--relation-runtime);
  border-radius: var(--radius-md); background: var(--bg-surface-raised);
}
.model-import-card { display: grid; gap: var(--space-2); margin: 10px 0 var(--space-3); padding: 10px var(--space-3); }
.model-import-field { display: grid; gap: 6px; color: var(--text-secondary); font-size: var(--step--1); }
.model-import-field input {
  width: 100%; min-width: 0; min-height: 38px; padding: 0 10px;
  border: 1px solid var(--border-default); border-radius: var(--radius-md);
  background: var(--bg-surface); color: var(--text-primary);
}
.model-import-actions { display: flex; gap: var(--space-2); flex-wrap: wrap; }
.model-import-facts { margin: 0; color: var(--text-secondary); font-size: var(--step--1); }
.model-forget-button {
  width: 38px; height: 38px; padding: 0; border: 1px solid var(--border-default);
  border-radius: var(--radius-md); background: transparent; color: var(--text-secondary); font-size: var(--step-3); line-height: 1;
}
.model-forget-button:hover:not(:disabled) { background: var(--bg-overlay); color: var(--text-primary); }
.model-forget-button:disabled { opacity: 0.4; }

.model-choice-field { display: grid; gap: 7px; margin: 10px 0 var(--space-3); }
.model-choice-row {
  display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); align-items: center;
}
.model-choice-field select {
  width: 100%; min-width: 0; min-height: 38px; padding: 0 10px;
  border: 1px solid var(--border-default); border-radius: var(--radius-md);
  background: var(--bg-surface); color: var(--text-primary);
}
.model-choice-actions {
  grid-column: 1 / -1; display: grid; gap: 0; align-items: stretch;
  border: 1px solid var(--border-subtle); border-radius: var(--radius-md); overflow: hidden;
}
.model-storage-row {
  display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; align-items: center;
  padding: 9px 10px; border-top: 1px solid var(--border-subtle); background: var(--bg-surface-raised);
}
.model-storage-row:first-child { border-top: none; }
.model-storage-copy { display: grid; gap: 2px; min-width: 0; }
.model-storage-label { color: var(--text-primary); font-size: var(--step--1); font-weight: 650; }
.model-storage-detail {
  color: var(--text-secondary); font-size: var(--step--2); overflow-wrap: anywhere;
}
.model-storage-controls { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.model-storage-controls .outline-button { height: 34px; padding: 0 10px; }
.model-choice-facts, .model-choice-notes {
  margin: 0; color: var(--text-secondary); font-size: var(--step--1);
}
.model-choice-notes:empty { display: none; }
.model-configuration { display: grid; gap: 10px; }
.model-cache-field {
  display: grid; grid-template-columns: minmax(150px, auto) minmax(220px, 1fr);
  gap: 10px; align-items: center;
}
.model-cache-field select {
  width: 100%; min-width: 0; min-height: 38px; padding: 0 10px;
  border: 1px solid var(--border-default); border-radius: var(--radius-md);
  background: var(--bg-surface); color: var(--text-primary);
}
.model-configuration-actions { display: flex; gap: var(--space-2); flex-wrap: wrap; }
.model-activation {
  display: grid; gap: 6px; margin-top: var(--space-1); padding-top: var(--space-3); border-top: 1px solid var(--border-subtle);
}
.model-activation-title { margin: 0; font-weight: 650; }
.model-activation .setting-note-line { margin-top: 0; }
.model-start-button {
  justify-self: start; min-width: min(100%, 300px); height: 44px; padding: 0 20px;
  background: var(--accent-bg); color: var(--accent-text-on); border: 1px solid var(--accent-bg);
  border-radius: var(--radius-md); font-weight: 700; font-size: var(--step-0);
}
.model-start-button:hover:not(:disabled) { filter: brightness(1.12); }
.model-start-button:disabled {
  background: var(--bg-overlay); color: var(--text-tertiary); border-color: var(--border-default);
}

/* Local voice recognition uses the same compact card language without sharing LLM lifecycle. */
.models-list { display: grid; gap: 10px; margin: var(--space-2) 0; }
.model-row { display: grid; grid-template-columns: 1fr auto; gap: var(--space-1) var(--space-3); align-items: center; padding: 10px var(--space-3); border: 1px solid var(--border-default); border-radius: var(--radius-md); }
.model-row[data-chosen="true"] { border-color: var(--text-secondary); }
.model-row .model-name { font-weight: 600; }
.model-row .model-facts, .model-row .model-notes { color: var(--text-secondary); font-size: var(--step--1); grid-column: 1; }
.model-row .model-controls { grid-column: 2; grid-row: 1 / span 3; display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
.model-row .model-controls .outline-button { height: 30px; padding: 0 10px; }

.local-asr-row {
  display: grid; grid-template-columns: minmax(0, 1fr) minmax(180px, 1.35fr);
  gap: 10px 14px; align-items: center; margin: 10px 0 6px;
}
.local-asr-row label { color: var(--text-secondary); font-size: var(--step--1); }
.local-asr-row select {
  width: 100%; min-height: 36px; padding: 7px 34px 7px 10px;
  background: var(--bg-surface-raised); color: var(--text-primary);
  border: 1px solid var(--border-default); border-radius: var(--radius-md);
}
.local-asr-row select:disabled { color: var(--text-tertiary); background: var(--bg-overlay); }
@media (max-width: 720px) {
  .local-asr-row { grid-template-columns: 1fr; }
}

.receipt-line {
  grid-area: receipt; margin: 6px 2px 0; color: var(--text-tertiary); font-size: var(--step--1);
}
.receipt-link,
.prompt-receipt-link {
  padding: 0; background: none; border: 0; color: var(--text-secondary);
  text-decoration: underline; text-decoration-color: var(--border-default); text-underline-offset: 2px;
}
.receipt-link:hover, .prompt-receipt-link:hover { color: var(--text-primary); text-decoration-color: currentColor; }

.timeline-empty { margin: 18% 6px 0; color: var(--text-secondary); max-width: 42ch; }
.timeline-empty-heading { margin: 0 0 6px; color: var(--text-primary); font-family: var(--font-serif); font-size: var(--step-3); line-height: 1.3; }
.timeline-empty p { margin: 0 0 6px; }

/* ------------------------------------------------------------ composer */

.composer {
  grid-column: 1; grid-row: 2;
  padding: var(--space-2) 14px 14px; border-top: 1px solid var(--border-subtle); background: var(--bg-surface);
}
.response-progress { margin: 0 0 6px; color: var(--text-secondary); font-size: var(--step--1); }
.response-progress[data-state="Failed"] { color: var(--status-critical); }

.ticket {
  position: relative;
  display: flex; align-items: flex-end; gap: var(--space-3);
  margin: 2px 0 10px; padding: 11px 14px var(--space-3) 18px;
  background: var(--ticket-bg); color: var(--ticket-text);
  border-radius: var(--radius-md) var(--radius-md) var(--radius-md) var(--radius-md);
  box-shadow: 0 1px 0 var(--ticket-border), 0 6px 18px var(--ticket-shadow);
}
.ticket::before {
  content: ""; position: absolute; left: 6px; top: 8px; bottom: 8px;
  border-left: 2px dotted var(--ticket-border);
}
.ticket.arrive { animation: hand-over var(--duration-base) var(--ease-standard); }
.ticket[data-state="Preparing"] .ticket-body { opacity: 0.7; }
.ticket-body { flex: 1; min-width: 0; }
.ticket p { margin: 0; }
.ticket-title { font-family: var(--font-serif); font-size: var(--step-1); font-weight: 600; line-height: 1.3; overflow-wrap: anywhere; }
.ticket-line { margin-top: 2px !important; font-size: var(--step--1); }
.ticket-source { margin-top: var(--space-1) !important; font-size: var(--step--2); color: var(--ticket-text-secondary); overflow-wrap: anywhere; }
.ticket-hash { font-family: var(--font-mono); white-space: nowrap; }
.ticket-diverged { margin-top: 6px !important; font-size: var(--step--1); font-weight: 600; }
.ticket-view {
  flex: none; height: 28px; padding: 0 10px;
  background: transparent; border: 1px solid var(--ticket-border); border-radius: var(--radius-md);
  color: var(--ticket-text); font-size: var(--step--1); font-weight: 600;
}
.ticket-view:hover:not(:disabled) { background: var(--ticket-hover); }
.ticket-view:disabled { opacity: 0.5; }

.composer-controls { display: flex; align-items: center; gap: var(--space-3); min-height: 30px; margin-bottom: var(--space-2); }
.share {
  display: inline-flex; align-items: center; gap: var(--space-2); flex: none;
  height: 28px; padding: 0 10px 0 var(--space-2);
  background: transparent; border: 1px solid var(--border-default); border-radius: var(--radius-lg);
  font-size: var(--step--1); font-weight: 600; color: var(--text-secondary);
}
.share-box { width: 13px; height: 13px; border: 1.5px solid currentColor; border-radius: var(--radius-sm); }
.share[aria-pressed="true"] { color: var(--ticket-text); background: var(--ticket-bg); border-color: var(--ticket-border); }
.share[aria-pressed="true"] .share-box { background: currentColor; box-shadow: inset 0 0 0 2px var(--ticket-bg); }
.share[data-state="Unavailable"] { color: var(--status-critical); border-color: currentColor; }
.share:disabled { opacity: 0.45; }
.composer-hold { margin: 0; color: var(--text-secondary); font-size: var(--step--1); }

.compose-row { display: flex; align-items: flex-end; gap: 10px; }
.compose-row textarea {
  flex: 1; min-height: 44px; max-height: 220px; resize: none;
  padding: 11px 18px; line-height: 1.45;
  background: var(--bg-surface-raised); border: 1px solid var(--border-default); border-radius: var(--radius-lg);
}
.compose-row textarea::placeholder { color: var(--text-tertiary); }
.voice-input-shell { position: relative; flex: none; }
.voice-input {
  display: grid; place-items: center; width: 44px; height: 44px; padding: 0;
  background: var(--bg-surface); color: var(--text-secondary); border: 1px solid var(--border-default); border-radius: 50%;
}
.voice-input:hover:not(:disabled) { color: var(--text-primary); border-color: var(--text-secondary); }
.voice-input:disabled { color: var(--text-tertiary); border-color: var(--border-subtle); background: var(--bg-overlay); }
.voice-input svg {
  width: 21px; height: 21px; fill: none; stroke: currentColor; stroke-width: 1.7;
  stroke-linecap: round; stroke-linejoin: round;
}
.voice-input[data-state="listening"] { color: var(--text-primary); border-color: var(--text-primary); }
.voice-input-overlay {
  position: absolute; right: 50%; bottom: calc(100% + 10px); z-index: 25;
  width: 148px; padding: 10px; transform: translateX(50%);
  background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--radius-lg); box-shadow: var(--shadow-2);
}
.voice-input-overlay[hidden] { display: none; }
.voice-input-overlay canvas {
  display: block; width: 112px; height: 44px; margin: 0 auto 7px;
  background: var(--bg-overlay); border: 1px solid var(--border-subtle); border-radius: var(--radius-md);
}
.voice-input-overlay p { margin: 0; color: var(--text-secondary); font-size: var(--step--2); line-height: 1.35; text-align: center; }
.send, .stop {
  flex: none; height: 44px; padding: 0 20px;
  border-radius: var(--radius-md); font-weight: 700;
}
.send { background: var(--accent-bg); color: var(--accent-text-on); border: 1px solid var(--accent-bg); }
.send:disabled { background: var(--bg-overlay); color: var(--text-tertiary); border-color: var(--border-default); }
.stop { background: var(--bg-surface); color: var(--text-primary); border: 1px solid var(--border-default); }

/* ------------------------------------------------------------ settings */

.settings { flex: 1; min-height: 0; overflow-y: auto; padding: var(--space-1) var(--space-4) 18px; }
.setting { padding: 14px 0; border-bottom: 1px solid var(--border-subtle); }
.setting:last-child { border-bottom: 0; }
.setting h2 { margin: 0 0 9px; font-size: var(--step-0); font-weight: 650; }
.setting-row { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.setting-note-line { margin: var(--space-2) 0 0; color: var(--text-secondary); font-size: var(--step--1); }
.memory-state { margin: 0 0 var(--space-3); }
.memory-label { margin: 0; font-weight: 600; }
.memory-state .setting-note-line { margin-top: var(--space-1); }
.path { font-family: var(--font-mono); font-size: var(--step--2); overflow-wrap: anywhere; }

.legend { display: flex; gap: var(--space-3); margin: 0 0 9px; padding: 0; list-style: none; font-size: var(--step--1); color: var(--text-secondary); }
.legend li { display: inline-flex; align-items: center; gap: 5px; }
.legend li::before { content: ""; width: 10px; height: 10px; border-radius: var(--radius-sm); border: 1px solid var(--border-default); }
.legend-core::before { background: var(--relation-foundation); }
.legend-used::before { background: var(--relation-conversation); }
.legend-remaining::before { background: var(--bg-overlay); }

.meter {
  position: relative; display: flex; height: 26px; overflow: hidden;
  border: 1px solid var(--border-default); border-radius: var(--radius-md); background: var(--bg-overlay);
}
.meter > span:not(.meter-text) { flex: 0 0 auto; width: 0; transition: width 320ms var(--ease-standard); }
.meter-core { background: var(--relation-foundation); }
.meter-used { background: var(--relation-conversation); }
.meter-remaining { background: var(--bg-overlay); flex: 1 1 auto !important; }
.meter[data-known="false"] .meter-remaining { background: transparent; }
.meter-text {
  position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);
  padding: 0 var(--space-2); border-radius: var(--radius-sm); background: var(--bg-surface);
  font-family: var(--font-mono); font-size: var(--step--1); font-weight: 600; color: var(--text-primary);
  font-variant-numeric: tabular-nums; white-space: nowrap;
}
.meter[data-known="false"] .meter-text { color: var(--text-secondary); }

.toggle-row { display: flex; align-items: center; gap: 10px; margin-top: var(--space-3); }
.toggle-label {
  flex: 1; display: flex; align-items: center; height: 34px; padding: 0 var(--space-3);
  border: 1px solid var(--border-default); border-radius: var(--radius-md); font-weight: 600; font-size: var(--step--1);
}
.toggle-detail {
  display: flex; align-items: center; height: 34px; padding: 0 var(--space-3);
  border: 1px solid var(--border-default); border-radius: var(--radius-md); font-size: var(--step--1); font-variant-numeric: tabular-nums;
}
/* The one fluid control (Vextreme v2 Switch): a pill track, inverse when on. */
.switch {
  position: relative; flex: none; width: 40px; height: 22px; padding: 0;
  background: var(--border-strong); border: 0; border-radius: var(--radius-full);
  transition: background var(--duration-base) var(--ease-standard);
}
.switch span {
  position: absolute; top: 3px; left: 3px; width: 16px; height: 16px; border-radius: var(--radius-full);
  background: var(--text-primary);
  transition: transform var(--duration-base) var(--ease-standard), background var(--duration-base) linear;
}
.switch[aria-checked="true"] { background: var(--accent-bg); }
.switch[aria-checked="true"] span { transform: translateX(18px); background: var(--accent-text-on); }

.home-row { display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: center; margin-top: 10px; }
.home-row[data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}"] { grid-template-columns: 1fr auto; row-gap: 10px; }
.home-label {
  display: flex; align-items: center; height: 34px; padding: 0 var(--space-3);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-md); color: var(--text-secondary); font-weight: 600; font-size: var(--step--1);
}
.outline-button.wide { height: 34px; text-align: left; }
a.outline-button { display: inline-flex; align-items: center; justify-content: center; text-decoration: none; color: inherit; height: 34px; }
[data-vex-ref="${VexLifeWebP1.SettingsDiagnostics.identity.ref}"] .home-row { grid-template-columns: 1fr auto auto; }
[data-vex-ref="${VexLifeWebP1.SettingsDiagnostics.identity.ref}"] .home-row .outline-button { height: 34px; padding: 0 var(--space-3); }
.outline-button.state { height: 34px; }

.setting-reset { padding-top: 18px; }
.danger-button {
  width: 100%; height: 42px;
  background: var(--critical-bg); color: var(--critical-text); border: 1px solid var(--critical-bg); border-radius: var(--radius-md);
  font-weight: 700;
}
.danger-button:hover:not(:disabled) { filter: brightness(1.06); }
.danger-button:disabled { opacity: 0.5; }

/* ------------------------------------------------------------ sheets + overlay */

.sheet {
  width: min(720px, calc(100vw - 32px)); max-height: min(80vh, 760px);
  padding: 0; border: 1px solid var(--border-default); border-radius: var(--radius-lg);
  background: var(--bg-surface); color: var(--text-primary); box-shadow: var(--shadow-2);
}
.sheet::backdrop { background: var(--scrim); }
.sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 14px var(--space-4) 10px; border-bottom: 1px solid var(--border-subtle); }
.sheet h2 { margin: 0; font-size: var(--step-1); }
.facts { display: grid; grid-template-columns: max-content 1fr; gap: var(--space-1) 14px; margin: var(--space-3) var(--space-4); font-size: var(--step--1); }
.facts dt { color: var(--text-tertiary); }
.facts dd { margin: 0; overflow-wrap: anywhere; }
.inspector-text {
  margin: 0 var(--space-4) var(--space-4); padding: var(--space-3) 14px; max-height: 52vh; overflow: auto;
  background: var(--ticket-bg); color: var(--ticket-text); border-radius: var(--radius-md);
  font-family: var(--font-serif); font-size: var(--step-0); line-height: 1.55; white-space: pre-wrap;
}
.confirm { width: min(440px, calc(100vw - 32px)); padding: 18px 18px var(--space-4); }
.confirm p { margin: var(--space-2) 0 var(--space-4); color: var(--text-secondary); }
.confirm-actions { display: flex; justify-content: flex-end; gap: 10px; }
.confirm .danger-button { width: auto; padding: 0 var(--space-4); height: 36px; }

.overlay { position: fixed; inset: auto 0 22px 0; display: flex; justify-content: center; pointer-events: none; z-index: 20; }
.snackbar {
  margin: 0; padding: 10px var(--space-4); max-width: min(520px, calc(100vw - 32px));
  background: var(--bg-inverse); color: var(--text-inverse); border-radius: var(--radius-md); box-shadow: var(--shadow-2);
  font-size: var(--step--1);
}
.binding-failure {
  position: fixed; inset: 16px 16px auto; margin: 0; padding: 14px var(--space-4);
  background: var(--critical-bg); color: var(--critical-text); border-radius: var(--radius-md); font-weight: 600; z-index: 30;
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
    padding: 10px var(--space-3); row-gap: 10px;
  }
  .brand-name { display: none; }
  .utilities { grid-column: 1 / -1; justify-self: stretch; padding: 0; background: none; border: 0; overflow-x: auto; }

  .model-choice-row { grid-template-columns: minmax(0, 1fr); }
  .model-choice-actions { justify-content: flex-start; }
  .model-cache-field { grid-template-columns: minmax(0, 1fr); }
  .model-start-button { width: 100%; min-width: 0; }

  /* The authored Browser↔Companion relationship remains one Seam. Narrow presentation
     reorients only its renderer manifestation: the same allocation/collapse state becomes
     top/bottom instead of left/right. */
  .frame, .frame[data-collapsed] {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: minmax(0, var(--before)) 12px minmax(0, 1fr);
  }
  .frame[data-collapsed="before"] { grid-template-rows: 0 12px minmax(0, 1fr); }
  .frame[data-collapsed="after"] { grid-template-rows: minmax(0, 1fr) 12px 0; }

  .frame .region-browser, .frame .region-companion { grid-column: 1; }
  .frame .region-browser { grid-row: 1; }
  .frame .region-companion { grid-row: 3; }

  .seam {
    display: flex; flex-direction: row; grid-column: 1; grid-row: 2;
    border-inline: 0; border-block: 1px solid var(--border-subtle);
    cursor: row-resize;
  }
  .frame.resizing { cursor: row-resize; }
  /* Narrow/touch layouts cannot rely on hover to reveal the existing collapse affordances. */
  .seam-collapse { width: 26px; height: 18px; opacity: 1; }
  .seam-collapse-before svg,
  .seam-collapse-after svg { transform: rotate(90deg); }
  .seam-grip svg { transform: rotate(90deg) scale(0.8); }

  .region-browser { border-bottom: 0; }
}

.message.waiting .speaker { display: none; }
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
