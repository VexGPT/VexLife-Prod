# macOS normal Browser provider probe

[VXG RealForever]

## Why this exists

This directory is an **isolated provider proof** for
`work.vexlife.new-beginnings.normal-browser-provider`.

The human intention is narrower than "make a new Mac app":

```text
VexLife Browser semantic identity
        ↓
one real browser provider
        ↓
ordinary HTTP(S) site/session/navigation behavior
```

The current generated web presentation is useful but cannot truthfully provide a complete browser
relationship because an HTML iframe can be refused by sites and cannot give its cross-origin parent
exact current URL/back-forward ownership after arbitrary child navigation.

This probe asks whether native macOS WebKit can supply that missing provider relationship **without**
pulling Conversation, Vision, model/runtime, Home, setup, or product lifecycle into the proof.

## Boundary

```text
THIS PROBE
= provider evidence

THIS PROBE
!= VexLife product presentation
!= VexWeb authoring authority
!= Mac front door
!= setup/lifecycle owner
!= Conversation client
!= Vision sensor
!= Vex context provider
!= local Product-service client
```

It deliberately lives under `exploration/`. Nothing in this directory is installed, launched by
VexLife setup, or treated as durable product process state.

## Source shape

- `BrowserProviderProbe.swift` — one native AppKit window containing one `WKWebView`.
- `run-macos-probe.sh` — proof-only Mac compiler/launcher. It builds into `${TMPDIR}`, not the
  repository or Product Home.

The probe owns only:
- a human-entered HTTP(S) URL;
- native WebKit navigation;
- current URL projection;
- Back / Forward / Reload controls;
- simple stdout observation lines for lived proof.

The initial page is the current Vextreme Terrain practicum.

## macOS proof command

From repository root:

```bash
exploration/macos-browser-provider/run-macos-probe.sh
```

An optional public HTTP(S) URL may be supplied as the first argument.

The proof launcher requires the Mac's existing Xcode Command Line Tools / Swift compiler. It does not
install a compiler or mutate VexLife setup.

## Observation lines

The process emits tab-separated observation lines such as:

```text
VEXLIFE_BROWSER_PROBE_READY	<url>
VEXLIFE_BROWSER_PROBE_NAVIGATION	COMMIT	<url>	canGoBack=<true|false>	canGoForward=<true|false>
VEXLIFE_BROWSER_PROBE_NAVIGATION	FINISH	<url>	canGoBack=<true|false>	canGoForward=<true|false>
VEXLIFE_BROWSER_PROBE_VEXTREME_CHROME	{"present":true,"links":[...]}
VEXLIFE_BROWSER_PROBE_FAILURE	<phase>	<message>
```

These lines are proof observations only. They are not a new receipt schema or durable runtime store.

## First lived questions

The first Mac proof should answer, in order:

1. Does Vextreme load at its real `https://vgong24.github.io/...` URL, and does the probe observe
   the canonical `#vex-site-nav` family shell and its Vextreme / VexLife / VexWork links?
2. Do ordinary page links navigate normally?
3. Does the native provider expose the exact current URL after navigation?
4. Do Back / Forward / Reload follow the same browsing history?
5. Can a page that refuses ordinary iframe embedding render here?
6. Does all of this happen with **zero** Conversation, Vision, model, Memory, Home, setup, or local
   Product-service effects?

Only after those questions have lived evidence should this provider be considered for connection to
the accepted BrowserViewport identity or Mac product lifecycle.

## Provider-selection boundary

WKWebView is the current **proof candidate**, not accepted product architecture.

A successful proof earns a provider-adoption decision; it does not by itself earn:
- a new long-lived VexLife presentation process;
- packaging;
- launchd ownership;
- Electron/JCEF/WebKit permanence;
- browser-to-Vex context coupling.

<!-- [VXG RealForever][VEXLIFE-PROD][EXPLORATION][MACOS-BROWSER-PROVIDER] -->
