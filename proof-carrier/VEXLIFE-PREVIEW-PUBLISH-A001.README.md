# VexLife Preview publication carrier A001

[VXG RealForever]

This branch is a host-carriage surface only. It must not merge into Product `main`.

It carries one exact Preview publication of:

- repository: `VexGPT/VexLife-Prod`
- branch: `main`
- source: `56926d928b479869dce1bd7c69216ac88ced2784`
- tree: `5f463a0733b82df45948b7bd7e99404f8aab9217`

The payload invokes the Product-owned release script:

```text
tools/release/publish-mac-release.sh preview
```

and returns remote readback for both the GitHub Preview Release and the `gh-pages` release catalog.

This is a direct VexLocalBridge release host package, not a `vex.relay.task/v2` RELEASE operation.
