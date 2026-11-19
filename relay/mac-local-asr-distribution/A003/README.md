# A003 — SourceOwner retry after A002 proof heap exhaustion

[VXG RealForever]

A002 canonical outer RETURN:
```text
sha256 6f67f2d5ebd4554f23dc00a05cf8cac5ca9bd8f0d48f43378cc415fa21cd1c9d
outer disposition PASS
inner sha256 d1909b06cd085591a3c3a7c30aa8475a4b00d5e3d836624a607d8ff8dcd5f02a
inner disposition SAFE_FAILURE
```

The current Mac Relay/VexLocalBridge path itself passed: admission, attempt reservation, native inner
execution, exact inner RETURN retention/consumption, and cleanup.

The first SourceOwner Gradle command then stopped at `compileKotlinJvm` with
`java.lang.OutOfMemoryError: Java heap space`. No Kotlin source diagnostic preceded the failure.
No Product/Home/microphone/lifecycle effect occurred.

A003 preserves the semantic SourceOwner task and rotates attempt/request/correlation/package identity.
The repair is companion-only:
- `-Xmx2048m`
- metaspace 768m
- reserved code cache 512m
- `--max-workers=1`

PR #384 remains byte-identical at:
```text
head caf20c78a797ac0579349915c26c79cfc4b1a39f
tree 7e499f4a3ae88ab6ccc906146d3f3ac7bc0509cb
```

Formation carrier:
```text
VEXLIFE-PR384-MAC-LOCAL-ASR-RELAY-FORMATION-A003--sha256-0e0d7be5ee2e936503398b9664c7e6700f5893206a2601e178e3513508ced0c2.zip
sha256 0e0d7be5ee2e936503398b9664c7e6700f5893206a2601e178e3513508ced0c2
bytes 8749
```

It uses current SDK `8543c7d755faba04d4c4019c02e84f992667918a`, current
`source.vexrelay.current-contract/v1`, native VexRelay task-archive carriage, and checkout-free
Mac source admission. Local Workspace is not used.

<!-- [VXG RealForever][PR384][PR386][A003][HEAP-REPAIR] -->