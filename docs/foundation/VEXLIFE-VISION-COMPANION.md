# VexLife Vision Companion

`[VXG RealForever]`

## Human question

Can VexLife expose one truthful Vision relationship beside Conversation while keeping product meaning,
screen permission, physical capture, outside observation, model interpretation and durable Memory as
separate owners?

Current product decision:

~~~text
accepted VexLife-Prod source includes:
  PR #77 local-qualified Vision private-image carriage
  PR #84 explicit local user-selected Vision != distribution-gate rule
  PR #86 Product local-service bridge to the carried Vision runtime
  PR #90 dynamic Product Vision presentation binding
  this source edge: exact Product artifact-contract currentization to the accepted SDK Safety source

AND

accepted Vextreme-SDK source already owns:
USER_SELECTED_SESSION system-picker capture
-> canonical Security active-sharing witness
-> content-absent active VISUAL observation

BUT

SDK_SOURCE_ACCEPTED != PROD_EFFECT_REACHABLE
ACCEPTED_COMPANION_MAIN != VISION_RUNTIME_BRIDGE
~~~

The current VexLife-Prod Vision slice therefore composes over the accepted lived Companion body while
remaining a truthful semantic/control surface. The SDK runtime artifact, Product private-image
carriage and ordinary local-service bridge are accepted. The presentation consumes Product Vision
reachability dynamically from the local service rather than hardcoding Ready or Held.

## Current semantic owner

`VexLifeVisionFamily` owns the human-facing Vision relationship, and it now speaks the persistent
selected-scope relationship rather than the retired one-shot Look Now:

~~~text
State
  Held(RouteUnreachable | RuntimeUnreadable | ScreenSafetyNotCurrent)
  Ready        no window chosen; Choose a Window… is lawful
  Selecting    the macOS picker is open; nothing is lawful until it settles
  Engaged      one window chosen; Release Window is lawful; nothing is captured
  Looking      SDK-reported only; Product admits no Control here

Control
  Engage
  Stop

Effect
  EngageScope
  StopScope
~~~

`observe(routeReachable, runtimeReadable, scopeState)` is the only path from SDK projection to Family
State. An unknown SDK state is `Held(RuntimeUnreadable)`, never a guess. `ENDED` (and no projection
yet) is `Ready`, because an ended scope is a fresh opportunity to choose again. A Safety hold still
admits `Stop`, so a person can always release a window they chose.

Service truth:

~~~text
GET /api/v1/vision
  -> PROJECT, then Family.observe
  -> vision-projection/v2 { state, routeReachable, display, effectsPerformed=false }

POST /api/v1/vision/control { controlRef, requestRef }
  -> re-observe
  -> Family.act(observed, control)
       null            -> 409 NOT_LAWFUL_NOW   (runtime never called)
       EngageScope     -> SDK ENGAGE
       StopScope       -> SDK STOP
  -> runtime ok=false  -> 200 RUNTIME_REFUSED  (SDK error code carried)
  -> otherwise         -> 200 REALIZED
  -> every result carries a fresh re-observation, never a predicted successor

GET /api/v1/vision/scope, POST /api/v1/vision/scope/{engage,stop}
  -> the underlying selected-scope coordinates, kept for diagnostics and the SDK contract

POST /api/v1/vision/look-now
  -> HELD -> VISION_LOOK_NOT_YET_ADMITTED_FROM_PERSISTENT_SCOPE
~~~

The display copy is part of the Family's `DisplayState`, so the renderer never decides what Vision
means. The Engaged detail tells the person plainly that nothing is captured and Vex can't look at the
window yet: choosing a window is a relationship, not a camera.

Permanent:

~~~text
READY != SCREEN_CAPTURE_AUTHORIZED
SCOPE_ENGAGED != SCREEN_CAPTURE_ACTIVE
SCOPE_ENGAGED != LOOKING
LOOKING != SCREEN_CAPTURED
CONVERSATION_TURN != AUTOMATIC_SCREENSHOT
ACTIVE_VISUAL_OBSERVATION != VISUAL_MODEL_INPUT
OBSERVATION != MEMORY
~~~


## Accepted outside effect owners

The Product artifact contract in this source binds the fresh local-qualified **persistent selected-scope**
runtime artifact:

