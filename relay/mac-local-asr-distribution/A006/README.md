PR #384 Mac Local-ASR distribution — SourceOwner A006

[VXG RealForever]

A005 canonical RETURN was consumed:

outer sha256
abdc48d777993a1f0ab1a6c0ea348955c0e418c53f74af06ce2e91d14525c0e9

retained inner sha256
51d73098840a5b7b08fdb9daf3b683a5684edee596951c5a9017f5538a5137e5

A005 materially passed:
- exact #384 head/tree;
- exact Mac application/service-image materialization;
- bundled whisper-server exists and is executable;
- exact whisper.cpp source commit;
- exact whisper.cpp source tree;
- private-service-image-root launcher binding.

It stopped before starting the final runtime smoke command because the current VexRelay executor
pre-validates a relative command path against the executor process cwd before applying the operation's
repository cwd. The provider executable itself had already passed `/bin/test -x`.

A006 changes only the proof launcher:
- command `/usr/bin/env` (absolute, current host executable);
- argv begins with the same repository-relative packaged `whisper-server`;
- same explicit DYLD_LIBRARY_PATH;
- same `--help` runtime smoke;
- then SourceOwner review snapshot / canonical RETURN.

This avoids widening the Product lane into shared SDK maintenance. No #384 Product bytes change.

Exact candidate:

#384 head cb47d5c92db709048ac668cfa3c0eadd34b02152
tree      c500a68873499ec754ea50a43d85f427debccb99

Local Workspace is not used. Product launch, real Home mutation, microphone, approval,
READY, merge and release publication remain held.

<!-- [VXG RealForever][PR384][PR386][A006][RELATIVE-COMMAND-CWD-WORKAROUND][DO-NOT-MERGE] -->
