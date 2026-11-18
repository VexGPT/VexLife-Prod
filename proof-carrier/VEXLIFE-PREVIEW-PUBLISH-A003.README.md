# VexLife Preview publication carrier A003

[VXG RealForever]

A001 proved the exact-current-main stop correctly. A002 showed that a chat-pinned current head can become
stale before Victor invokes the host carrier in this actively merging repository.

A003 removes that human churn without weakening exactness:

```text
one command on macos-victor
-> observe a stable VexLife-Prod main head + tree
-> seal that exact head/tree into candidate.json during final package formation
-> digest-bind the package
-> payload re-checks origin/main == sealed head
-> source-owned Preview release process
-> independent Release + gh-pages readback
-> canonical RETURN
```

The current-main observation is performed by the bootstrap immediately before package formation. The package
still contains one immutable exact candidate; this is not a floating release task.

This carrier branch is execution carriage only and must not merge into Product main.
