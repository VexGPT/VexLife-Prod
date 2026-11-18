# Temporary Relay carriage for PR #276 — Memory Review / Keep A005

[VXG RealForever]

A001-A004 remain immutable history. A005 proves the post-A004 semantic-assurance corrections only.

Exact Product candidate:

```text
base 428bb31c8609474ecd6528a044ee47a9a9e89628
head 573c9589e310b4d46d6a9cd6d9ce4e3c48c569ab
tree 3e69e83f069be788abbc021ee702d2b18da80aa0
```

A005 adds no prompt/service/web behavior. The six-path membrane now includes
`VexLifeSemanticAcceptanceWriter.kt` only to expose an internal leased entry point while preserving
its ordinary public Keep behavior.

Semantic goals:
- one first durable human decision per review;
- exact decision receipt reused across retry even when retry time differs;
- conflicting second decision rejected;
- Keep decision written only under current Conversation custody;
- semantic acceptance and Score append share one Conversation writer lease;
- Not now / Decline still perform no Memory/Score mutation.

This temp PR must remain unmerged.

<!-- [VXG RealForever][PR276][TEMP-RELAY][A005] -->