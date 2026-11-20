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
<div class="workspace" id="workspace" data-vex-screen="${VexLifeWebP1.Surface.screenRef}" data-vex-structural-ref="${VexLifeWebP1.Refs.WorkspaceFrame.value}">

  <header class="chrome" data-vex-ref="${VexLifeWebP1.Header.identity.ref}" data-vex-structural-ref="${VexLifeWebP1.Refs.HeaderRegion.value}">
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
      <ul class="quick-actions" data-vex-ref="${VexLifeWebP1.QuickActions.identity.ref}" id="quick-actions" aria-label="Quick actions" data-open="false"><li hidden data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"><button type="button" id="vex-summon" hidden></button></li><li class="quick-action-item" data-vex-ref="${VexLifeWebP1.Speech.identity.ref}"><button type="button" id="speech-source" class="quick-action quick-action-glyph" aria-label="Speech" title="Speech">🔊</button></li><li class="quick-action-item" data-vex-ref="${VexLifeWebP1.Language.identity.ref}"><button type="button" id="language-source" class="quick-action quick-action-glyph" aria-label="Language" title="Language">🌐</button></li><li class="quick-action-item" data-vex-ref="${VexLifeWebP1.VisionQuickAccess.identity.ref}"><button type="button" id="vision-quick-access-source" class="quick-action quick-action-glyph" aria-label="Vision" title="Vision">◉</button></li><li class="quick-action-item" data-vex-ref="${VexLifeWebP1.Theme.identity.ref}"><button type="button" id="theme-quick-action" class="quick-action quick-action-glyph" aria-label="Theme" title="Toggle dark / light theme">☀️</button></li><li class="quick-action-item" data-vex-ref="${VexLifeWebP1.ReleaseAlerts.identity.ref}"><button type="button" id="release-alerts-source" class="quick-action quick-action-glyph" aria-label="Release alerts" title="Release alerts">🔔</button></li><li class="quick-action-item" data-vex-ref="${VexLifeWebP1.Support.identity.ref}"><a id="support-quick-action" class="quick-action quick-action-glyph" aria-label="Support Vextreme and VexLife" title="Support the creator">🫴</a></li><li class="quick-action-trigger-item" data-vex-ref="${VexLifeWebP1.QuickActionsTrigger.identity.ref}"><button type="button" class="quick-action quick-action-trigger quick-action-glyph" id="quick-actions-trigger" aria-label="Open quick actions" aria-expanded="false">🌀</button></li></ul>
      <section class="release-alerts-panel" data-vex-ref="${VexLifeWebP1.ReleaseAlertsPanel.identity.ref}" id="release-alerts-panel" aria-labelledby="release-alerts-title" data-open="false" hidden>
        <p class="release-alerts-title" id="release-alerts-title"></p>
        <p class="release-alerts-empty" id="release-alerts-empty"></p>
        <ol class="release-alerts-list" data-vex-ref="${VexLifeWebP1.ReleaseAlertsList.identity.ref}" id="release-alerts-list"></ol>
      </section>
      <section class="language-panel" data-vex-ref="${VexLifeWebP1.LanguagePanel.identity.ref}" id="language-panel" aria-labelledby="reply-language-label" data-open="false" hidden>
        <label class="language-field" data-vex-ref="${VexLifeWebP1.ReplyLanguageChoice.identity.ref}">
          <span class="language-title" id="reply-language-label" data-vex-text="workspace.label.reply-language">workspace.label.reply-language</span>
          <select id="reply-language"></select>
        </label>
        <p class="language-note" data-vex-text="workspace.description.reply-language">workspace.description.reply-language</p>
        <label class="language-field" data-vex-ref="${VexLifeWebP1.ScreenLanguageChoice.identity.ref}">
          <span class="language-title" data-vex-text="workspace.label.screen-language">workspace.label.screen-language</span>
          <select id="screen-language"></select>
        </label>
        <p class="language-note" data-vex-text="workspace.description.screen-language">workspace.description.screen-language</p>
      </section>
      <section class="speech" data-vex-ref="${VexLifeWebP1.SpeechPanel.identity.ref}" id="speech" aria-label="Speech" data-open="false" hidden>
        <div class="speech-head">
          <span class="speech-title">Speak Vex aloud</span>
          <button class="switch" type="button" role="switch" data-vex-ref="${VexLifeWebP1.SpeechEnabled.identity.ref}" id="speech-switch" aria-checked="false" aria-label="Speak Vex aloud"><span></span></button>
        </div>
        <p class="speech-status" id="speech-status" role="status">Speech is off. You can still choose where to start reading.</p>
        <label class="speech-field"><span>Voice</span><select data-vex-ref="${VexLifeWebP1.SpeechVoice.identity.ref}" id="speech-voice"></select></label>
        <label class="speech-check"><input type="checkbox" data-vex-ref="${VexLifeWebP1.SpeechReadThoughts.identity.ref}" id="speech-thoughts"> Read out thoughts</label>
        <label class="speech-field"><span>Speed</span><input type="range" data-vex-ref="${VexLifeWebP1.SpeechRate.identity.ref}" id="speech-rate" min="0.7" max="1.6" step="0.1" value="1"><output id="speech-rate-value">1.0×</output></label>
        <div class="speech-controls">
          <button class="icon-button" type="button" data-vex-ref="${VexLifeWebP1.SpeechPrevious.identity.ref}" id="speech-previous" aria-label="Previous message" title="Previous message"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="M14 4.5 7 10l7 5.5z"/><path class="glyph-line" d="M5.5 4.5v11"/></svg></button>
          <button class="icon-button speech-play" type="button" data-vex-ref="${VexLifeWebP1.SpeechPlayPause.identity.ref}" id="speech-play" aria-label="Play" title="Play"><svg viewBox="0 0 20 20" aria-hidden="true"><path class="glyph-play" d="M6.5 4.5 15 10l-8.5 5.5z"/><path class="glyph-pause" d="M7 4.5v11M13 4.5v11"/></svg></button>
          <button class="icon-button" type="button" data-vex-ref="${VexLifeWebP1.SpeechStop.identity.ref}" id="speech-stop" aria-label="Stop" title="Stop"><svg viewBox="0 0 20 20" aria-hidden="true"><rect x="5.5" y="5.5" width="9" height="9" rx="1"/></svg></button>
          <button class="icon-button" type="button" data-vex-ref="${VexLifeWebP1.SpeechNext.identity.ref}" id="speech-next" aria-label="Next message" title="Next message"><svg viewBox="0 0 20 20" aria-hidden="true"><path d="M6 4.5 13 10l-7 5.5z"/><path class="glyph-line" d="M14.5 4.5v11"/></svg></button>
          <button class="quiet-button" type="button" data-vex-ref="${VexLifeWebP1.SpeechChooseStart.identity.ref}" id="speech-choose" aria-pressed="false">Start here…</button>
        </div>
      </section>
    </div>
  </header>

  <main class="frame" id="frame" data-vex-structural-ref="${VexLifeWebP1.Refs.BodyFrame.value}" data-vex-structural-parent="${VexLifeWebP1.Refs.BodyRegion.value}">
    <section class="region region-browser" data-vex-ref="${VexLifeWebP1.Browser.identity.ref}" data-vex-structural-ref="${VexLifeWebP1.Refs.BrowserRegion.value}" aria-label="Browser">
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
      <section class="vex-context" data-vex-ref="${VexLifeWebP1.VexContext.identity.ref}" id="vex-context" aria-labelledby="vex-context-heading" hidden>
        <header class="vex-context-head">
          <div>
            <h2 id="vex-context-heading">Vex context</h2>
            <p>See what shaped Vex's last reply, and choose how the next turn is put together.</p>
          </div>
          <span class="context-profile-badge" id="context-profile-badge">VexLife default</span>
        </header>

        <section class="context-profile-card" data-vex-ref="${VexLifeWebP1.VexContextProfile.identity.ref}" aria-labelledby="context-profile-heading">
          <div class="context-profile-title">
            <div>
              <h3 id="context-profile-heading">Context profile</h3>
              <p id="context-profile-summary">Checking your saved context relationship…</p>
            </div>
            <button class="quiet-button" type="button" data-vex-ref="${VexLifeWebP1.VexContextProfileHome.identity.ref}" id="context-profile-home">View in Vex Home</button>
          </div>
          <label class="context-profile-field" data-vex-ref="${VexLifeWebP1.VexContextProfileUse.identity.ref}">
            <span>Saved profile</span>
            <select id="context-profile-choice" aria-label="Choose a saved Context Profile"></select>
          </label>
          <label class="context-profile-field">
            <span>Name</span>
            <input id="context-profile-name" maxlength="80" value="VexLife Default">
          </label>
          <label class="context-profile-field">
            <span>Foundation</span>
            <select id="context-profile-foundation-mode">
              <option value="PRODUCT_DEFAULT">VexLife foundation</option>
              <option value="EXTEND_PRODUCT_DEFAULT">VexLife foundation + my extension</option>
            </select>
          </label>
          <label class="context-profile-field context-profile-foundation-text" id="context-profile-foundation-field" hidden>
            <span>My foundation</span>
            <textarea id="context-profile-foundation-text" rows="7" maxlength="16000" placeholder="Write the context you want Vex to receive here."></textarea>
            <small>Configuration only. This does not copy your conversations or memories into the profile.</small>
          </label>
          <div class="context-profile-actions">
            <button class="outline-button" type="button" data-vex-ref="${VexLifeWebP1.VexContextProfileSave.identity.ref}" id="context-profile-save">Save</button>
            <button class="quiet-button" type="button" data-vex-ref="${VexLifeWebP1.VexContextProfileSaveAs.identity.ref}" id="context-profile-save-as">Save as…</button>
            <button class="quiet-button" type="button" data-vex-ref="${VexLifeWebP1.VexContextProfileRestore.identity.ref}" id="context-profile-restore">Restore VexLife default</button>
            <a class="quiet-button context-profile-export" data-vex-ref="${VexLifeWebP1.VexContextProfilePortability.identity.ref}" id="context-profile-export" href="/api/v1/context-profiles/export" download>Export</a>
            <label class="quiet-button context-profile-import" data-vex-ref="${VexLifeWebP1.VexContextProfileImport.identity.ref}" for="context-profile-import">Import</label>
            <input id="context-profile-import" type="file" accept=".json,.vexcontext.json,application/json" hidden>
          </div>
          <p class="context-profile-note" id="context-profile-note" role="status" aria-live="polite"></p>
        </section>

        <section class="context-changes" data-vex-ref="${VexLifeWebP1.VexContextChanges.identity.ref}" aria-labelledby="context-changes-heading">
          <h3 id="context-changes-heading">Context changes</h3>
          <div id="context-changes-list" class="context-change-list">
            <p class="context-empty">No completed turn yet. Vex will show changes here as your context relationship develops.</p>
          </div>
        </section>

        <section class="context-causal" aria-labelledby="context-causal-heading">
          <div class="context-causal-heading">
            <div>
              <h3 id="context-causal-heading">What Vex received</h3>
              <p id="vex-context-turn-summary">No completed turn yet.</p>
            </div>
            <span id="vex-context-budget" class="context-budget"></span>
          </div>

          <div class="context-stack" data-vex-ref="${VexLifeWebP1.VexContextInput.identity.ref}">
            <details class="context-section context-labs" data-context-kind="vex-labs" open>
              <summary>
                <span>Vex Labs Native Test</span>
                <small id="vex-labs-status">Scratch native model test</small>
              </summary>
              <div class="context-section-body vex-labs-body">
                <p class="vex-labs-intro">Try a different context configuration against the real native model without writing the Lab conversation back into your real Conversation or Memory.</p>
                <div class="vex-labs-actions">
                  <button class="outline-button" type="button" id="vex-labs-test">Test configuration</button>
                  <button class="outline-button" type="button" id="vex-labs-save-supported">Save supported settings</button>
                  <button class="outline-button" type="button" id="vex-labs-restore">Restore defaults</button>
                  <button class="outline-button" type="button" id="vex-labs-reload">Reload current conversation</button>
                  <button class="outline-button" type="button" id="vex-labs-clear">Clear Lab</button>
                </div>
                <div class="vex-labs-quick-config">
                  <label>
                    <span>Active context</span>
                    <input id="vex-labs-input-budget" type="number" min="1" step="1024" inputmode="numeric">
                    <small>Temporary for Lab turns only.</small>
                  </label>
                  <label>
                    <span>Structured Memory</span>
                    <select id="vex-labs-memory-mode">
                      <option value="CURRENT">Use current Memory</option>
                      <option value="NONE">No Memory</option>
                    </select>
                    <small>Your real Memory is never reset by this switch.</small>
                  </label>
                  <label>
                    <span>Memory prompt depth</span>
                    <input id="vex-labs-memory-budget" type="number" min="0" step="1024" inputmode="numeric">
                    <small>Temporary maximum Memory tokens for Lab turns.</small>
                  </label>
                </div>
                <p class="vex-labs-note" id="vex-labs-note" role="status" aria-live="polite"></p>
                <div class="vex-labs-fields" id="vex-labs-fields"></div>
                <details class="vex-labs-request" id="vex-labs-request" hidden>
                  <summary>Last Lab model request</summary>
                  <p id="vex-labs-request-sha"></p>
                  <pre id="vex-labs-request-body"></pre>
                </details>
              </div>
            </details>
            <details class="context-section context-foundation" data-vex-ref="${VexLifeWebP1.VexContextInputFoundation.identity.ref}" data-context-kind="foundation" open>
              <summary><span>Foundation</span><small id="context-foundation-status">Standing context</small></summary>
              <div class="context-section-body" id="context-foundation-body"></div>
            </details>
            <details class="context-section context-memory" data-vex-ref="${VexLifeWebP1.VexContextInputMemory.identity.ref}" data-context-kind="memory">
              <summary><span>Structured Memory</span><small id="context-memory-status">Checking</small></summary>
              <div class="context-section-body" id="context-memory-body"></div>
            </details>
            <details class="context-section context-continuity" data-vex-ref="${VexLifeWebP1.VexContextInputContinuity.identity.ref}" data-context-kind="continuity">
              <summary><span>Conversation continuity</span><small id="context-continuity-status">Checking</small></summary>
              <div class="context-section-body" id="context-continuity-body"></div>
            </details>
            <details class="context-section context-conversation" data-vex-ref="${VexLifeWebP1.VexContextInputConversation.identity.ref}" data-context-kind="conversation" open>
              <summary><span>Conversation</span><small id="context-conversation-status">Checking</small></summary>
              <div class="context-section-body" id="context-conversation-body"></div>
            </details>
            <details class="context-section context-browser" data-vex-ref="${VexLifeWebP1.VexContextInputBrowser.identity.ref}" data-context-kind="browser">
              <summary><span>Browser / things Vex reached</span><small id="context-browser-status">Checking</small></summary>
              <div class="context-section-body" id="context-browser-body"></div>
            </details>
            <details class="context-section context-request" data-vex-ref="${VexLifeWebP1.VexContextInputRequest.identity.ref}" data-context-kind="request" open>
              <summary><span>Your message</span><small id="context-request-status">Current request</small></summary>
              <div class="context-section-body" id="context-request-body"></div>
            </details>
            <details class="context-section context-exact" data-vex-ref="${VexLifeWebP1.VexContextInputComponents.identity.ref}" data-context-kind="exact">
              <summary><span>How Vex put it together</span><small>Exact order available</small></summary>
              <ol class="context-component-list" id="context-component-list"></ol>
            </details>
          </div>

          <div class="context-flow-arrow" aria-hidden="true">↓</div>

          <details class="context-section context-process" data-vex-ref="${VexLifeWebP1.VexContextProcess.identity.ref}" data-context-kind="process" open>
            <summary><span>Vex's working process</span><small id="context-process-status">Session view</small></summary>
            <label class="context-process-setting">
              <span>Show process from</span>
              <select id="context-process-retention">
                <option value="SESSION_ONLY">This browser session</option>
                <option value="PREVIOUS_TURN">Previous reply only</option>
              </select>
            </label>
            <div class="context-section-body" id="context-process-body"></div>
          </details>

          <div class="context-flow-arrow" aria-hidden="true">↓</div>

          <details class="context-section context-output" data-vex-ref="${VexLifeWebP1.VexContextOutput.identity.ref}" data-context-kind="output" open>
            <summary><span>Vex's reply</span><small>Exact reply</small></summary>
            <div class="context-section-body" id="context-output-body"></div>
          </details>

          <div class="context-flow-arrow" aria-hidden="true">↓</div>

          <details class="context-section context-after" data-vex-ref="${VexLifeWebP1.VexContextAfter.identity.ref}" data-context-kind="after">
            <summary><span>After the turn</span><small>What stayed</small></summary>
            <div class="context-section-body" id="context-after-body"></div>
          </details>
        </section>
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

    <section class="region region-companion" data-vex-ref="${VexLifeWebP1.Companion.identity.ref}" data-vex-structural-ref="${VexLifeWebP1.Refs.CompanionRegion.value}" aria-labelledby="companion-title">
      <div class="companion-head" data-vex-structural-ref="${VexLifeWebP1.Refs.CompanionHeader.value}">
        <div class="companion-mode-shell">
          <h1 class="companion-title" id="companion-title">Converse with Vex</h1>
          <label class="visually-hidden" for="companion-mode">Conversation mode</label>
          <select class="companion-mode" id="companion-mode" aria-label="Conversation mode">
            <option value="CONVERSATION">Converse with Vex</option>
            <option value="LAB">Vex Labs Test</option>
          </select>
        </div>
        <p class="companion-status" data-vex-ref="${VexLifeWebP1.CompanionStatus.identity.ref}" id="companion-status" role="status" aria-live="polite">
          <span class="status-dot" aria-hidden="true"></span>
          <span id="companion-status-text">Vex not checked</span>
          <span class="status-model" id="companion-status-model"></span>
        </p>
        <button class="quiet-button" type="button" data-vex-legacy="CompanionCheck" id="companion-check" hidden>Check again</button>
        <button class="outline-button private-mode" type="button" data-vex-ref="${VexLifeWebP1.PrivateMode.identity.ref}" id="private-mode" aria-pressed="false" title="Everything you and Vex say here is kept in your Vex Home.">
          <span class="private-dot" aria-hidden="true"></span><span id="private-mode-label">Kept in your archives</span>
        </button>
        <button class="outline-button vision-control" type="button" data-vex-ref="${VexLifeWebP1.Vision.identity.ref}" id="vision-control"
          aria-describedby="vision-control-status" aria-disabled="true" disabled title="Vision availability is checking">
          <svg class="vision-mark" viewBox="0 0 20 20" aria-hidden="true"><path d="M2.4 10s2.8-4.3 7.6-4.3 7.6 4.3 7.6 4.3-2.8 4.3-7.6 4.3S2.4 10 2.4 10Z"/><circle cx="10" cy="10" r="2.3"/></svg>
          <span data-vex-text="workspace.label.vision">workspace.label.vision</span>
          <span class="vision-state" data-vex-text="workspace.status.vision-checking">workspace.status.vision-checking</span>
        </button>
        <span class="visually-hidden" id="vision-control-status" data-vex-text="workspace.description.vision-checking">workspace.description.vision-checking</span>
        <button class="outline-button view-switch" type="button" data-vex-ref="${VexLifeWebP1.CompanionViewSwitch.identity.ref}" id="view-switch" aria-controls="view-settings" aria-expanded="false">Settings</button>
      </div>

      <div class="conversation" data-vex-ref="${VexLifeWebP1.Conversation.identity.ref}" data-vex-structural-parent="${VexLifeWebP1.Refs.CompanionContent.value}" id="view-conversation">
        <section class="private-banner" data-vex-ref="${VexLifeWebP1.PrivateBanner.identity.ref}" id="private-banner" aria-live="polite" hidden>
          <p class="private-banner-text" id="private-banner-text"></p>
          <ul class="private-notes" id="private-notes"></ul>
          <div class="private-proposal" id="private-proposal" hidden>
            <p class="private-proposal-text" id="private-proposal-text"></p>
            <div class="private-proposal-actions">
              <button class="outline-button" type="button" id="private-keep">Keep this note</button>
              <button class="quiet-button" type="button" id="private-decline">Don't keep</button>
            </div>
          </div>
          <button class="quiet-button" type="button" id="private-propose">Keep something…</button>
        </section>
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

          <ul class="composer-attachments" data-vex-ref="${VexLifeWebP1.ComposerAttachments.identity.ref}" id="composer-attachments" aria-live="polite" hidden></ul>
          <form class="compose-row" id="compose-form">
            <button class="attach" type="button" data-vex-ref="${VexLifeWebP1.Attach.identity.ref}" id="attach" aria-label="workspace.button.attach">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8.5 12.5l6-6a3 3 0 0 1 4.2 4.2l-7.4 7.4a5 5 0 0 1-7.1-7.1l6.7-6.7"/></svg>
            </button>
            <input class="visually-hidden" type="file" id="attach-input" multiple tabindex="-1" aria-hidden="true">
            <label class="visually-hidden" for="draft">Message Vex</label>
            <textarea id="draft" rows="1" placeholder="Message Vex" enterkeyhint="send"></textarea>
            <div class="voice-input-shell">
              <button class="voice-input" type="button" data-vex-ref="${VexLifeWebP1.VoiceInput.identity.ref}" id="voice-input" aria-pressed="false" aria-expanded="false" aria-label="workspace.button.voice-input-start">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="9" y="3.5" width="6" height="11" rx="3"/><path d="M6.5 10.5a5.5 5.5 0 0 0 11 0M12 16v4M9 20h6"/></svg>
              </button>
              <div class="voice-input-overlay" id="voice-input-overlay" hidden>
                <canvas id="voice-waveform" width="112" height="44" aria-hidden="true"></canvas>
                <p id="voice-input-status" role="status" aria-live="polite"></p>
              </div>
            </div>
            <button class="send" type="submit" data-vex-ref="${VexLifeWebP1.Send.identity.ref}" id="send" data-vex-text="workspace.button.conversation-send" aria-label=""></button>
            <button class="stop" type="button" data-vex-ref="${VexLifeWebP1.Stop.identity.ref}" id="stop" data-vex-text="workspace.button.conversation-stop" aria-label="" hidden></button>
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

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsLocalSources.identity.ref}" aria-labelledby="local-sources-heading">
          <div class="setting-row">
            <div>
              <h2 id="local-sources-heading" data-vex-text="workspace.heading.local-sources">Local folders</h2>
              <p class="setting-note-line local-sources-description" data-vex-text="workspace.description.local-sources">Folders on this computer Vex may read when you ask.</p>
            </div>
            <button class="outline-button" type="button" data-vex-ref="${VexLifeWebP1.LocalSourcesAdd.identity.ref}" id="local-sources-add" data-vex-text="workspace.button.local-sources-add">Add folder</button>
          </div>
          <ul class="local-sources" data-vex-ref="${VexLifeWebP1.LocalSourcesList.identity.ref}" id="local-sources-list"></ul>
          <p class="setting-note-line" id="local-sources-status" role="status" aria-live="polite"></p>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}" aria-labelledby="continuity-heading">
          <h2 id="continuity-heading">Continuity</h2>
          <div class="memory-state" data-vex-ref="${VexLifeWebP1.ContinuityMemory.identity.ref}" id="vex-memory">
            <p class="memory-label" id="vex-memory-label" data-vex-text="workspace.status.memory-checking">workspace.status.memory-checking</p>
            <p class="setting-note-line" id="vex-memory-detail" data-vex-text="workspace.description.memory-checking">workspace.description.memory-checking</p>
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


        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsModels.identity.ref}" aria-labelledby="models-heading">
          <h2 id="models-heading" data-vex-text="workspace.heading.models"></h2>
          <p class="setting-note-line" data-vex-text="workspace.description.models"></p>

          <div class="model-import-card">
            <p class="memory-label" data-vex-text="workspace.heading.model-import"></p>
            <p class="setting-note-line" data-vex-text="workspace.description.model-import"></p>
            <label class="model-import-field">
              <span data-vex-text="workspace.label.model-import-url"></span>
              <input id="model-import-url" type="url" inputmode="url" autocomplete="off" spellcheck="false">
            </label>
            <div class="model-import-actions">
              <button class="outline-button" id="model-import-check" type="button" data-vex-text="workspace.button.model-import-check"></button>
              <button class="outline-button" id="model-import-add" type="button" data-vex-text="workspace.button.model-import-add" hidden></button>
            </div>
            <p class="setting-note-line" id="model-import-result" role="status" aria-live="polite"></p>
            <p class="model-import-facts" id="model-import-facts"></p>
          </div>

          <div class="model-state-card" data-vex-ref="${VexLifeWebP1.ModelsState.identity.ref}" id="models-state">
            <p class="memory-label" data-vex-text="workspace.heading.model-swap-process"></p>
            <p class="setting-note-line" id="models-current-state"></p>
          </div>

          <div class="model-choice-field" data-vex-ref="${VexLifeWebP1.ModelChoice.identity.ref}">
            <span class="home-label" id="model-choice-label" data-vex-text="workspace.label.model-choice"></span>
            <div class="model-choice-row">
              <select id="model-choice" aria-labelledby="model-choice-label"></select>
              <div class="model-choice-actions" data-vex-ref="${VexLifeWebP1.ModelsList.identity.ref}" id="models-list"></div>
            </div>
            <p class="model-choice-facts" id="model-choice-facts"></p>
            <p class="model-choice-notes" id="model-choice-notes"></p>
          </div>

          <div class="model-configuration" data-vex-ref="${VexLifeWebP1.ModelConfiguration.identity.ref}" id="model-configuration">
            <div>
              <p class="memory-label" data-vex-text="workspace.heading.model-configuration"></p>
              <p class="setting-note-line" id="model-configuration-recommendation"></p>
            </div>
            <label class="model-cache-field" data-vex-ref="${VexLifeWebP1.ModelPromptCacheEntries.identity.ref}">
              <span data-vex-text="workspace.label.model-prompt-cache-entries"></span>
              <select id="model-prompt-cache-entries"></select>
            </label>
            <p class="setting-note-line" data-vex-text="workspace.description.model-prompt-cache-entries"></p>
            <div class="model-configuration-actions">
              <button class="outline-button" data-vex-ref="${VexLifeWebP1.ModelUseRecommended.identity.ref}" id="model-use-recommended" type="button" data-vex-text="workspace.button.model-use-recommended"></button>
            </div>
            <div class="model-activation">
              <p class="model-activation-title" data-vex-text="workspace.heading.model-activation"></p>
              <p class="setting-note-line" data-vex-text="workspace.description.model-start-effect"></p>
              <button class="model-start-button" data-vex-ref="${VexLifeWebP1.ModelStart.identity.ref}" id="model-start" type="button" data-vex-text="workspace.button.model-start"></button>
            </div>
          </div>

          <p class="setting-note-line" id="models-note"></p>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsLocalAsr.identity.ref}" aria-labelledby="local-asr-heading">
          <h2 id="local-asr-heading" data-vex-text="workspace.heading.local-asr"></h2>
          <p class="setting-note-line" data-vex-text="workspace.description.local-asr"></p>
          <div class="local-asr-row">
            <label for="local-asr-selection" data-vex-text="workspace.label.local-asr-selection"></label>
            <select id="local-asr-selection" data-vex-ref="${VexLifeWebP1.LocalAsrSelection.identity.ref}" disabled></select>
          </div>
          <div class="models-list" id="local-asr-models-list" data-vex-ref="${VexLifeWebP1.LocalAsrModelsList.identity.ref}"></div>
          <div class="local-asr-row">
            <label for="local-asr-spoken-language" data-vex-text="workspace.label.local-asr-spoken-language"></label>
            <select id="local-asr-spoken-language" data-vex-ref="${VexLifeWebP1.LocalAsrSpokenLanguage.identity.ref}" disabled></select>
          </div>
          <p class="setting-note-line" data-vex-text="workspace.description.local-asr-spoken-language-note"></p>
          <div class="local-asr-row">
            <label for="local-asr-composer-output" data-vex-text="workspace.label.local-asr-composer-output"></label>
            <select id="local-asr-composer-output" data-vex-ref="${VexLifeWebP1.LocalAsrComposerOutput.identity.ref}" disabled></select>
          </div>
          <p class="setting-note-line" data-vex-text="workspace.description.local-asr-composer-output-note"></p>
          <p class="setting-note-line" data-vex-ref="${VexLifeWebP1.LocalAsrStatus.identity.ref}" id="local-asr-status" role="status"></p>
          <p class="setting-note-line" id="local-asr-note" data-vex-text="workspace.description.local-asr-note"></p>
          <p class="setting-note-line" id="local-asr-storage-note" data-vex-ref="${VexLifeWebP1.LocalAsrStorageNote.identity.ref}"></p>
        </section>

        <section class="setting" data-vex-ref="${VexLifeWebP1.SettingsDiagnostics.identity.ref}" aria-labelledby="diagnostics-heading">
          <h2 id="diagnostics-heading" data-vex-text="workspace.heading.diagnostics"></h2>
          <div class="memory-state" data-vex-ref="${VexLifeWebP1.DiagnosticsTrace.identity.ref}" id="diagnostics-trace">
            <p class="memory-label" id="diagnostics-status"></p>
            <p class="setting-note-line" id="diagnostics-capture"></p>
            <p class="setting-note-line" id="diagnostics-records"></p>
          </div>
          <p class="setting-note-line" data-vex-text="workspace.description.diagnostics"></p>
          <div class="home-row">
            <button class="outline-button" type="button" data-vex-ref="${VexLifeWebP1.DiagnosticsEnhancedCapture.identity.ref}" id="diagnostics-enhanced" data-vex-text="workspace.button.diagnostics-start-enhanced" disabled></button>
            <button class="outline-button" type="button" data-vex-ref="${VexLifeWebP1.DiagnosticsClearHistory.identity.ref}" id="diagnostics-clear" data-vex-text="workspace.button.diagnostics-clear-history" disabled></button>
            <a class="outline-button" data-vex-ref="${VexLifeWebP1.DiagnosticsExport.identity.ref}" id="diagnostics-export" href="/api/v1/diagnostics/export" download data-vex-text="workspace.button.diagnostics-export"></a>
          </div>
          <p class="setting-note-line" data-vex-text="workspace.description.diagnostics-enhanced-note"></p>
          <p class="setting-note-line" data-vex-text="workspace.description.diagnostics-export-note"></p>
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


  <div class="overlay" id="overlay-root" data-vex-structural-ref="${VexLifeWebP1.Refs.OverlayRoot.value}">
    <p class="snackbar" id="snackbar" data-vex-structural-ref="${VexLifeWebP1.Refs.Snackbar.value}" role="status" aria-live="polite" hidden></p>
  </div>

  <p class="binding-failure" id="binding-failure" role="alert" hidden></p>
</div>
</body>
</html>
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
