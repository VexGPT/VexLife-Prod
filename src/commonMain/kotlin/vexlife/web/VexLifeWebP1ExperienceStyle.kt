package vexlife.web

/**
 * Product-specific deterministic presentation consequences for the generated P1 workspace.
 *
 * This source consumes accepted Blueprint/VexWeb identities. It does not own Conversation,
 * browser/provider behavior, or semantic product state. The visual language is adapted from the
 * current VexLife `reference/browser/app.css` token/experience source while VexLife-Prod remains the
 * product/runtime authority.
 */
internal object VexLifeWebP1ExperienceStyle {
    fun render(): String =
        """
        /* Generated VexLife-Prod experience manifestation. Do not edit generated CSS by hand. */
        html{color-scheme:dark;background:#0f1012;}
        body{background:#0f1012;color:#f5f5f4;}
        *,*::before,*::after{box-sizing:border-box;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"]{--vex-surface-canvas:#0f1012;--vex-surface-panel:#17181c;--vex-surface-raised:#202126;--vex-surface-overlay:#272930;--vex-line:#383a42;--vex-strong:#5a5d68;--vex-text-primary:#f5f5f4;--vex-text-secondary:#b7b8bf;--vex-text-faint:#858791;--vex-accent:#f2f2ef;--vex-accent-text:#111216;--vex-attention:#e9c46a;--vex-info:#9bb7d4;--vex-shadow:0 18px 55px rgba(0,0,0,.35);--vex-motion-tactile:90ms;--vex-motion-fast:140ms;--vex-ease-tactile:cubic-bezier(.2,.8,.2,1);background:var(--vex-surface-canvas);color:var(--vex-text-primary);font:400 17px/1.55 Inter,system-ui,-apple-system,"Segoe UI",sans-serif;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] textarea{font:inherit;color:inherit;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button{min-width:44px;min-height:44px;cursor:pointer;transition:transform var(--vex-motion-tactile) var(--vex-ease-tactile),background-color var(--vex-motion-tactile) linear,border-color var(--vex-motion-tactile) linear;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button:not(:disabled):active{transform:scale(.98);}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button:focus-visible,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] textarea:focus-visible,[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:focus-visible{outline:2px solid var(--vex-info);outline-offset:2px;}
        [data-vex-ref="${VexLifeWebP1.Refs.HeaderRegion.value}"]{background:color-mix(in srgb,var(--vex-surface-panel) 96%,transparent);border-bottom:1px solid var(--vex-line);}
        [data-vex-ref="${VexLifeWebP1.Header.identity.ref}"]{height:100%;display:flex;align-items:center;gap:12px;padding:10px 14px;}
        [data-vex-ref="${VexLifeWebP1.Utilities.identity.ref}"]{display:flex;align-items:center;min-width:max-content;}
        [data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"]{display:flex;align-items:center;}
        [data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"] [data-vex-generated-part="control"]{width:44px;height:44px;min-width:44px;min-height:44px;border:1px solid var(--vex-line);border-radius:14px;background:var(--vex-surface-raised);color:var(--vex-text-primary);padding:0;font:700 18px/1 Georgia,"Times New Roman",serif;box-shadow:inset 0 0 0 1px color-mix(in srgb,var(--vex-accent) 10%,transparent);}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]{display:grid;align-content:center;gap:2px;min-width:max-content;letter-spacing:-.01em;}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"] [data-vex-generated-part="brand-name"]{color:var(--vex-text-primary);font:700 18px/1 Georgia,"Times New Roman",serif;}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"] [data-vex-generated-part="brand-home"]{color:var(--vex-text-faint);font:700 9px/1.2 ui-monospace,"SFMono-Regular",Menlo,monospace;letter-spacing:.08em;}
        [data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}"]{flex:1;max-width:760px;min-width:160px;}
        [data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}"] [data-vex-generated-part="text-field"]{height:44px;min-height:44px;resize:none;overflow:hidden;border:1px solid var(--vex-line);border-radius:12px;background:var(--vex-surface-raised);padding:10px 12px;color:var(--vex-text-primary);}
        [data-vex-ref="${VexLifeWebP1.AppSelector.identity.ref}"]{display:none;}
        [data-vex-ref="${VexLifeWebP1.Refs.BodyFrame.value}"]{background:var(--vex-surface-canvas);}
        [data-vex-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"]{left:0;width:100%;padding:12px;background:var(--vex-surface-canvas);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"]{left:var(--vex-context-workspace-boundary);width:calc(100% - var(--vex-context-workspace-boundary));padding:12px 12px 12px 6px;background:transparent;}
        [data-vex-ref="${VexLifeWebP1.Browser.identity.ref}"]{height:100%;min-height:0;}
        [data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}"]{height:100%;min-height:0;overflow:hidden;border:1px solid var(--vex-line);border-radius:18px;background:var(--vex-surface-panel);box-shadow:var(--vex-shadow);}
        [data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}"] iframe{display:block;width:100%;height:100%;background:#fff;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]{left:calc(var(--vex-context-workspace-boundary) - 6px);top:0;width:12px;height:100%;cursor:col-resize;z-index:18;touch-action:none;background:transparent;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]::before{content:"";position:absolute;left:5px;top:12px;bottom:12px;width:2px;border-radius:999px;background:color-mix(in srgb,var(--vex-text-secondary) 36%,transparent);transition:background-color var(--vex-motion-fast) ease,box-shadow var(--vex-motion-fast) ease;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:hover::before,[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:focus-visible::before{background:var(--vex-info);box-shadow:0 0 18px color-mix(in srgb,var(--vex-info) 32%,transparent);}
        [data-vex-ref="${VexLifeWebP1.Companion.identity.ref}"]{height:100%;min-height:0;display:grid;grid-template-rows:auto minmax(0,1fr);overflow:hidden;border:1px solid var(--vex-line);border-radius:18px;background:color-mix(in srgb,var(--vex-surface-panel) 98%,transparent);box-shadow:var(--vex-shadow);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionHeader.value}"]{min-height:72px;display:grid;grid-template-columns:minmax(0,1fr) auto;grid-template-rows:auto auto;align-content:center;column-gap:12px;padding:11px 14px;border-bottom:1px solid var(--vex-line);background:color-mix(in srgb,var(--vex-surface-panel) 96%,transparent);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionHeader.value}"] [data-vex-generated-part="companion-eyebrow"]{grid-column:1;color:var(--vex-text-faint);font:700 10px/1.2 ui-monospace,"SFMono-Regular",Menlo,monospace;letter-spacing:.11em;}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionHeader.value}"] [data-vex-generated-part="companion-title"]{grid-column:1;color:var(--vex-text-primary);font:700 22px/1.1 Georgia,"Times New Roman",serif;}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionHeader.value}"] [data-vex-generated-part="companion-status"]{grid-column:2;grid-row:1 / span 2;align-self:center;color:var(--vex-text-secondary);font-size:11px;}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionContent.value}"]{min-height:0;display:grid;grid-template-columns:minmax(0,1.6fr) minmax(210px,.7fr);gap:12px;padding:12px;overflow:hidden;}
        [data-vex-ref="${VexLifeWebP1.Conversation.identity.ref}"]{min-height:0;display:grid;grid-template-rows:minmax(0,1fr) auto auto auto;gap:8px;overflow:hidden;border:1px solid var(--vex-line);border-radius:14px;background:var(--vex-surface-raised);padding:12px;}
        [data-vex-ref="${VexLifeWebP1.ConversationTimeline.identity.ref}"]{min-height:0;overflow:auto;overscroll-behavior:contain;scrollbar-gutter:stable;padding:2px 4px 2px 0;}
        [data-vex-generated-part="conversation-message"]{display:grid;grid-template-columns:34px minmax(0,1fr);gap:9px;padding:10px 8px;border-bottom:1px solid color-mix(in srgb,var(--vex-line) 70%,transparent);}
        [data-vex-generated-part="conversation-message"][data-vex-conversation-role="human"]{margin-left:8%;border-radius:12px;background:color-mix(in srgb,var(--vex-accent) 7%,transparent);}
        [data-vex-generated-part="conversation-avatar"]{width:30px;height:30px;display:grid;place-items:center;border:1px solid var(--vex-line);border-radius:9px;background:var(--vex-surface-raised);color:var(--vex-text-secondary);font:700 11px/1 ui-monospace,"SFMono-Regular",Menlo,monospace;}
        [data-vex-conversation-role="vex"] [data-vex-generated-part="conversation-avatar"]{color:var(--vex-info);border-color:color-mix(in srgb,var(--vex-info) 38%,var(--vex-line));}
        [data-vex-generated-part="conversation-message-body"] strong{display:block;margin-bottom:3px;font-size:12px;color:var(--vex-text-secondary);}
        [data-vex-generated-part="conversation-message-body"] p{margin:0;white-space:pre-wrap;overflow-wrap:anywhere;color:var(--vex-text-primary);font-size:15px;line-height:1.55;}
        [data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}"]{display:flex;justify-content:center;}
        [data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}"] [data-vex-generated-part="control"]{border:1px solid var(--vex-line);border-radius:999px;background:var(--vex-surface-raised);padding:7px 12px;color:var(--vex-text-secondary);font-size:12px;}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"]{min-height:22px;color:var(--vex-text-secondary);font-size:12px;font-style:italic;}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="sending"]{color:var(--vex-info);}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="error"],[data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="browser-error"]{color:var(--vex-attention);}
        [data-vex-ref="${VexLifeWebP1.Composer.identity.ref}"]{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:8px;padding-top:10px;border-top:1px solid var(--vex-line);}
        [data-vex-ref="${VexLifeWebP1.Composer.identity.ref}"]>[data-vex-generated-part="text-field"]{min-height:48px;max-height:144px;resize:vertical;border:1px solid var(--vex-line);border-radius:12px;background:var(--vex-surface-raised);padding:11px 12px;color:var(--vex-text-primary);}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"]{display:flex;align-items:flex-end;}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"] [data-vex-generated-part="control"]{border:0;border-radius:11px;background:var(--vex-accent);padding:10px 16px;color:var(--vex-accent-text);font-weight:750;}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"] [data-vex-generated-part="control"]:disabled{cursor:not-allowed;opacity:.5;}
        [data-vex-ref="${VexLifeWebP1.Settings.identity.ref}"]{min-height:0;overflow:auto;border:1px solid var(--vex-line);border-radius:14px;background:var(--vex-surface-raised);padding:12px;color:var(--vex-text-secondary);font-size:12px;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"]{border:0;border-radius:0;background:transparent;padding:0;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] h3,[data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] h4{margin:0 0 7px;color:var(--vex-text-primary);font:700 12px/1.2 ui-monospace,"SFMono-Regular",Menlo,monospace;letter-spacing:.02em;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] dl{display:grid;grid-template-columns:auto minmax(0,1fr);gap:4px 10px;margin:0 0 10px;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] dt{color:var(--vex-text-secondary);}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] dd{margin:0;color:var(--vex-text-primary);overflow-wrap:anywhere;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] section{margin-top:7px;padding-top:7px;border-top:1px solid color-mix(in srgb,var(--vex-line) 70%,transparent);}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] ul{margin:4px 0 0;padding-left:18px;}
        [data-vex-ref="${VexLifeWebP1.SettingsResources.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsHome.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsReset.identity.ref}"]{display:none;}
        @media(max-width:900px){[data-vex-ref="${VexLifeWebP1.Header.identity.ref}"]{gap:8px;padding-inline:10px;}[data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]{font-size:16px;}[data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"] [data-vex-generated-part="control"]{width:40px;height:40px;min-width:40px;min-height:40px;}[data-vex-ref="${VexLifeWebP1.Refs.CompanionContent.value}"]{grid-template-columns:1fr;grid-template-rows:minmax(0,1fr) auto;}[data-vex-ref="${VexLifeWebP1.Settings.identity.ref}"]{max-height:34%;}}
        @media(max-width:700px){[data-vex-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"]{padding:8px;}[data-vex-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"]{padding:8px 8px 8px 4px;}[data-vex-ref="${VexLifeWebP1.Companion.identity.ref}"]{border-radius:14px;}[data-vex-ref="${VexLifeWebP1.Refs.CompanionContent.value}"]{padding:9px;gap:9px;}[data-vex-generated-part="conversation-message"][data-vex-conversation-role="human"]{margin-left:0;}}
        @media(prefers-reduced-motion:reduce){[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *::before,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *::after{animation:none!important;transition:none!important;scroll-behavior:auto!important;}}
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
