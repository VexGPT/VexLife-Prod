package vexlife.web

import vexlife.workspace.VexLifeSourceTemplate
import vexlife.workspace.VexLifeWorkspaceApp

/**
 * Canonical VexLife-Prod product presentation template.
 *
 * The DOM follows the A003 nested-browser experience directly. Stable identities come only from
 * the current Blueprint/VexWeb source; this file does not own Conversation, Memory, Context,
 * Heretic/runtime, browser admission, or effect authority.
 *
 * Presentation fidelity is intentional: a product-specific renderer is allowed to preserve the
 * product's lived composition rather than forcing that composition through generic HTML shapes.
 */
internal object VexLifeWebP1ExperienceMarkup {
    /**
     * The canonical Vextreme family mark, carried at the VexLife Product Brand coordinate. Geometry
     * is copied (not fetched) from `vgong24/Vextreme@460df846b181e4673de512ba54f2c412ad8de362`
     * `assets/logo/vextreme-mark.svg` (120x120 viewBox, 5-unit round stroke in currentColor). The
     * product name stays VexLife; the family header/rail is a later, separately earned relationship.
     */
    const val FamilyMarkPath =
        "M 51.89 109.32 A 50 50 0 1 1 68.11 109.32 L 39.73 35.68 L 60 65.41 L 80.27 35.68 Z"

