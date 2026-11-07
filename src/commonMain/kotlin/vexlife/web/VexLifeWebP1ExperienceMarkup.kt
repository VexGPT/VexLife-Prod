package vexlife.web

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
        <svg viewBox="0 0 32 32"><circle cx="16" cy="16" r="10.5"/><path d="M11.2 11.5 16 21.5l4.8-10"/></svg>
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
        <option value="browser" selected>Browser</option>
        <option value="" disabled>More apps aren't connected yet</option>
      </select>
    </label>

    <div class="utilities" data-vex-ref="${VexLifeWebP1.Utilities.identity.ref}">
      <ul class="quick-actions" data-vex-ref="${VexLifeWebP1.QuickActions.identity.ref}" id="quick-actions" aria-label="Quick actions"><li hidden data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"><button type="button" id="vex-summon" hidden></button></li></ul>
    </div>
  </header>

  <main class="frame" id="frame" data-vex-structural-ref="${VexLifeWebP1.Refs.BodyFrame.value}">
    <section class="region region-browser" data-vex-ref="${VexLifeWebP1.Browser.identity.ref}" aria-label="Browser">
      <p class="frame-status" data-vex-ref="${VexLifeWebP1.BrowserFrameStatus.identity.ref}" id="frame-status" role="status" hidden></p>
      <div class="viewport" data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}">
        <iframe id="page-frame" title="Opened page" referrerpolicy="no-referrer"
          sandbox="allow-scripts allow-same-origin"></iframe>
        <div class="viewport-empty" id="viewport-empty"><p>Open any public web page from the address field. Pages open signed out.</p></div>
      </div>
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
          aria-describedby="vision-control-status" aria-disabled="true" disabled title="Vision capture is not connected yet">
          <svg class="vision-mark" viewBox="0 0 20 20" aria-hidden="true"><path d="M2.4 10s2.8-4.3 7.6-4.3 7.6 4.3 7.6 4.3-2.8 4.3-7.6 4.3S2.4 10 2.4 10Z"/><circle cx="10" cy="10" r="2.3"/></svg>
          <span>Vision</span>
          <span class="vision-state">Held</span>
        </button>
        <span class="visually-hidden" id="vision-control-status">Vision is present in VexLife, but screen permission and capture are not connected yet.</span>
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
          <h2 id="memory-heading">Mac memory</h2>
          <div class="meter" data-vex-ref="${VexLifeWebP1.MemoryResource.identity.ref}" id="memory" role="img" aria-label="Memory not measured yet">
            <span class="meter-used" id="memory-used"></span>
            <span class="meter-remaining" id="memory-remaining"></span>
            <span class="meter-text" id="memory-text">Measuring</span>
          </div>
          <p class="setting-note-line" id="memory-note"></p>

          <div class="toggle-row">
            <span class="toggle-label" id="thoughts-label">Viewable thoughts</span>
            <button class="switch" type="button" role="switch" data-vex-ref="${VexLifeWebP1.ThoughtVisibility.identity.ref}" id="thought-visibility" aria-checked="true" aria-labelledby="thoughts-label"><span></span></button>
          </div>
          <div class="toggle-row">
            <span class="toggle-label" id="alert-label">Low memory alert</span>
            <span class="toggle-detail" id="alert-threshold">25% left</span>
            <button class="switch" type="button" role="switch" data-vex-ref="${VexLifeWebP1.LowMemoryAlert.identity.ref}" id="low-memory-alert" aria-checked="true" aria-labelledby="alert-label alert-threshold"><span></span></button>
          </div>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsHome.identity.ref}" aria-labelledby="home-heading">
          <h2 id="home-heading">VexLife Home</h2>
          <div class="home-row">
            <button class="outline-button wide" type="button" data-vex-ref="${VexLifeWebP1.HomeDirectories.identity.ref}" id="home-directories" disabled>View accessible directories</button>
            <button class="outline-button" type="button" data-vex-ref="${VexLifeWebP1.HomeAddDirectory.identity.ref}" id="home-add" disabled>Add</button>
          </div>
          <div class="home-row" data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}">
            <span class="home-label">Continuity mode</span>
            <button class="outline-button state" type="button" data-vex-ref="${VexLifeWebP1.ContinuityMode.identity.ref}" id="continuity-mode" disabled>Not connected</button>
            <span class="home-label">Dream sync</span>
            <button class="outline-button state" type="button" data-vex-ref="${VexLifeWebP1.DreamSync.identity.ref}" id="dream-sync" disabled>Not connected</button>
          </div>
          <p class="setting-note-line">This presentation does not open or mutate your Vex Home. Canonical Conversation stays upstream in VexLife; browser-context receipts are kept separately in <span class="path" id="setting-data-dir"></span>.</p>
        </section>

        <section class="setting setting-reset" data-vex-ref="${VexLifeWebP1.SettingsReset.identity.ref}">
          <button class="danger-button" type="button" data-vex-ref="${VexLifeWebP1.CleanChat.identity.ref}" id="clean-chat">Start clean chat</button>
          <p class="setting-note-line">Not connected yet. This presentation does not own canonical Conversation reset/archive semantics, and it does not clear VexLife Memory.</p>
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
