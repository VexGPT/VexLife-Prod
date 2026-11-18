# Temporary Relay carriage for PR #252 — A002

[VXG RealForever]

A001 was not replayed after the shown invocation found a pre-existing local bootstrap root:

~~~text
PR252_A001_BOOTSTRAP_ROOT_ALREADY_EXISTS_DO_NOT_REPLAY
~~~

That invocation stopped before formation. Because the earlier A001 local custody is not available as
canonical RETURN evidence, A001 is treated as unconsumed/unknown rather than guessed as PASS or FAIL.

A002 preserves the same semantic task and exact Product candidate, while rotating attempt/request/
correlation/package identities and using a fresh attempt-local bootstrap root.

Exact candidate remains:

~~~text
base 0ebc8a768b2bc356c1912b87d10fae70b4a3c9e6
head e23f61ff933b72f8cfb23bd5f79a108834871784
tree d62d54659f22fda14cc84a30d90cd5460036b914
~~~

Proof commands and carrier mechanics are unchanged from A001.

No GitHub Actions. No Product mutation. No replay of A001.

<!-- [VXG RealForever][PR252][TEMP-RELAY][A002] -->
