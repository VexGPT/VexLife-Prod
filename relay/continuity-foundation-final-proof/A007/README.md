[VXG RealForever]

# Continuity foundation final proof — A007

Fresh repair of A006 formation only.

Exact Product candidate remains unchanged:

```text
head  603bfebed58bba47b510d5cba27ab8795ad71c6c
tree  55b689eed95f5a961dc1ee2bd72cd5789f17c66e
base  655e7c78cefba06a5e87afad285bb25a08632f05
```

A006 was rejected by the current VexRelay manifest validator before Product source acquisition because
`stage.maxFiles=50`; ExactHeadSourceOwnerProof emits REVIEW_SNAPSHOT with that value, and the current
manifest contract requires 1..10.

A007 changes only:
- attempt/package/task identity A006 -> A007;
- `stage.maxFiles=10`;
- adds local preflight assertions for every explicit manifest/command cap used by this package.

Already audited:
- `maxSelectedSourceEntries=10`;
- `maxSelectedSourceBytes=3,000,000` <= 5,000,000;
- `maxDiffBytes=8,000,000` <= 10,000,000;
- proof command timeouts 1,800,000 / 1,800,000 / 3,600,000 <= 3,600,000;
- outer `exactPathRefs` has no item-count cap and may carry the exact #212 changed-path membrane.

Product head/tree, authority boundary and proof commands remain unchanged.

Bundle SHA-256:
`3b358e2451720f8d355dcf38a5acc554780c68b5eea785313ec6254a8d3ffd67`