~~~text
SDK commit     = 1c2c9d76114ca35f00125f0a5c93e531e3e6a746
SDK tree       = 6376f68279f0750ee6d3868b8472368199763064
archive        = vexlife-vision-runtime-darwin-arm64-node-v26.8.1-sdk-1c2c9d76.tar.xz
archiveSha256  = f057f551099a90088fcd5ccc600391090810272cc8d3ed3e0af560e236189709
manifestSha256 = e3c1279a32df87bb93fb2e5952f9c9faa184dfd9c3804470635bf773055d71be
runtimeSha256  = 4175cb52bf45b495632c570796ef3f31c0cf40ed277b415929f8f8daf1e98461
helperSha256   = 414226b84629a390d525efd54dcc184b974725f9f74c37c2dfe4492ab1e4a79b
Node           = v26.8.1
truthClass     = LOCAL_QUALIFIED_NOT_DISTRIBUTABLE
release        = LOCAL_ONLY_NO_RELEASE
~~~

This is the A006 rebuild, bound as the successor to the original A002 artifact. The A002 archive and
its builder bytes were searched for and are gone (#114 A005, A007), so a contract naming them could never
be satisfied and Vision could never be carried. A006 was built from the same accepted SDK commit and tree
with the same Node archive, and qualified only:

~~~text
PROJECT
-> SHUTDOWN
~~~

Its bytes differ from A002 because the builder was reconstructed and these builds are not byte-
reproducible; the source the runtime and helper come from is identical. The successor binding is
recorded here rather than treated as A002 recovery:

~~~text
A006_REBUILT_FROM_ACCEPTED_SOURCE
= current Product artifact contract
!= A002_BYTES_RECOVERED
!= LIVED_ENGAGE_PROVEN
~~~

The image build still fails closed: an archive whose name or SHA-256 differs from this contract is
rejected, and an image built without one carries no Vision runtime, so the service projects Vision as
`Held(RouteUnreachable)`.

No ENGAGE, picker presentation, capture, Product mutation, visual-model input, Memory, training or
publication occurred during artifact qualification.

The accepted persistent outside relationship is:

~~~text
Product service
-> one long-lived carried vision-runtime child
-> Process Factory persistent selected-scope Product runtime
-> Process Factory host custody
-> VexLifeVisionSelectedScopeAgent
-> SCContentSharingPicker SINGLE_WINDOW
-> retained provider scope / retained SCStream object
-> NO stream start merely while ENGAGED
~~~

Rightful ownership remains separate:

~~~text
Process Factory selected-scope session
  owns selected-scope semantic lifecycle

Process Factory host custody
  owns native child/process lifetime

native selected-scope host
  owns retained provider/filter/SCStream objects

Vex Safety
  owns current SCREEN authorization/currentness

VexLife-Prod
  owns only one carried runtime child + command correlation + bounded projection validation

VexLifeVisionFamily
  still owns the current human-facing Product semantic surface
~~~

The historical one-shot USER_SELECTED_SESSION capture path remains accepted lineage/evidence, but it is
not the protocol spoken by the current persistent artifact.


## Current Product realization state

This source candidate binds the new persistent artifact and replaces the old one-process-per-LookNow
bridge with one service-lifetime child relationship:

~~~text
VexLife-Prod service starts
-> validate exact carried vision-runtime executable
-> spawn one persistent child with no semantic CLI arguments
-> no picker/capture merely from process start

Product scope routes
-> newline-delimited commandRef-bound JSON
-> PROJECT / ENGAGE / STOP

service close
-> SHUTDOWN
-> join/terminate owned child
~~~

The Product bridge validates the exact SDK content-absent selected-scope projection before returning it.
Provider/window/native identifiers, helper paths, child handles and raw image data do not enter the
Product contract.

The current Product source deliberately does **not** claim that the running installed service has
already rotated to this artifact. Source acceptance, private-image assembly, installation/service
rotation, and lived ENGAGE proof remain separate predicates.

Permanent:

~~~text
PERSISTENT_ARTIFACT_CONTRACT_CURRENT
!= RUNNING_PRODUCT_ROTATED_TO_ARTIFACT

PRODUCT_SERVICE_CHILD_SOURCE_ACCEPTED
!= RUNNING_PRODUCT_VISION_SCOPE_CURRENT
!= WEB_PERSISTENT_SCOPE_PRESENTATION_CURRENT

PRODUCT_SERVICE_CHILD
!= SELECTED_SCOPE_SESSION_OWNER
!= NATIVE_PROVIDER_OWNER
!= LOOK_EXECUTOR
!= DISTRIBUTION WORK
~~~

No Developer ID, notarization, App Store or public-release work is required for this local-only Product
bridge.


## Local user-selected Vision is not a distribution gate

The accepted macOS Product route is the SDK-owned user-selected session path:

~~~text
USER_SELECTED_SESSION
-> SCContentSharingPicker
-> SINGLE_WINDOW
-> at most one user-directed snapshot
~~~

Its accepted contract explicitly preserves:

~~~text
persistentHostPermissionRequired=false
stableSignerRequiredForSession=false
~~~

Therefore local VexVision capability must not be gated on public-distribution trust:

~~~text
LOCAL_USER_SELECTED_VISION
!= DEVELOPER_ID_SIGNING_REQUIRED
!= NOTARIZATION_REQUIRED
!= APP_STORE_DISTRIBUTION_REQUIRED
!= GITHUB_RELEASE_REQUIRED

LOCAL_ONLY_NO_RELEASE
!= LOCAL_EXECUTION_FORBIDDEN
~~~

A person may run the local/private VexLife-Prod route and use the accepted system-picker relationship
with the permissions and selection macOS asks that person to grant for that session. That local
user/OS consent remains real authority; it is not replaced by repository source, Product state or an
Apple distribution account.

Developer ID signing, notarization and public release are separate distribution/persistent-package
concerns. The accepted stable-signer permission agent also remains a separate route for persistent
host-permission semantics. Neither route is a prerequisite merely to let a person choose one window
through the accepted `USER_SELECTED_SESSION` relationship.

Permanent routing rule for future Product work:

~~~text
PRODUCT_VISION_LOCAL_REACHABILITY
-> current Safety
-> accepted local Product runtime
-> USER_SELECTED_SESSION
-> OS-owned picker / person selection

PRODUCT_VISION_LOCAL_REACHABILITY
!= ONB-DIST
!= DEVELOPER_ID
!= NOTARIZATION
~~~

Do not send ordinary local VexVision enablement back through signing/notarization/distribution work
unless the actual selected outcome is public distribution or persistent stable-signer semantics.

## No premature Store or model input

The accepted SCREEN observation remains content-absent.

~~~text
ACTIVE_VISUAL_OBSERVATION != IMAGE_BYTES
VISUAL_OBSERVATION_REF != IMAGE_BYTES
RAW_FRAME_OBSERVED != STORE_REQUIRED
OBSERVATION != MEMORY
OBSERVATION != TRAINING
~~~

The accepted system-picker helper deliberately lets raw frame bytes leave scope after bounded
digest/dimension formation. Therefore accepted capture/observation truth can later currentize Product
state, but it cannot by itself make Vex interpret the image.

A future visual interpretation edge must separately earn:

~~~text
fresh Look Now capture
-> bounded ephemeral image resource custody
-> exact authorized local visual-model input
-> derived visual meaning
-> raw resource discarded
~~~

No Vision Store is earned merely for symmetry.

## Product manifestation

The current Blueprint keeps the stable Companion Vision coordinate and adds one quick-access projection:

~~~text
element.vexlife-workspace.companion.vision
element.vexlife-workspace.header.utilities.quick-actions.vision
~~~

These are two presentations of one Vision relationship, not two Vision owners. Both render the same
Product `DisplayState`: the Companion header shows its label and detail, and the utility-row eye shows
its primary label and indicator (`NONE`, `SCOPE`, `LOOKING`). Static markup starts Held/disabled until the
live projection loads. When the projection names no primary Control, both coordinates are disabled;
otherwise both POST that exact `controlRef` to `/api/v1/vision/control` and render the re-observed
projection the service returns.

This is dynamic Product capability truth, not a source-hardcoded availability promise.

## Reachable and still held effects

Once this source candidate is accepted **and** a Product image carrying the exact persistent artifact is
installed/running, the ordinary Product service may reach only the selected-scope effects admitted here:

~~~text
Product Vision route READY
+ explicit POST /api/v1/vision/scope/engage
-> accepted SDK ENGAGE
-> macOS system picker
-> one user-selected window
-> persistent selected scope
-> no capture

GET /api/v1/vision/scope
-> PROJECT current content-absent scope truth

POST /api/v1/vision/scope/stop
-> release current selected scope
~~~

Still held:

~~~text
LOOK execution
screen capture from persistent scope
visual-model input
raw-frame persistence
automatic screenshot on Conversation turns
continuous/background capture
browser control
Memory
training
publication
~~~

The legacy `/api/v1/vision/look-now` coordinate is intentionally held, and no presentation offers Look
Now. A later Vex-access LOOK edge must separately earn one fresh capture from the already-engaged
selected scope, then ephemeral visual interpretation and Conversation Context composition; when it
does, it arrives as a new Family Control, not as a renderer button.

<!-- [VXG RealForever][VEXLIFE-PROD][VISION-COMPANION][PERSISTENT-SCOPE-SERVICE] -->
