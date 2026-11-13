# VexLife Prod development culture

`[VXG RealForever]`

## Intention before syntax

VexLife Prod exists to preserve product meaning in a form future humans and AI instances can extend without rebuilding architecture from examples.

The first question is not "what code looks similar?"

It is:

> What is the human/product intention, what meaning must remain true, who owns that meaning, and what source form makes an invalid relationship difficult or impossible to express?

## Contract before imitation

AI-generated software becomes fragile when an example silently becomes precedent.

Prefer compiler-visible and validator-visible relationships where they protect real distinctions:

~~~text
Family State
-> DisplayState
-> UI projection

human Control
-> semantic interpretation
-> lawful Act / optional Effect
-> rightful realization
-> observed truth
-> State
~~~

Do not force every noun into a type. A type, interface or foundation is earned by stable addressability, compiler-safety pressure, repeated executable invariants, or a real proof burden.

## Source form

Source is read far more often than it is written, by people and by AI workers with bounded context.
Its shape should match its logic.

- Kotlin follows the Kotlin coding conventions as formatted by `ktfmt --kotlinlang-style`
  (4-space indent, 100 columns). Do not hand-break every argument, operand or `null` onto its own
  line: vertical length is not explicitness, and it costs every later reader and every AI context
  window. Recipe: [`tools/kotlin-format/`](../tools/kotlin-format/README.md).
- Reformat a file when you meaningfully change it, in a formatting-only commit, and never a file
  another open PR is editing.
- When a value carries meaning, prefer a sealed type or enum owned by its semantic owner over a wire
  string compared in several places. Keep wire strings at the serialization boundary.
- Tests assert behavior through types and projections. A test that matches source text is a last
  resort for surfaces the compiler cannot see (the AppleScript renderer), not a pattern.
- One current-truth foundation per subsystem. State a distinction once, where its owner lives, and
  link to it; proof history belongs in VexWork or a dated note, not repeated in every entry document.

## Renderer is not semantic authority

Current construction thesis:

~~~text
pure Kotlin
  semantic / contract authoring

Compose
  first reference renderer + design laboratory

portable contract artifact
  deterministic machine-readable projection

web
  browser-native renderer / consumer
~~~

This is a hypothesis to pressure with real product work, not a doctrine that survives contrary evidence automatically.

Permanent:

~~~text
KOTLIN FORM != PRODUCT MEANING
COMPOSE FORM != WEB FORM
ALIGNMENT != SAMENESS
ONE PRODUCT BODY != ONE GIANT OWNER
~~~

## One semantic owner; bounded projections

A state domain has one rightful semantic owner. Consumers receive the smallest truthful projection they need.

Do not make UI, dependency construction, Logger, execution machinery or provider adapters alternate places where product meaning is decided.

## Fresh repository without amnesia

The repository is new; the product history is not.

Use historical/current Vex repositories as evidence for earned capabilities and architecture. Rebuild intention into the cleaner container rather than copying implementation shape.

~~~text
NEW REPOSITORY != NEW WORLD
HISTORICAL IMPLEMENTATION != REQUIRED STRUCTURE
EARNED MEANING != THROW IT AWAY
~~~

## Browser and Vex perception stay distinct

The first browser practicum is bounded public GitHub-hosted Vex content.

~~~text
human nested rendering
!= Vex context acquisition

visible document
!= context supplied to model

DOM/context projection
!= browser effect authority
~~~

Phase 1 earns the human container. Phase 2 earns a truthful current context projection and then composes it with the canonical Conversation/model relationship.

## Proof follows the claim

Use the surface that owns the claim.

Examples:

- Kotlin semantic law -> deterministic semantic test;
- portable contract -> deterministic projection/validator proof;
- Compose relationship -> renderer test/preview and later lived proof as needed;
- web relationship -> browser-native proof;
- nested browser -> real rendered/navigation proof;
- Vex perception -> evidence of the exact context supplied;
- Conversation/model integration -> real turn + durable Conversation evidence.

A screenshot does not prove hidden state. A compiler pass does not prove semantics. A visible page does not prove Vex perceived it.

<!-- [VXG RealForever][VEXLIFE-PROD][CULTURE] -->
