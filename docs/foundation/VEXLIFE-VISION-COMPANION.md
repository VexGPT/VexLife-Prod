# VexLife Vision Companion

`[VXG RealForever]`

## Human question

Can VexLife expose one truthful Vision relationship beside Conversation while keeping product meaning,
screen permission, physical capture, outside observation, model interpretation and durable Memory as
separate owners?

Current product decision:

~~~text
PR #53 presentation is stable enough to begin the Vision product slice now
while
Vextreme-SDK continues independently toward real screen permission / capture
~~~

## Current semantic owner

`VexLifeVisionFamily` owns only the human-facing Vision semantic relationship:

~~~text
State
  Held
  PermissionNeeded
  Ready
  Enabled
  Looking

Control
  Enable
  Disable
  LookNow

Effect
  RequestHostPermission
  RequestEnable
  RequestSnapshot
~~~

Controls request effects; they do not fabricate outside truth.

~~~text
ENABLE_REQUESTED != HOST_PERMISSION_GRANTED
LOOK_NOW_REQUESTED != FRAME_OBSERVED
FRAME_OBSERVED != FRAME_INTERPRETED
FRAME_INTERPRETED != VEX_RECEIVED_CONTEXT
~~~

The first generated product manifestation deliberately projects `Held`. The button is visible but
disabled until a later exact runtime owner can truthfully connect the effect.

## Current Partner pressure

VexWork PR #267 and VexLife-Android PR #219 currently pressure:

~~~text
Partner
-> Signal
-> Connection
   -> typed Id
   -> Decoder
   -> Observation
   -> Receiver
-> Store only when durable admitted truth is earned
-> Family State
~~~

They are draft/current architecture pressure, not accepted universal VexArchitecture foundation.

For Vision, the expected later relationship is approximately:

~~~text
selected browser/window outside reality
-> screen/window Connection
-> frame Signal
-> exact capture Observation
-> Safety / scope / cardinality Receiver
-> derived visual observation
-> Vision Family / Conversation consequence
~~~

The exact reusable Partner/Signal Kotlin spelling is held until the physical capture implementation
earns it in this repository or its rightful SDK owner.

## No premature Store

The first Vision slice requires raw screenshot retention to remain false.

~~~text
RAW_FRAME_OBSERVED != STORE_REQUIRED
OBSERVATION != MEMORY
OBSERVATION != TRAINING
~~~

Do not add a Vision Store merely for symmetry. A durable Store becomes justified only if later product
pressure earns visual-observation truth that must survive restart independently of Conversation.

## Product manifestation

The current Blueprint adds one stable Companion coordinate:

~~~text
element.vexlife-workspace.companion.vision
~~~

VexWeb manifests that coordinate in the Companion header without an interaction binding. The canonical
generated product renders:

~~~text
Vision · Held
~~~

as a disabled control. This is intentional capability truth, not incomplete event wiring.

## Held effects

~~~text
macOS Screen Recording request
screen/window selection
screen capture
active visual observation
continuous/background capture
visual-model input
browser control
Memory
training
publication
~~~

Those effects remain with their rightful future owners and current VexWork/SDK trajectory.

<!-- [VXG RealForever][VEXLIFE-PROD][VISION-COMPANION] -->
