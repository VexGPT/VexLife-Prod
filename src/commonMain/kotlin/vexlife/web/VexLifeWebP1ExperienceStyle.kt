package vexlife.web

/**
 * Product-specific deterministic presentation consequences for the generated VexLife workspace.
 *
 * The perceptual reference is the VexLife-Prod A003 nested workspace: quiet neutral chrome,
 * peer Browser/Companion panes around one Seam, Conversation/Settings as alternate Companion views,
 * restrained radii, and a paper context ticket. Blueprint/VexWeb relationships remain source
 * authority; these measurements are renderer consequences rather than new semantic owners.
 */
internal object VexLifeWebP1ExperienceStyle {
    fun render(): String =
        """
        /* Generated VexLife-Prod experience continuity restoration. Do not edit generated CSS. */
        :root{--ground:#eef0ee;--surface:#fff;--sunken:#e8eae7;--raised:#f6f7f5;--rule:#d4d7d2;--rule-strong:#b4b9b2;--ink:#1b1e22;--ink-2:#555c66;--ink-3:#868d97;--you:#e6edf6;--you-rule:#c9d6e6;--vex:#fff;--vex-rule:#cfd3cd;--core:#a7c4ea;--used:#f2d877;--remaining:#a6dbb1;--ready:#2f9e55;--danger:#d6372f;--paper:#efe5cd;--paper-ink:#2c271e;--paper-rule:#c8b993;--focus:#3c78c8;--shadow:0 10px 30px rgba(20,24,30,.12);--sans:-apple-system,BlinkMacSystemFont,"SF Pro Text","Inter",system-ui,sans-serif;--voice:"Iowan Old Style","Palatino Linotype",Palatino,"Book Antiqua",Georgia,serif;--mono:ui-monospace,"SF Mono",Menlo,Consolas,monospace;--step--1:12px;--step-0:13.5px;--step-1:15px;--step-2:17px;--chrome-height:64px;--ease:cubic-bezier(.2,.75,.2,1);color-scheme:light;}
        @media(prefers-color-scheme:dark){:root:not([data-theme="light"]){--ground:#101317;--surface:#171b20;--sunken:#1f242b;--raised:#1c2026;--rule:#2b313a;--rule-strong:#3f4651;--ink:#e6e9ed;--ink-2:#a2aab5;--ink-3:#6f7783;--you:#22303f;--you-rule:#334459;--vex:#1d2228;--vex-rule:#323944;--core:#5f8ed0;--used:#cdab43;--remaining:#57a96d;--ready:#6fcb8d;--danger:#d9443b;--focus:#8fb4e8;--shadow:0 14px 40px rgba(0,0,0,.45);color-scheme:dark;}}
        *,*::before,*::after{box-sizing:border-box;}
        html,body{background:var(--ground);color:var(--ink);}
        body{font:400 var(--step-0)/1.45 var(--sans);-webkit-font-smoothing:antialiased;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"]{background:var(--ground);color:var(--ink);font:400 var(--step-0)/1.45 var(--sans);}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] textarea{font:inherit;color:inherit;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button:focus-visible,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] textarea:focus-visible,[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:focus-visible{outline:2px solid var(--focus);outline-offset:2px;}

        [data-vex-ref="${VexLifeWebP1.Refs.HeaderRegion.value}"]{background:var(--surface);border-bottom:1px solid var(--rule);}
        [data-vex-ref="${VexLifeWebP1.Header.identity.ref}"]{height:100%;display:grid;grid-template-columns:auto minmax(220px,520px) auto minmax(0,1fr);align-items:center;gap:14px;padding-left:14px;}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"]{display:flex;align-items:center;gap:10px;min-width:max-content;color:var(--ink);}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"] [data-vex-generated-part="brand-mark"]{display:grid;place-items:center;width:40px;height:40px;border-radius:9px;background:#1b1e22;color:#f4f4f1;font:650 15px/1 var(--sans);}
        [data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"] [data-vex-generated-part="brand-name"]{font-weight:650;font-size:var(--step-2);letter-spacing:-.01em;}
        [data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}"]{min-width:0;}
        [data-vex-ref="${VexLifeWebP1.BrowserQuery.identity.ref}"] [data-vex-generated-part="text-field"]{width:100%;height:36px;min-height:36px;resize:none;overflow:hidden;padding:8px 12px;background:var(--raised);border:1px solid var(--rule-strong);border-radius:8px;color:var(--ink);}
        [data-vex-ref="${VexLifeWebP1.AppSelector.identity.ref}"] [data-vex-generated-part="control"]{width:136px;height:36px;min-height:36px;padding:0 14px;background:var(--surface);border:1px solid var(--rule-strong);border-radius:8px;font-weight:600;text-align:left;}
        [data-vex-ref="${VexLifeWebP1.Utilities.identity.ref}"]{justify-self:end;align-self:stretch;display:flex;align-items:center;padding:0 14px;background:var(--sunken);border-left:1px solid var(--rule);}
        [data-vex-ref="${VexLifeWebP1.QuickActions.identity.ref}"]{display:flex;gap:10px;align-items:center;}
        [data-vex-ref="${VexLifeWebP1.VexSummon.identity.ref}"] [data-vex-generated-part="control"]{display:grid;place-items:center;width:42px;height:42px;min-width:42px;min-height:42px;padding:0;background:var(--surface);border:1px solid var(--rule);border-radius:50%;color:var(--ink-2);}

        [data-vex-ref="${VexLifeWebP1.Refs.BodyFrame.value}"]{background:var(--ground);}
        [data-vex-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"]{left:0;width:var(--vex-browser-share);background:var(--ground);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"]{left:var(--vex-browser-share);width:calc(100% - var(--vex-browser-share));background:var(--surface);}
        [data-vex-ref="${VexLifeWebP1.Browser.identity.ref}"]{height:100%;min-height:0;display:flex;flex-direction:column;}
        [data-vex-ref="${VexLifeWebP1.BrowserFrameStatus.identity.ref}"]{display:none;margin:0;padding:8px 14px;background:var(--paper);color:var(--paper-ink);border-bottom:1px solid var(--paper-rule);font-size:var(--step--1);}
        [data-vex-ref="${VexLifeWebP1.BrowserFrameStatus.identity.ref}"][data-vex-frame-state="loading"]{display:block;background:var(--sunken);color:var(--ink-2);border-bottom-color:var(--rule);}
        [data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}"]{position:relative;flex:1;min-height:0;overflow:hidden;background:var(--ground);}
        [data-vex-ref="${VexLifeWebP1.BrowserViewport.identity.ref}"] iframe{position:absolute;inset:0;width:100%;height:100%;border:0;background:#fff;}

        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]{left:calc(var(--vex-browser-share) - 6px);top:0;width:12px;height:100%;z-index:18;display:flex;align-items:center;justify-content:center;background:var(--surface);border-inline:1px solid var(--rule);cursor:col-resize;touch-action:none;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]::before{content:"⋮";color:var(--ink-3);font-size:18px;line-height:1;}
        [data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:hover::before,[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]:focus-visible::before{color:var(--ink);}

        [data-vex-ref="${VexLifeWebP1.Companion.identity.ref}"]{height:100%;min-height:0;display:flex;flex-direction:column;background:var(--surface);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionHeader.value}"]{display:flex;align-items:center;gap:10px;min-height:46px;padding:6px 14px;border-bottom:1px solid var(--rule);}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionHeader.value}"] [data-vex-generated-part="companion-title"]{margin:0;font-size:var(--step-1);font-weight:600;}
        [data-vex-ref="${VexLifeWebP1.CompanionStatus.identity.ref}"]{display:flex;align-items:baseline;gap:6px;min-width:0;color:var(--ink-2);font-size:var(--step--1);}
        [data-vex-ref="${VexLifeWebP1.CompanionStatus.identity.ref}"]::before{content:"";width:7px;height:7px;border-radius:50%;background:var(--ink-3);align-self:center;flex:none;}
        [data-vex-ref="${VexLifeWebP1.CompanionStatus.identity.ref}"][data-vex-ready="true"]::before{background:var(--ready);}
        [data-vex-ref="${VexLifeWebP1.CompanionViewSwitch.identity.ref}"]{margin-left:auto;}
        [data-vex-ref="${VexLifeWebP1.CompanionViewSwitch.identity.ref}"] [data-vex-generated-part="control"]{height:30px;min-height:30px;padding:0 12px;background:var(--surface);border:1px solid var(--rule-strong);border-radius:7px;font-size:var(--step--1);font-weight:600;}
        [data-vex-ref="${VexLifeWebP1.Refs.CompanionContent.value}"]{min-height:0;flex:1;display:block;overflow:hidden;}
        [data-vex-ref="${VexLifeWebP1.Conversation.identity.ref}"]{height:100%;min-height:0;display:flex;flex-direction:column;}
        [data-vex-ref="${VexLifeWebP1.Settings.identity.ref}"]{height:100%;min-height:0;overflow-y:auto;padding:4px 16px 18px;color:var(--ink);font-size:var(--step-0);}

        [data-vex-ref="${VexLifeWebP1.ConversationTimeline.identity.ref}"]{flex:1;min-height:0;overflow-y:auto;overscroll-behavior:contain;margin:0;padding:18px 10px 12px 16px;}
        [data-vex-generated-part="conversation-message"]{display:grid;grid-template-columns:minmax(0,1fr) 40px;grid-template-areas:"bubble index" "speaker index";margin:0 0 18px;}
        [data-vex-generated-part="conversation-index"]{grid-area:index;align-self:center;justify-self:end;color:var(--ink-3);font-size:var(--step--1);font-variant-numeric:tabular-nums;}
        [data-vex-generated-part="conversation-bubble"]{grid-area:bubble;position:relative;max-width:min(92%,640px);padding:10px 14px 11px;border-radius:12px;border:1px solid;overflow-wrap:anywhere;}
        [data-vex-generated-part="conversation-bubble"]::after{content:"";position:absolute;bottom:-7px;width:12px;height:12px;background:inherit;border:inherit;border-top:0;border-left:0;transform:rotate(45deg);}
        [data-vex-conversation-role="human"] [data-vex-generated-part="conversation-bubble"]{justify-self:end;background:var(--you);border-color:var(--you-rule);border-bottom-right-radius:4px;}
        [data-vex-conversation-role="human"] [data-vex-generated-part="conversation-bubble"]::after{right:14px;}
        [data-vex-conversation-role="vex"] [data-vex-generated-part="conversation-bubble"]{justify-self:start;background:var(--vex);border-color:var(--vex-rule);border-bottom-left-radius:4px;font-family:var(--voice);font-size:var(--step-1);line-height:1.55;}
        [data-vex-conversation-role="vex"] [data-vex-generated-part="conversation-bubble"]::after{left:14px;}
        [data-vex-generated-part="conversation-speaker"]{grid-area:speaker;margin:9px 2px 0;font-size:var(--step--1);color:var(--ink-2);font-weight:600;}
        [data-vex-conversation-role="human"] [data-vex-generated-part="conversation-speaker"]{justify-self:end;}
        [data-vex-conversation-role="vex"] [data-vex-generated-part="conversation-speaker"]{justify-self:start;}
        [data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}"]{display:flex;justify-content:center;}
        [data-vex-ref="${VexLifeWebP1.ConversationJumpLatest.identity.ref}"] [data-vex-generated-part="control"]{height:28px;min-height:28px;padding:0 10px;background:var(--surface);border:1px solid var(--rule-strong);border-radius:14px;color:var(--ink-2);font-size:var(--step--1);}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"]{min-height:20px;margin:0 14px 6px;color:var(--ink-2);font-size:var(--step--1);}
        [data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="error"],[data-vex-ref="${VexLifeWebP1.ResponseProgress.identity.ref}"][data-vex-progress-state="browser-error"]{color:var(--danger);}

        [data-vex-ref="${VexLifeWebP1.Composer.identity.ref}"]{display:grid;grid-template-columns:minmax(0,1fr) auto;grid-template-areas:"ticket ticket" "share share" "draft send";gap:8px 10px;padding:8px 14px 14px;border-top:1px solid var(--rule);background:var(--surface);}
        [data-vex-ref="${VexLifeWebP1.Composer.identity.ref}"]>[data-vex-generated-part="text-field"]{grid-area:draft;min-height:44px;max-height:220px;resize:none;padding:11px 18px;background:var(--raised);border:1px solid var(--rule-strong);border-radius:22px;line-height:1.45;}
        [data-vex-ref="${VexLifeWebP1.ContextReceipt.identity.ref}"]{grid-area:ticket;display:none;position:relative;padding:11px 14px 12px 18px;background:var(--paper);color:var(--paper-ink);border-radius:6px 10px 10px 6px;box-shadow:0 1px 0 var(--paper-rule),0 6px 18px rgba(44,39,30,.12);}
        [data-vex-ref="${VexLifeWebP1.ContextReceipt.identity.ref}"][data-vex-context-included="true"]{display:block;}
        [data-vex-ref="${VexLifeWebP1.ContextReceipt.identity.ref}"]::before{content:"";position:absolute;left:6px;top:8px;bottom:8px;border-left:2px dotted var(--paper-rule);}
        [data-vex-ref="${VexLifeWebP1.ContextShare.identity.ref}"]{grid-area:share;}
        [data-vex-ref="${VexLifeWebP1.ContextShare.identity.ref}"] [data-vex-generated-part="control"]{display:inline-flex;align-items:center;height:28px;min-height:28px;padding:0 10px;background:transparent;border:1px solid var(--rule-strong);border-radius:14px;color:var(--ink-2);font-size:var(--step--1);font-weight:600;}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"]{grid-area:send;}
        [data-vex-ref="${VexLifeWebP1.Send.identity.ref}"] [data-vex-generated-part="control"]{height:44px;min-height:44px;padding:0 20px;border-radius:9px;background:var(--ink);color:var(--surface);border:1px solid var(--ink);font-weight:700;}
        [data-vex-ref="${VexLifeWebP1.Stop.identity.ref}"]{display:none;}
        [data-vex-ref="${VexLifeWebP1.ContextInspector.identity.ref}"]{display:none;}

        [data-vex-ref="${VexLifeWebP1.SettingsContext.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsResources.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsHome.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsContinuity.identity.ref}"],[data-vex-ref="${VexLifeWebP1.SettingsReset.identity.ref}"]{padding:14px 0;border-bottom:1px solid var(--rule);}
        [data-vex-ref="${VexLifeWebP1.SettingsReset.identity.ref}"]{border-bottom:0;padding-top:18px;}
        [data-vex-generated-part="settings-heading"]{margin:0 0 9px;font-size:var(--step-0);font-weight:650;}
        [data-vex-ref="${VexLifeWebP1.ContextWindow.identity.ref}"],[data-vex-ref="${VexLifeWebP1.MemoryResource.identity.ref}"]{position:relative;display:flex;height:26px;overflow:hidden;border:1px solid var(--rule-strong);border-radius:6px;background:var(--sunken);}
        [data-vex-generated-part="meter-fill"]{height:100%;background:var(--remaining);}
        [data-vex-generated-part="meter-text"]{position:absolute;inset:0;display:grid;place-items:center;font-size:var(--step--1);font-weight:600;color:#1b1e22;font-variant-numeric:tabular-nums;}
        [data-vex-ref="${VexLifeWebP1.ThoughtVisibility.identity.ref}"],[data-vex-ref="${VexLifeWebP1.LowMemoryAlert.identity.ref}"],[data-vex-ref="${VexLifeWebP1.HomeDirectories.identity.ref}"],[data-vex-ref="${VexLifeWebP1.HomeAddDirectory.identity.ref}"],[data-vex-ref="${VexLifeWebP1.ContinuityMode.identity.ref}"],[data-vex-ref="${VexLifeWebP1.DreamSync.identity.ref}"]{margin-top:10px;}
        [data-vex-ref="${VexLifeWebP1.ThoughtVisibility.identity.ref}"] [data-vex-generated-part="control"],[data-vex-ref="${VexLifeWebP1.LowMemoryAlert.identity.ref}"] [data-vex-generated-part="control"],[data-vex-ref="${VexLifeWebP1.HomeDirectories.identity.ref}"] [data-vex-generated-part="control"],[data-vex-ref="${VexLifeWebP1.HomeAddDirectory.identity.ref}"] [data-vex-generated-part="control"],[data-vex-ref="${VexLifeWebP1.ContinuityMode.identity.ref}"] [data-vex-generated-part="control"],[data-vex-ref="${VexLifeWebP1.DreamSync.identity.ref}"] [data-vex-generated-part="control"]{height:34px;min-height:34px;padding:0 12px;background:var(--surface);border:1px solid var(--rule-strong);border-radius:7px;color:var(--ink-2);font-size:var(--step--1);font-weight:600;}
        [data-vex-ref="${VexLifeWebP1.CleanChat.identity.ref}"] [data-vex-generated-part="control"]{width:100%;height:42px;min-height:42px;background:var(--danger);color:#fff;border:1px solid var(--danger);border-radius:7px;font-weight:700;}
        [data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] button:disabled{cursor:default;opacity:.5;}

        @media(max-width:860px){[data-vex-ref="${VexLifeWebP1.Header.identity.ref}"]{grid-template-columns:auto minmax(0,1fr) auto;padding:10px 12px;row-gap:10px;}[data-vex-ref="${VexLifeWebP1.Brand.identity.ref}"] [data-vex-generated-part="brand-name"]{display:none;}[data-vex-ref="${VexLifeWebP1.Utilities.identity.ref}"]{grid-column:1/-1;justify-self:stretch;padding:0;background:none;border:0;overflow-x:auto;}[data-vex-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"]{left:0;top:0;width:100%;height:34%;}[data-vex-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"]{left:0;top:34%;width:100%;height:66%;}[data-vex-ref="${VexLifeWebP1.BrowserCompanionHandle.identity.ref}"]{display:none;}}
        @media(prefers-reduced-motion:reduce){[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *::before,[data-vex-screen="${VexLifeWebP1.Surface.screenRef}"] *::after{animation:none!important;transition:none!important;scroll-behavior:auto!important;}}
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
