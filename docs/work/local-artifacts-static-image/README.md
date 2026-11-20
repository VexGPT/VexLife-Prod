# [VexLife Local Artifacts] Static image adaptation

[VXG RealForever]

## Stable base

This work starts after the Local Artifacts attachment/direct-context base landed on Product main.

~~~text
VexLife-Prod main
a4dd25e472d153b1cef24f7f35132933f3d725d0

tree
c86ad042177af0451fa0c35a0fdfec8bf0b745e6

integration
#434
~~~

The attachment path is an accepted dependency, not an invitation to reopen it:

~~~text
small readable attachment
-> exact OriginalArtifact
-> immutable readable projection
-> exact current REQUEST binding
-> bounded direct current-turn user-role context

large / overflow attachment
-> exact request-local resourceRef
-> bounded reach when more content is needed
~~~

Permanent:

~~~text
IMAGE WORK != REWRITE TEXT/DOC ATTACHMENTS
ORIGINAL BYTES != DERIVED REPRESENTATION
DERIVED REPRESENTATION != MODEL INTERPRETATION
EXACT HUMAN WORDS != ATTACHMENT CONTEXT
~~~

## Human outcome

A person can attach a supported static image such as PNG/JPEG to an ordinary Vex turn and ask about it,
without having to understand Vision, screen capture, provider tool syntax, or model plumbing.

~~~text
person
-> Add Files
-> image
-> exact request-bound visual material
-> qualified model-usable adaptation
-> Vex receives what the image actually contains
-> ordinary answer
~~~

No image understanding is claimed by this formation commit.

## Existing owners already earned

### Local Artifact

VexLifeLocalArtifact already owns:

- exact immutable original bytes;
- full SHA-256 artifact identity;
- per-request binding;
- per-message display name;
- separation between original material and derived projection.

Static image attachment should reuse that custody and request relationship.

### Current text/container converter

VexLifeLocalArtifactConversion currently admits:

~~~text
TEXT
ZIP
DOCX
XLSX
PPTX
~~~

Its DerivedProjection is readable-text-shaped. PNG/JPEG currently fall outside that admitted
relationship; do not disguise binary image bytes as text and do not infer image type from filename alone.

### VexLife Vision

VexLifeVisionFamily owns a different human relationship:

~~~text
selected live window scope
-> Engage / Stop
-> selected-scope state
~~~

Current Vision source explicitly preserves:

~~~text
SCOPE_ENGAGED != LOOKING
LOOKING != SCREEN_CAPTURED
ACTIVE_VISUAL_OBSERVATION != VISUAL_MODEL_INPUT
OBSERVATION != MEMORY
~~~

Therefore:

~~~text
STATIC IMAGE ATTACHMENT != VEXLIFE VISION
~~~

An attached PNG/JPEG needs no Screen Recording permission or selected-window session.

## Reusable adaptation seam

The reusable relationship is:

~~~text
OriginalArtifact
-> format/media-specific Adapter
-> bounded model-usable Derived Representation
-> exact REQUEST binding / current-turn projection
-> Companion interpretation
~~~

The existing readable-text projection is one realization of this relationship, not proof that every
future representation must be text.

A visual realization may eventually be one of:

~~~text
A. exact selected Companion supports qualified visual input
   image bytes / normalized image representation
   -> provider-native visual carriage
   -> Companion

B. selected Companion is text-only
   image
   -> qualified local visual Partner
   -> bounded visual interpretation
   -> ordinary Companion current-turn context
~~~

Do not create a generic Partner abstraction merely for symmetry. A Partner is earned only when one
real qualified visual realization needs a separate model/runtime owner.

## Model/runtime boundary

Current shipped Local Artifacts proof does not establish multimodal Companion input. No visual feature
may be inferred from model name, size, provider marketing, or generic Qwen lineage.

Before direct visual carriage is offered:

~~~text
exact model identity
+ Companion representation
+ runtime realization
+ runtime qualification
-> visual-input support truth
~~~

Otherwise the relationship stays held or uses a separately qualified visual adapter/Partner.

## First implementation edge

Start smaller than a multimodal framework.

1. Preserve existing TEXT/ZIP/DOCX/XLSX/PPTX behavior byte-for-byte.
2. Recognize one static image family from content signatures, initially PNG/JPEG.
3. Keep exact original bytes in existing Local Artifact custody.
4. Form a bounded image/media description owned by the artifact adaptation layer:
   - exact artifact ref;
   - admitted media type;
   - dimensions where safely decoded;
   - adapter/version identity;
   - no model interpretation yet.
5. Resolve one real model-usable visual realization:
   - qualified native visual input, or
   - one qualified visual interpreter producing bounded text.
6. Project the resulting derived representation as user-supplied current-request content, never system authority.
7. Preserve an exact held reason when no qualified visual realization exists.

## Proof pressure

The first implementation must prove both preservation and new behavior:

~~~text
existing text/document attachment tests unchanged          PASS
existing lived resume/direct-context relationship          PRESERVED
PNG/JPEG signature classification                          deterministic
invalid/spoofed image                                      held
oversized image                                            held/bounded
image-derived content                                      not system authority
static image path                                           no Vision Family mutation
visual interpretation                                      no Memory promotion
unsupported selected model/adapter                         explicit held truth
~~~

Lived acceptance:

~~~text
attach one supported image
-> ask an ordinary question about it
-> Vex answers from the actual visual material
-> no filename/content hallucination
-> no screen-picker/Vision side effect
~~~

## Relationship to future live Vision

If live Vision later earns capture-to-model interpretation, it may reuse the visual adaptation seam
after capture:

~~~text
static attachment ----> exact visual resource --\
                                            visual adaptation -> model-usable representation -> Vex
fresh Vision capture -> exact visual resource --/
~~~

The two source owners remain distinct even if they later share an adapter.

<!-- [VXG RealForever][VEXLIFE-PROD][LOCAL-ARTIFACTS][STATIC-IMAGE][FORMATION] -->
