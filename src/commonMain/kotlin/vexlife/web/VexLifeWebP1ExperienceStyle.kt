package vexlife.web

/**
 * Product-specific deterministic presentation consequences for the generated P1 workspace.
 *
 * This source consumes accepted Blueprint/VexWeb identities. It does not own Conversation,
 * browser/provider behavior, or semantic product state. The visual language is adapted from the
 * current VexLife experience evidence and design-token source while VexLife-Prod remains the
 * product/runtime authority.
 */
internal object VexLifeWebP1ExperienceStyle {
    fun render(): String =
        """
        /* Generated VexLife-Prod experience manifestation. Do not edit generated CSS by hand. */
        html{color-scheme:dark;background:#081016;}
        body{background:#081016;color:#F4F7FA;}
        *,*::before,*::after{box-sizing:border-box;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"]{--vex-surface-canvas:#081016;--vex-surface-panel:#101A23;--vex-surface-raised:#172431;--vex-text-primary:#F4F7FA;--vex-text-secondary:#A8BAC9;--vex-accent:#73A8FF;--vex-focus:#8CC2FF;--vex-healthy:#67D792;--vex-attention:#F4C56A;--vex-blocked:#F47B7B;--vex-line:color-mix(in srgb,#A8BAC9 22%,transparent);--vex-shadow:0 22px 70px rgba(0,0,0,.38);background:var(--vex-surface-canvas);color:var(--vex-text-primary);font:400 17px/1.55 Inter,system-ui,-apple-system,"Segoe UI",sans-serif;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] textarea{font:inherit;color:inherit;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button{min-width:44px;min-height:44px;cursor:pointer;transition:transform 120ms ease,background-color 120ms ease,border-color 120ms ease;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button:not(:disabled):active{transform:scale(.98);}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button:focus-visible,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] textarea:focus-visible,[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:focus-visible{outline:3px solid var(--vex-focus);outline-offset:2px;}
        [data-vex-ref="${VexLifeWebP1.Refs.HeaderRegion.value}"]{background:color-mix(in srgb,var(--vex-surface-panel) 96%,transparent);border-bottom:1px solid var(--vex-line);}
        [data-vex-ref="${VexLifeWebP1.Header.identity.ref}"]{height:100%;display:flex;align-items:center;gap:12px;padding:10px 14px;}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]{display:flex;align-items:center;gap:10px;min-width:max-content;font:700 18px/1 Georgia,"Times New Roman",serif;letter-spacing:-.01em;}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]::before{content:"V";width:42px;height:42px;display:grid;place-items:center;border:1px solid var(--vex-line);border-radius:14px;background:var(--vex-surface-raised);color:var(--vex-text-primary);font:700 18px/1 Georgia,"Times New Roman",serif;box-shadow:inset 0 0 0 1px color-mix(in srgb,var(--vex-accent) 10%,transparent);}
        [data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}"]{flex:1;max-width:760px;min-width:160px;}
        [data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}"] [data-vex-generated-part="text-field"]{height:44px;min-height:44px;resize:none;overflow:hidden;border:1px solid var(--vex-line);border-radius:12px;background:var(--vex-surface-raised);padding:10px 12px;color:var(--vex-text-primary);}
        [data-vex-ref="${VexLifeWebP1.AppSelector.identity.ref}"],[data-vex-ref="${VexLifeWebP1.Utilities.identity.ref}"]{display:none;}
        [data-vex-ref="${VexLifeWebP1.Refs.BodyFrame.value}"]{--vex-browser-share:50%;background:var(--vex-surface-canvas);}
        [data-vex-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"]{left:0;width:var(--vex-browser-share);padding:12px 6px 12px 12px;background:var(--vex-surface-canvas);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"]{left:var(--vex-browser-share);width:calc(100% - var(--vex-browser-share));padding:12px 12px 12px 6px;background:var(--vex-surface-canvas);}
        [data-vex-ref="${VexLifeWebP1.Browser.identity.ref}"]{height:100%;min-height:0;}
        [data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}"]{height:100%;min-height:0;overflow:hidden;border:1px solid var(--vex-line);border-radius:18px;background:var(--vex-surface-panel);box-shadow:var(--vex-shadow);}
        [data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}"] iframe{display:block;width:100%;height:100%;background:#fff;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]{left:calc(var(--vex-browser-share) - 6px);top:0;width:12px;height:100%;cursor:col-resize;z-index:18;touch-action:none;background:transparent;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]::before{content:"";position:absolute;left:5px;top:12px;bottom:12px;width:2px;border-radius:999px;background:color-mix(in srgb,var(--vex-text-secondary) 36%,transparent);transition:background-color 120ms ease,box-shadow 120ms ease;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:hover::before,[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:focus-visible::before{background:var(--vex-accent);box-shadow:0 0 18px color-mix(in srgb,var(--vex-accent) 32%,transparent);}
        [data-vex-ref="${VexLifeWebP1.Companion.identity.ref}"]{height:100%;min-height:0;display:grid;grid-template-rows:minmax(0,1fr) auto;gap:10px;overflow:hidden;border:1px solid var(--vex-line);border-radius:18px;background:var(--vex-surface-panel);box-shadow:var(--vex-shadow);padding:12px;}
        [data-vex-ref="${VexLifeWebP1.Conversation.identity.ref}"]{min-height:0;display:grid;grid-template-rows:minmax(0,1fr) auto auto auto;gap:8px;}
        [data-vex-ref="${VexLifeWebP1.ConversationTimeline.identity.ref}"]{min-height:0;overflow:auto;overscroll-behavior:contain;scrollbar-gutter:stable;padding:2px 4px 2px 0;}
        [data-vex-generated-part="conversation-message"]{display:grid;grid-template-columns:34px minmax(0,1fr);gap:9px;padding:10px 8px;border-bottom:1px solid color-mix(in srgb,var(--vex-line) 70%,transparent);}
        [data-vex-generated-part="conversation-message"][data-vex-conversation-role="human"]{margin-left:8%;border-radius:12px;background:color-mix(in srgb,var(--vex-accent) 7%,transparent);}
        [data-vex-generated-part="conversation-avatar"]{width:30px;height:30px;display:grid;place-items:center;border:1px solid var(--vex-line);border-radius:9px;background:var(--vex-surface-raised);color:var(--vex-text-secondary);font:700 11px/1 ui-monospace,"SFMono-Regular",Menlo,monospace;}
        [data-vex-conversation-role="vex"] [data-vex-generated-part="conversation-avatar"]{color:var(--vex-accent);border-color:color-mix(in srgb,var(--vex-accent) 38%,var(--vex-line));}
        [data-vex-generated-part="conversation-message-body"] strong{display:block;margin-bottom:3px;font-size:12px;color:var(--vex-text-secondary);}
        [data-vex-generated-part="conversation-message-body"] p{margin:0;white-space:pre-wrap;overflow-wrap:anywhere;color:var(--vex-text-primary);font-size:15px;line-height:1.55;}
        [data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}"]{display:flex;justify-content:center;}
        [data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}"] [data-vex-generated-part="control"]{border:1px solid var(--vex-line);border-radius:999px;background:var(--vex-surface-raised);padding:7px 12px;color:var(--vex-text-secondary);font-size:12px;}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"]{min-height:22px;color:var(--vex-text-secondary);font-size:12px;font-style:italic;}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="sending"]{color:var(--vex-accent);}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="error"],[data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="browser-error"]{color:var(--vex-blocked);}
        [data-vex-ref="${VexLifeWebP1.Composer.identity.ref}"]{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:end;gap:8px;padding-top:10px;border-top:1px solid var(--vex-line);}
        [data-vex-ref="${VexLifeWebP1.Composer.identity.ref}"]>[data-vex-generated-part="text-field"]{min-height:48px;max-height:144px;resize:vertical;border:1px solid var(--vex-line);border-radius:12px;background:var(--vex-surface-raised);padding:11px 12px;color:var(--vex-text-primary);}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"]{display:flex;align-items:flex-end;}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"] [data-vex-generated-part="control"]{border:0;border-radius:11px;background:var(--vex-accent);padding:10px 16px;color:#081016;font-weight:750;}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"] [data-vex-generated-part="control"]:disabled{cursor:not-allowed;opacity:.5;}
        [data-vex-ref="${VexLifeWebP1.Settings.identity.ref}"]{max-height:34%;overflow:auto;border-top:1px solid var(--vex-line);padding-top:10px;color:var(--vex-text-secondary);font-size:12px;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"]{border:1px solid var(--vex-line);border-radius:13px;background:var(--vex-surface-raised);padding:10px 12px;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] h3,[data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] h4{margin:0 0 7px;color:var(--vex-text-primary);font:700 12px/1.2 ui-monospace,"SFMono-Regular",Menlo,monospace;letter-spacing:.02em;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] dl{display:grid;grid-template-columns:auto minmax(0,1fr);gap:4px 10px;margin:0 0 10px;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] dt{color:var(--vex-text-secondary);}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] dd{margin:0;color:var(--vex-text-primary);overflow-wrap:anywhere;}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] section{margin-top:7px;padding-top:7px;border-top:1px solid color-mix(in srgb,var(--vex-line) 70%,transparent);}
        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"] ul{margin:4px 0 0;padding-left:18px;}
        [data-vex-ref="${VexLifeWebP1.SettingsResources.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsHome.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsReset.identity.ref}"]{display:none;}
        @media(max-width:900px){[data-vex-ref="${VexLifeWebP1.Refs.BodyFrame.value}"]{--vex-browser-share:44%;}[data-vex-ref="${VexLifeWebP1.Header.identity.ref}"]{gap:8px;padding-inline:10px;}[data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]{font-size:16px;}[data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]::before{width:38px;height:38px;}}
        @media(max-width:700px){[data-vex-ref="${VexLifeWebP1.Refs.BodyFrame.value}"]{--vex-browser-share:38%;}[data-vex-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"]{padding:8px 4px 8px 8px;}[data-vex-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"]{padding:8px 8px 8px 4px;}[data-vex-ref="${VexLifeWebP1.Companion.identity.ref}"]{border-radius:14px;padding:9px;}[data-vex-generated-part="conversation-message"][data-vex-conversation-role="human"]{margin-left:0;}}
        @media(prefers-reduced-motion:reduce){[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *::before,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *::after{animation:none!important;transition:none!important;scroll-behavior:auto!important;}}
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
