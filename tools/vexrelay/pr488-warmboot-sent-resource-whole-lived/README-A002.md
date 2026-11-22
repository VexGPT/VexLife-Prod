[VXG RealForever]

# PR #488 whole-lived acceptance A002 — concurrent setup recovery

A001 reached the exact Product candidate build and then stopped before the lived gate:

```text
SAFE_FAILURE
FRONT_DOOR_CONTROL_FAILED
```

Read-only recovery identified the exact Product failure projection:

```text
state      UPDATE_AVAILABLE
control    CONVERGE_DESIRED
effect     CONVERGE_PRODUCT:START
failure    STOPPED_SAFELY
detail     VexLife setup lease holder is not one exact owned VexLife setup for this Home
```

A001 ran inside VexLocalBridge capsule `cc8ad614...`; five seconds after that stop, the current
runtime receipt was formed under a different VexLocalBridge capsule `e378ac7c...`. The Product
lease recognizer is byte-identical between the old running source and the recovered candidate.
This is therefore treated as concurrent setup pressure unless A002 produces contrary evidence.

A002 changes proof carriage only:
- stable-clear setup-lease preflight before START / CONVERGE_DESIRED;
- exact holder PID + bounded command evidence when occupied;
- full Front Door activation failure retained in after-state;
- a lease race during convergence is an environment hold, not a candidate finding.

Product source, candidate head/tree, model selection, Conversation/Memory protections, and the
human two-Send lived gate are unchanged.

DO NOT MERGE this proof companion.
