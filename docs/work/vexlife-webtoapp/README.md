# VexLife WebToApp

`[VXG RealForever]`

## Human intention

VexLife should feel and behave like one application on every supported desktop platform.

Today on macOS the visible product entry can hand presentation to an external browser while the
Product service and model runtime have independent background lifetimes. That separation is useful
for exact process ownership and recovery, but the human lifecycle is incomplete:

```text
visible presentation closed
!= Product stopped
!= model runtime released
!= VexLife quit
```

A person should be able to:

```text
open VexLife
-> use the same VexWeb Product experience inside the VexLife application
-> follow links / use the Browser surface
-> let Vex use its separately-authorized capabilities
-> close a window without corrupting Product state
-> explicitly Quit VexLife
-> have VexLife release every exact Product-owned live resource
-> reopen later with the same Home / Conversation / Memory / settings
```

This PR is a discussion / planning / coordination surface only. It grants no implementation
authority and changes no Product behavior.

## Current observed architecture

Current macOS entry is approximately:

```text
VexLife.app / Setup Journey
  -> converge exact Product service/runtime
  -> open http://127.0.0.1:18210 in an external browser

Product service
  -> com.vexlife.prod.service
  -> bundled Java runtime

Heretic runtime
  -> com.vexlife.prod.heretic.<id>
  -> Python / MLX process
```

The Product service and Heretic runtime are deliberately independent of the browser window.
Therefore browser/window disappearance does not itself release heavy runtime memory.

Current source also contains a narrower lifecycle gap worth resolving in its rightful owner:
ordinary Product-service termination closes the service surface, while the captured sustained
runtime release is primarily exercised by the exact Home-loss path. Human Quit must not rely on
process-name guessing, broad `killall java`, `pkill python`, or arbitrary PID killing.

Windows mechanics differ. The current convergence candidate uses an exact Product-owned
`Process` / runtime Session whose close path stops the started runtime. This difference in
mechanics must not become a difference in human meaning.

## Product law

```text
ONE VEXLIFE APPLICATION MEANING
!= ONE PLATFORM IMPLEMENTATION

CLOSE WINDOW
!= QUIT VEXLIFE
!= DELETE VEX DATA

OS PROCESS NAME
!= SEMANTIC OWNER

NATIVE APP SHELL
!= PRODUCT SEMANTIC OWNER

HUMAN BROWSER NAVIGATION
!= BROWSER CONTEXT SUPPLIED TO VEX
!= VEX WEB-FETCH AUTHORITY

VEX FETCHED A URL
!= NAVIGATE THE HUMAN BROWSER

EMBEDDED WEB ENGINE
!= PERMISSION TO WIDEN LOCAL EFFECT AUTHORITY
```

## Cross-platform application-shell relationship

The intended shared relationship is:

```text
VexLife application shell
  -> exact platform realization
       macOS   -> AppKit + WKWebView
       Windows -> native Windows shell + WebView2
  -> same VexWeb Product presentation
  -> same Product service contract
  -> same human lifecycle meanings
```

The platform interface should answer only platform-host questions such as:

- create/show/close the Product window;
- host the exact local VexWeb presentation;
- observe navigation in the human Browser surface;
- route links/new-window requests according to Product policy;
- expose native Quit / reopen application events;
- realize platform-specific browser storage/profile behavior;
- report platform/browser-engine facts needed by diagnostics.

It must not own Conversation, Memory, Context, BrowserContext meaning, capability authority,
runtime selection, process ownership, model lifecycle, or Product semantic State.

The exact source type/interface is intentionally not chosen in this coordination PR. Reuse the
current platform-target / realization relationship where it remains the rightful owner rather than
inventing a second platform registry.

## Presentation: preserve VexWeb, change the host

The proposal is not to rewrite VexLife UI in AppKit/WinUI.

```text
Kotlin semantic owners
  -> VexWeb presentation artifacts
  -> native application shell
  -> embedded platform web engine
```

