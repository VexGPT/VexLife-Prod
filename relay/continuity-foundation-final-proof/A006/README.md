[VXG RealForever]

# Continuity foundation final proof — A006

Fresh repair of A005 formation only.

Exact Product candidate remains unchanged:

```text
head  603bfebed58bba47b510d5cba27ab8795ad71c6c
tree  55b689eed95f5a961dc1ee2bd72cd5789f17c66e
base  655e7c78cefba06a5e87afad285bb25a08632f05
```

A005 was rejected by the current VexRelay manifest validator before Product source acquisition because
its proof policy set `maxSelectedSourceEntries=50`. Current contract requires 1..10.

A006 changes only:
- attempt/package/task identity A005 -> A006;
- `maxSelectedSourceEntries=10`.

Proof commands, candidate head/tree, authority boundary and non-mutation policy are unchanged.

Bundle SHA-256:
`816a94a753195d3b93fbd01ca2a43db8adef87ce945b3b7035267a84bdf603e7`
