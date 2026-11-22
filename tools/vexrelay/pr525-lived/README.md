# PR #525 — composed runtime continuity + Warm Boot lived proof

[VXG RealForever]

This DO-NOT-MERGE sibling carries the next lived host proof after:
- #518 Stage-B SourceOwner PASS + Independent Assurance;
- #525 runtime-continuity composition SourceOwner A002 PASS;
- #525 Independent Assurance CLEAR_ON_EXACT_HEAD.

## Exact Product coordinates

```text
#518 base
3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d

A = exact #525 proved head
5a26a310878b191f39de521a8b5ae01a2d10b224

B = same-tree source-rotation witness
3c45cee139b2532f058a891715539f74394dfcb1

A tree == B tree
c96fe037602801379231b7b5c7f3b4808057e4df
```

B changes source identity only. No Product file differs from A.

## Lived question

Can the accepted composition realize the whole physical seam on the selected local 4B?

```text
already-installed/saved heretic-4b
-> Product no-acquisition Start of A when needed
-> A exact runtime receipt/PID
-> same-tree B service rotation using --reuse-existing
-> same runtime receipt/PID survives
-> B Prompt Preparation READY_REUSABLE
-> one ordinary human Send
-> Prompt Preparation CONSUMED
-> first provider stage cachedPromptTokens > 0
-> prefillTokens < promptTokens
-> same Send TURN_COMPLETED
```

## Safety / authority

Before starting anything the task asks Product's own setup-model projection and requires:
```text
selected = heretic-4b
savedInHome = true
```

No model-selection effect, model switch, or model acquisition is allowed.

If A is not already CURRENT, the task may use only existing Product no-acquisition controls:
```text
START_REQUIRED -> START
UPDATE/REPAIR -> CONVERGE_DESIRED
SERVICE_RECOVERY_REQUIRED -> RECOVER_STALE_SERVICE
```

Other states stop.

The B rotation uses the exact packaged setup with `--reuse-existing`, so it cannot acquire or switch models.

The harness never writes Conversation or Memory and never synthesizes the human Send.

## Human boundary

One terminal command starts the task.

Do not Send while it builds/starts/rotates.

Only after B reaches `READY_REUSABLE` does the task open VexLife automatically. That browser opening is the human gate:
- send one short ordinary text message;
- no attachment;
- return the canonical RETURN ZIP.

## RETURN content boundary

RETURN retains only source/runtime/service coordinates, aggregate hashes of protected Conversation/Memory before and after source rotation, preparation state, turnRef, provider token/timing counters, findings, and cleanup evidence.

It does not export the human message, model answer, Memory text, or reasoning.

Even PASS does not merge #525/#518, add UI, or implement ACTIVE/STAGING epochs. It closes the first composed physical reuse predicate.

<!-- [VXG RealForever][PR525][COMPOSED-LIVED-C001][DO-NOT-MERGE] -->


## C001 consumed / C002 current

C001 stopped before Relay task formation during local Node syntax checking:

```text
node --check payload-C001.mjs
SyntaxError: Unexpected reserved word
const aSource=await waitJson(...)
```

No Product build, setup, runtime, provider, Send, Conversation, or Memory effect occurred.

Root cause:
```text
C001 generator spliced at the first literal try{
-> that try{ belonged to findExe()
-> lived body became nested in a non-async helper
-> later predecessor helper definitions were discarded
```

C002 is rebuilt from the original A002 payload by preserving the complete helper layer through the
exact top-level execution anchor:

```text
try {
  if (!RESULT_DIR || !PAYLOAD_ROOT) ...
```

The composed A/B Product coordinates, host authority, lived acceptance, and human boundary are unchanged.

Exact C002 payload is independently syntax-compiled before bootstrap publication.

<!-- [VXG RealForever][PR525][COMPOSED-LIVED-C001-CONSUMED][C002-CURRENT] -->