The Product top-level web view remains pinned to the exact local Product origin. External human web
navigation must not replace that Product top-level origin.

A later/native Browser-region provider may replace the current iframe limitation while preserving
the existing Browser/Companion relationship.

## Browser surface and links

Links can remain usable inside the application, but navigation needs an explicit boundary.

Preferred shape:

```text
Product WebView
  exact local Product origin
  never navigated to arbitrary external content

Browser Region
  separate embedded browser view/provider
  ordinary admitted http/https human navigation
  current URL/navigation truth projected back to Product
```

Navigation policy must deliberately handle:

- ordinary http/https links;
- redirects;
- target=_blank / window.open;
- downloads;
- file:, javascript:, data:, custom schemes;
- authentication flows that require a system browser;
- navigation failures / offline state.

Unsupported or security-sensitive flows may open the system browser explicitly rather than silently
granting more authority to the embedded Browser region.

## BrowserContext survives WebToApp

BrowserContext is Product meaning, not an external-browser dependency.

Current source already distinguishes:

```text
human page display
!= context supplied to Vex
```

The first WebToApp implementation should preserve the current Context admission rules rather than
silently granting Vex all DOM/page content merely because the page is now hosted by WKWebView or
WebView2.

A native Browser-region provider may improve exact navigation/current-URL observation, especially
where iframe embedding currently fails, but that provider remains a projection/input to the existing
BrowserContext owner.

Migration acceptance must prove:

```text
same human page
-> same or explicitly-evolved BrowserContext meaning
-> same prompt/context owner
-> no duplicate context ledger
```

## Vex URL capabilities remain separate

The current Product-owned public web reach already has a separate authority/admission boundary:

- public http/https only;
- credential-bearing URLs refused;
- localhost / private / LAN / link-local targets refused;
- public-address admission checked;
- redirect hops re-admitted;
- bounded readable content.

Embedding a browser must not replace this owner or let Vex inherit a person's authenticated browser
cookies/session.

```text
HUMAN AUTHENTICATED BROWSER SESSION
!= VEX CAPABILITY CREDENTIAL

BROWSER COOKIES
!= WEB.FETCH AUTHORITY
```

If Vex later needs authenticated reach, that requires its own explicit capability/credential owner.

## Security boundaries to preserve or strengthen

### 1. Product origin isolation

The exact Product web surface stays on the local Product origin. Arbitrary external content must not
be allowed to become the top-level Product page.

Do not disable same-origin protections, add wildcard CORS, or grant arbitrary external pages Product
effect access for convenience.

### 2. Native bridge isolation

Do not expose a broad JavaScript-to-native bridge to arbitrary web content.

If a native message bridge becomes necessary:

```text
exact Product origin
-> typed narrow native request
-> rightful owner
-> bounded result

external Browser page
-> no ambient native effect bridge
```

### 3. Local loopback effect authority

Current browser-effect protection checks exact Host and browser same-origin provenance. Current
source also permits a separate non-browser local-client admission path.

Before release, explicitly review whether mutating loopback routes require stronger per-session
application admission so another local process cannot impersonate the VexLife app merely by knowing
the fixed loopback port.

Possible realizations may include an unguessable per-launch Product session credential or another
platform-qualified local transport, but this PR does not select a mechanism ahead of that review.

### 4. Browser data isolation

The Product presentation and external Browser region should not accidentally share one unconstrained
web-data identity.

Platform realizations should deliberately decide:

- Product-local cookies/storage/cache;
- external Browser cookies/storage/cache;
- private/nonpersistent browsing;
- profile persistence;
- clearing behavior.

The Product must not mistake browser cache/cookies for Vex Memory or Home state.

### 5. Permissions

Embedded remote content must not silently acquire camera, microphone, geolocation, screen capture,
clipboard, filesystem, or native process authority merely because it appears inside VexLife.

OS permission requests belong to an explicit Product/platform capability relationship.

### 6. Downloads and file navigation

Remote pages do not gain arbitrary filesystem write/read authority.