    fun render(): String =
        """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark">
<title>VexLife</title>
<link rel="stylesheet" href="vexweb.css">
<script type="module" src="vexweb.js"></script>
</head>
<body>
<div class="workspace" id="workspace" data-vex-screen="${VexLifeWebP1.Surface.screenRef}">

  <header class="chrome" data-vex-ref="${VexLifeWebP1.Header.identity.ref}">
    <a class="brand" data-vex-ref="${VexLifeWebP1.Brand.identity.ref}" href="/" aria-label="VexLife">
      <span class="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 120 120"><path d="$FamilyMarkPath"/></svg>
      </span>
      <span class="brand-name">VexLife</span>
    </a>

    <form class="address" id="address-form" autocomplete="off">
      <label class="visually-hidden" for="query">Page address</label>
      <input id="query" data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}" type="text" list="page-suggestions" spellcheck="false"
        inputmode="url" placeholder="Type a web address" title="">
      <datalist id="page-suggestions">
        <option value="vgong24.github.io/Vextreme/pages/terrain-map.html">Vextreme terrain</option>
        <option value="vexgpt.github.io/VexWork/orientation.html">VexWork orientation</option>
        <option value="vexgpt.github.io/VexWork/">VexWork navigator</option>
        <option value="vgong24.github.io/VexLife/">VexLife</option>
      </datalist>
      <button class="icon-button" type="button" id="reload" aria-label="Reload page" title="Reload page">
        <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M15.5 8.2A5.8 5.8 0 1 0 15.8 12"/><path d="M15.9 3.8v4.6h-4.6"/></svg>
      </button>
      <p class="address-error" id="address-error" role="status" hidden></p>
    </form>

    <label class="app-selector">
      <span class="visually-hidden">App</span>
      <select id="app-selector" data-vex-ref="${VexLifeWebP1.AppSelector.identity.ref}">
${VexLifeWorkspaceApp.entries.joinToString("\n") { "        <option value=\"${it.key}\">${it.label}</option>" }}
      </select>
    </label>

    <div class="utilities" data-vex-ref="${VexLifeWebP1.Utilities.identity.ref}">
      <ul class="quick-actions" data-vex-ref="${VexLifeWebP1.QuickActions.identity.ref}" id="quick-actions" aria-label="Quick actions"><li hidden data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"><button type="button" id="vex-summon" hidden></button></li><li hidden data-vex-ref="${VexLifeWebP1.VisionQuickAccess.identity.ref}"><button type="button" id="vision-quick-access-source" hidden></button></li><li hidden data-vex-ref="${VexLifeWebP1.ReadAloud.identity.ref}"><button type="button" id="read-aloud-source" hidden></button></li></ul>
      <section class="read-aloud" id="read-aloud" aria-label="Read aloud" hidden>
        <p class="read-aloud-status" id="read-aloud-status" role="status">Reads Vex's latest reply, or choose where to start.</p>
        <div class="read-aloud-controls">
          <button class="icon-button" type="button" id="read-aloud-previous" aria-label="Previous message" title="Previous message"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="M14 4.5 7 10l7 5.5z"/><path d="M5.5 4.5v11"/></svg></button>
          <button class="icon-button read-aloud-play" type="button" id="read-aloud-play" aria-label="Play" title="Play"><svg viewBox="0 0 20 20" aria-hidden="true"><path class="glyph-play" d="M6.5 4.5 15 10l-8.5 5.5z"/><path class="glyph-pause" d="M7 4.5v11M13 4.5v11"/></svg></button>
          <button class="icon-button" type="button" id="read-aloud-stop" aria-label="Stop" title="Stop"><svg viewBox="0 0 20 20" aria-hidden="true"><rect x="5.5" y="5.5" width="9" height="9" rx="1"/></svg></button>
          <button class="icon-button" type="button" id="read-aloud-next" aria-label="Next message" title="Next message"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="M6 4.5 13 10l-7 5.5z"/><path d="M14.5 4.5v11"/></svg></button>
          <button class="quiet-button" type="button" id="read-aloud-choose" aria-pressed="false">Start here…</button>
        </div>
        <label class="read-aloud-field"><span>Speed</span><input type="range" id="read-aloud-rate" min="0.7" max="1.6" step="0.1" value="1"><output id="read-aloud-rate-value">1.0×</output></label>
        <label class="read-aloud-field"><span>Voice</span><select id="read-aloud-voice"></select></label>
      </section>
    </div>
  </header>

  <main class="frame" id="frame" data-vex-structural-ref="${VexLifeWebP1.Refs.BodyFrame.value}">
    <section class="region region-browser" data-vex-ref="${VexLifeWebP1.Browser.identity.ref}" aria-label="Browser">
      <p class="frame-status" data-vex-ref="${VexLifeWebP1.BrowserFrameStatus.identity.ref}" id="frame-status" role="status" hidden></p>
      <nav class="family" data-vex-ref="${VexLifeWebP1.SourceTemplateFamily.identity.ref}" id="source-template-family" aria-label="Source template" hidden>
${VexLifeSourceTemplate.Members.joinToString("\n") { "        <button type=\"button\" class=\"family-member\" data-family-key=\"${it.key}\" data-family-home=\"${it.home}\" data-family-scope=\"${it.scope}\">${it.label}</button>" }}
        <span class="family-note">Victor's live work, read-only</span>
      </nav>
      <div class="viewport" data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}">
        <iframe id="page-frame" title="Opened page" referrerpolicy="no-referrer"
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox allow-downloads allow-modals"></iframe>
        <iframe id="context-frame" class="context-frame" title="Vex page context provider" referrerpolicy="no-referrer"
          sandbox="allow-scripts allow-same-origin" aria-hidden="true" tabindex="-1"></iframe>
        <div class="viewport-empty" id="viewport-empty"><p>Open a web page from the address field. Vex page context is separate and may not be available for every page.</p></div>
      </div>
      <section class="archives" data-vex-ref="${VexLifeWebP1.Archives.identity.ref}" id="archives" aria-labelledby="archives-heading" hidden>
        <header class="archives-head">
          <h2 id="archives-heading">Your archives</h2>
          <p class="archives-summary" id="archives-summary">Checking your conversation…</p>
        </header>
        <ol class="archives-timeline" data-vex-ref="${VexLifeWebP1.ArchivesTimeline.identity.ref}" id="archives-timeline" aria-label="Conversation by day"></ol>
      </section>
    </section>

    <div class="seam" id="seam" data-vex-structural-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}" role="separator" aria-orientation="vertical" tabindex="0"
      aria-label="Resize browser and companion" aria-valuemin="15" aria-valuemax="85">
      <button class="seam-collapse seam-collapse-before" type="button" id="collapse-before" aria-label="Hide browser" title="Hide browser">
        <svg viewBox="0 0 12 12" aria-hidden="true"><path d="M7.5 2 3.5 6l4 4"/></svg>
      </button>
      <span class="seam-grip" aria-hidden="true">
        <svg viewBox="0 0 24 12"><path d="M2 6h20M6 2 2 6l4 4M18 2l4 4-4 4"/></svg>
      </span>
      <button class="seam-collapse seam-collapse-after" type="button" id="collapse-after" aria-label="Hide companion" title="Hide companion">
        <svg viewBox="0 0 12 12" aria-hidden="true"><path d="m4.5 2 4 4-4 4"/></svg>
      </button>
    </div>

    <section class="region region-companion" data-vex-ref="${VexLifeWebP1.Companion.identity.ref}" aria-labelledby="companion-title">
      <div class="companion-head" data-vex-structural-ref="${VexLifeWebP1.Refs.CompanionHeader.value}">
        <h1 class="companion-title" id="companion-title">Converse with Vex</h1>
        <p class="companion-status" data-vex-ref="${VexLifeWebP1.CompanionStatus.identity.ref}" id="companion-status" role="status" aria-live="polite">
          <span class="status-dot" aria-hidden="true"></span>
          <span id="companion-status-text">Vex not checked</span>
          <span class="status-model" id="companion-status-model"></span>
        </p>
        <button class="quiet-button" type="button" data-vex-legacy="CompanionCheck" id="companion-check" hidden>Check again</button>
        <button class="outline-button vision-control" type="button" data-vex-ref="${VexLifeWebP1.Vision.identity.ref}" id="vision-control"
          aria-describedby="vision-control-status" aria-disabled="true" disabled title="Vision availability is checking">
          <svg class="vision-mark" viewBox="0 0 20 20" aria-hidden="true"><path d="M2.4 10s2.8-4.3 7.6-4.3 7.6 4.3 7.6 4.3-2.8 4.3-7.6 4.3S2.4 10 2.4 10Z"/><circle cx="10" cy="10" r="2.3"/></svg>
          <span>Vision</span>
          <span class="vision-state">Held</span>
        </button>
        <span class="visually-hidden" id="vision-control-status">Vision route availability is checking.</span>
        <button class="outline-button view-switch" type="button" data-vex-ref="${VexLifeWebP1.CompanionViewSwitch.identity.ref}" id="view-switch" aria-controls="view-settings" aria-expanded="false">Settings</button>
      </div>

      <div class="conversation" data-vex-ref="${VexLifeWebP1.Conversation.identity.ref}" data-vex-structural-parent="${VexLifeWebP1.Refs.CompanionContent.value}" id="view-conversation">
        <ol class="timeline" data-vex-ref="${VexLifeWebP1.ConversationTimeline.identity.ref}" id="timeline" aria-label="Conversation with Vex"></ol>
        <button class="outline-button jump-latest" type="button" data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}" id="jump-latest" hidden>New messages</button>

        <div class="composer" data-vex-ref="${VexLifeWebP1.Composer.identity.ref}">
          <p class="response-progress" data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}" id="response-progress" role="status" aria-live="polite" hidden></p>

          <div class="ticket" data-vex-ref="${VexLifeWebP1.ContextReceipt.identity.ref}" id="ticket" hidden>
            <div class="ticket-body">
              <p class="ticket-title" id="ticket-title"></p>
              <p class="ticket-line" id="ticket-line"></p>
              <p class="ticket-source"><span id="ticket-url"></span> <span class="ticket-hash" id="ticket-hash"></span></p>
              <p class="ticket-diverged" id="ticket-diverged" hidden>You've moved inside the page since you included it. Vex still gets the page named here.</p>
            </div>
            <button class="ticket-view" type="button" id="ticket-view">View text</button>
          </div>

          <div class="composer-controls">
            <button class="share" type="button" data-vex-ref="${VexLifeWebP1.ContextShare.identity.ref}" id="context-share" aria-pressed="false">
              <span class="share-box" aria-hidden="true"></span>
              <span id="context-share-label">Include this page</span>
            </button>
            <p class="composer-hold" id="composer-hold" role="status"></p>
          </div>

          <form class="compose-row" id="compose-form">
            <label class="visually-hidden" for="draft">Message Vex</label>
            <textarea id="draft" rows="1" placeholder="Message Vex" enterkeyhint="send"></textarea>
            <button class="send" type="submit" data-vex-ref="${VexLifeWebP1.Send.identity.ref}" id="send">Send</button>
            <button class="stop" type="button" data-vex-ref="${VexLifeWebP1.Stop.identity.ref}" id="stop" hidden>Stop</button>
          </form>
        </div>
      </div>

      <div class="settings" data-vex-ref="${VexLifeWebP1.Settings.identity.ref}" data-vex-structural-parent="${VexLifeWebP1.Refs.CompanionContent.value}" id="view-settings" hidden>
        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}" aria-labelledby="context-heading">
          <div class="setting-row">
            <h2 id="context-heading">Context window</h2>
            <ul class="legend" aria-hidden="true">
              <li class="legend-core">Input</li><li class="legend-used">Reserved</li><li class="legend-remaining">Remaining</li>
            </ul>
          </div>
          <div class="meter" data-vex-ref="${VexLifeWebP1.ContextWindow.identity.ref}" id="context-window" role="img" aria-label="Context window not measured yet">
            <span class="meter-core" id="context-core"></span>
            <span class="meter-used" id="context-used"></span>
            <span class="meter-remaining" id="context-remaining"></span>
            <span class="meter-text" id="context-text">Not exposed yet</span>
          </div>
          <p class="setting-note-line" id="context-note"></p>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsResources.identity.ref}" aria-labelledby="memory-heading">
          <h2 id="memory-heading">Current resources</h2>
          <div class="meter" data-vex-ref="${VexLifeWebP1.MemoryResource.identity.ref}" id="memory" role="img" aria-label="System memory not measured yet">
            <span class="meter-used" id="memory-used"></span>
            <span class="meter-remaining" id="memory-remaining"></span>
            <span class="meter-text" id="memory-text">Measuring</span>
          </div>
          <p class="setting-note-line" id="resource-note">Current machine and model resource truth has not been observed yet.</p>

          <div class="toggle-row">
            <span class="toggle-label" id="thoughts-label">Viewable thoughts</span>
            <button class="switch" type="button" role="switch" data-vex-ref="${VexLifeWebP1.ThoughtVisibility.identity.ref}" id="thought-visibility" aria-checked="true" aria-labelledby="thoughts-label"><span></span></button>
          </div>
          <div class="toggle-row">
            <span class="toggle-label" id="alert-label">Low memory alert</span>
            <span class="toggle-detail" id="alert-threshold">Not connected</span>
            <button class="switch" type="button" role="switch" data-vex-ref="${VexLifeWebP1.LowMemoryAlert.identity.ref}" id="low-memory-alert" aria-checked="true" aria-labelledby="alert-label alert-threshold"><span></span></button>
          </div>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsHome.identity.ref}" aria-labelledby="home-heading">
          <h2 id="home-heading">VexLife Home</h2>
          <p class="setting-note-line" id="home-current-state">Current Home has not been observed yet.</p>
          <div class="home-row">
            <button class="outline-button wide" type="button" data-vex-ref="${VexLifeWebP1.HomeDirectories.identity.ref}" id="home-directories" disabled>View accessible directories</button>
            <button class="outline-button" type="button" data-vex-ref="${VexLifeWebP1.HomeAddDirectory.identity.ref}" id="home-add" disabled>Add</button>
          </div>
          <p class="setting-note-line">This presentation does not open or mutate your Vex Home. Canonical Conversation stays upstream in VexLife; browser-context receipts are kept separately in <span class="path" id="setting-data-dir"></span>.</p>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}" aria-labelledby="continuity-heading">
          <h2 id="continuity-heading">Continuity</h2>
          <div class="memory-state" data-vex-ref="${VexLifeWebP1.ContinuityMemory.identity.ref}" id="vex-memory">
            <p class="memory-label" id="vex-memory-label">Memory · Checking</p>
            <p class="setting-note-line" id="vex-memory-detail">VexLife hasn't read Vex's Memory yet.</p>
            <p class="setting-note-line" id="memory-note"></p>
          </div>
          <div class="home-row">
            <span class="home-label">Continuity mode</span>
            <button class="outline-button state" type="button" data-vex-ref="${VexLifeWebP1.ContinuityMode.identity.ref}" id="continuity-mode" disabled>Not available yet</button>
            <span class="home-label">Dream sync</span>
            <button class="outline-button state" type="button" data-vex-ref="${VexLifeWebP1.DreamSync.identity.ref}" id="dream-sync" disabled>Not available yet</button>
          </div>
          <p class="setting-note-line">Continuity mode will choose how another device reaches Vex: as a window onto this Mac's Vex, or as a Vex of its own. Dream sync will let Vex propose what to keep from your conversations so you can accept or decline each one before anything is kept or shared. Neither exists in VexLife-Prod yet.</p>
        </section>

        <section class="setting setting-reset" data-vex-ref="${VexLifeWebP1.SettingsReset.identity.ref}">
          <button class="outline-button wide" type="button" data-vex-ref="${VexLifeWebP1.CleanChat.identity.ref}" id="clean-chat" disabled>Start clean chat · Not available yet</button>
          <p class="setting-note-line">A clean chat will let your next message start fresh: Vex won't be given your earlier messages. Nothing is deleted; they stay in your conversation, and Vex's Memory is untouched.</p>
        </section>

        <section class="setting setting-renderer">
          <p class="setting-note-line" id="setting-renderer"></p>
        </section>
      </div>
    </section>
  </main>

  <dialog class="sheet" data-vex-ref="${VexLifeWebP1.ContextInspector.identity.ref}" id="inspector" aria-labelledby="inspector-title">
    <div class="sheet-head">
      <h2 id="inspector-title">What Vex receives</h2>
      <button class="quiet-button" type="button" id="inspector-close">Close</button>
    </div>
    <dl class="facts" id="inspector-facts"></dl>
    <pre class="inspector-text" id="inspector-text"></pre>
  </dialog>

  <dialog class="sheet confirm" id="confirm-clean" aria-labelledby="confirm-title">
    <h2 id="confirm-title">Start a clean chat?</h2>
    <p id="confirm-text"></p>
    <div class="confirm-actions">
      <button class="quiet-button" type="button" id="confirm-cancel">Keep this chat</button>
      <button class="danger-button" type="button" id="confirm-accept">Start clean chat</button>
    </div>
  </dialog>

  <div class="overlay" id="overlay-root">
    <p class="snackbar" id="snackbar" role="status" aria-live="polite" hidden></p>
  </div>

  <p class="binding-failure" id="binding-failure" role="alert" hidden></p>
</div>
</body>
</html>
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