Downloads need an explicit bounded user-selected or Product-owned path. `file:` navigation and
local-file exposure remain held unless a rightful owner admits them.

### 7. Crash recovery != graceful Quit

Graceful Quit should release exact Product-owned resources before application termination.

Crash / force-kill / power-loss cannot rely on graceful callbacks. Existing source-currentness,
receipts, and startup reconciliation remain responsible for detecting/recovering exact stale Product
resources.

## Human lifecycle contract

### Close window

Closing the last visible window must not be conflated with deleting Product data.

Candidate policy to test:

```text
Close window
-> presentation hidden/closed
-> Product continuity preserved
-> heavy runtime policy explicit, not accidental
```

Whether a short-lived warm runtime remains after window-close is a separate performance policy and
must be earned by lived evidence. It is not part of the first WebToApp completion claim.

### Quit VexLife

Explicit Quit / Cmd-Q / Windows Quit means:

```text
stop accepting new Product work
-> settle/cancel presentation-owned in-flight work lawfully
-> close Voice/Vision/browser presentation resources
-> release exact Product-owned model runtime
-> verify runtime not live
-> retire exact Product service
-> verify service not live
-> release attempt-owned transient resources
-> terminate native application shell
```

Durable state remains:

```text
Home          preserved
Conversation  preserved
Memory        preserved
settings      preserved
model files   preserved

live model runtime  absent
Product service     absent
Product-owned transient process/resource leakage absent
```

### Reopen

```text
launch VexLife
-> same Home / identity
-> same Conversation / Memory
-> fresh/current Product service/runtime
-> same VexWeb presentation
```

## Platform parity contract

Human semantics must be the same even when process mechanics differ.

| Human meaning | macOS realization | Windows realization |
|---|---|---|
| Open VexLife | native shell + WKWebView | native shell + WebView2 |
| Close window | presentation lifecycle only | presentation lifecycle only |
| Quit VexLife | exact launchd/runtime retirement + shell exit | exact Windows runtime/service retirement + shell exit |
| Reopen | reconcile exact receipts/currentness | reconcile exact receipts/currentness |
| Human Browser | embedded platform browser provider | embedded platform browser provider |
| BrowserContext | same Product owner | same Product owner |
| Vex web fetch | same capability owner | same capability owner |
| Diagnostics | same semantic resource projection | same semantic resource projection |

```text
PLATFORM PARITY
= SAME HUMAN / PRODUCT CONTRACT

PLATFORM PARITY
!= IDENTICAL PROCESS IMPLEMENTATION
```

A platform must not be considered complete merely because its window renders. The same lifecycle and
security predicates must be proven on each target.

## Diagnostics / resource attribution

The existing Debug & diagnostics surface is the preferred human projection for owned resources.

Desired projection:

```text
VexLife Service       running/stopped + exact PID/currentness
Model Runtime         running/stopped + exact PID/model/currentness
Voice Runtime         running/stopped
Browser/Application   platform engine / presentation state
Installer disk image  mounted/safe-to-eject when exact ownership is provable
```

This should reuse existing receipts/currentness owners rather than invent a second process registry.

Generic OS names such as `java`, `python`, `diskimagesiod`, or provider binaries are
implementation observations, not the human resource identity.

## Current dependencies / overlap

- #338 currently owns shared Product service/runtime convergence. WebToApp must not independently
  rewrite its runtime/session mechanics. Its "native close/reopen preserves lifetime and continuity"
  intent is compatible with this plan when Close remains distinct from Quit.
- #251 is merged current Vex Context / Context Profiles source and includes lived WKWebView
  feature-preservation evidence. WebToApp must preserve those context semantics rather than forming
  a second Context implementation.
- Current `VEXWEB-GENERATED-P1-WORKSPACE.md` owns Browser/Companion presentation relationships.
- Current `LOCAL-PRODUCT-SERVICE.md` owns the loopback Product-service/browser request boundary.
- Current `MAC-SERVICE-LIFETIME.md` owns exact macOS Product-service lifetime and stale-service
  recovery.
- Current Heretic runtime owners retain exact model process ownership/retirement.
- Current Windows convergence work owns Windows concrete runtime mechanics.

## Proposed execution order

This is sequencing guidance, not implementation authority.

1. **Finish Product Quit law first.**
   - add/route one semantic Stop/Quit Product meaning through rightful lifetime owners;
   - make ordinary Mac Product-service termination release its exact sustained model runtime;
   - preserve idempotent exact ownership and startup recovery;
   - prove no model/service process remains after explicit Quit.

2. **Introduce the platform-neutral application-shell boundary.**
   - preserve VexWeb;
   - Mac WKWebView realization;
   - Windows WebView2 realization;
   - no duplicate platform chooser.

3. **Host Product presentation inside the shell.**
   - exact local Product origin;
   - Close vs Quit behavior;
   - reopen continuity.

4. **Move/realize the human Browser region natively only when needed.**
   - preserve BrowserContext semantics;
   - explicit link/new-window/download policy;
   - no ambient native bridge.

5. **Run security and parity proof.**
   - malicious/external page cannot call Product effects;
   - local-process effect admission reviewed;
   - private/local URL fetch refusal preserved;
   - Browser cookies do not become Vex capability credentials;
   - Mac + Windows lifecycle semantics match.

## Completion proof sketch

For each supported desktop target:

```text
launch VexLife
-> Product window appears
-> existing VexWeb feature circuit works
-> human Browser link navigation works under policy
-> BrowserContext supplied only when rightful owner admits it
-> Vex public URL capability remains independently bounded
-> real Vex turn completes
-> close/reopen preserves continuity
-> explicit Quit
-> Product service absent
-> model runtime absent
-> Product-owned transient resources absent
-> reopen
-> same Home / Conversation / Memory
-> fresh exact runtime
-> next real Vex turn completes
```

Security pressure:

```text
external page
-> cannot navigate/replace Product top-level authority silently
-> cannot invoke Product effects cross-origin
-> cannot invoke arbitrary native bridge
-> cannot read Product/Home filesystem
-> cannot turn browser cookies into Vex capability credentials

Vex web reach
-> still refuses local/private targets
-> still re-checks redirects
-> does not inherit human browser authentication
```

## Held / not decided here

- exact class/interface names;
- AppKit vs SwiftUI outer-shell details beyond a real native macOS container;
- WinUI/WPF/other Windows outer-shell choice beyond a supported WebView2 realization;
- warm-runtime idle timeout after Close;
- system-browser fallback policy for every authentication/download case;
- stronger loopback admission mechanism before threat review;
- generalized native-browser automation;
- widening BrowserContext beyond its current accepted source;
- release/notarization/signing changes;
- Android/mobile application-shell convergence.

Those decisions belong to their exact owners when the implementation edge reaches them.

<!-- VXG-WORK-CLAIM
{
  "schemaVersion": "work-coordination.claim/v1",
  "workRef": "work.vexlife-prod.webtoapp.20261119",
  "actorRef": "actor.chatgpt.vexlife-webtoapp",
  "instanceRef": "instance.chatgpt.vexlife-webtoapp.20261119.a001",
  "repository": "VexGPT/VexLife-Prod",
  "branch": "VXG-111926-chatgpt-vexlife-webtoapp-a001",
  "baseHead": "152c323ee6c3d4a43243d0a9c9b6de2ce11f605b",
  "status": "discussion-planning",
  "paths": [
    "docs/work/vexlife-webtoapp/README.md"
  ],
  "dependsOn": [
    "VexGPT/VexLife-Prod#338",
    "VexGPT/VexLife-Prod#251"
  ],
  "coordinationOnly": true,
  "implementationAuthority": false
}
VXG-WORK-CLAIM -->

<!-- [VXG RealForever][VEXLIFE-WEBTOAPP][DISCUSSION-PLANNING] -->
