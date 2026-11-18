# Temporary Relay carriage for PR #252 — A003

[VXG RealForever]

## Why A003 exists

A002 reached the accepted VexRelay recipe compiler but was rejected during manifest validation before
a task archive was formed:

~~~text
manifest.mjs:421
MANIFEST_SOURCE_ENTRY_CAP
~~~

Cause:

~~~text
#252 exact changed paths      10
A002 maxSelectedSourceEntries 12  INVALID
A002 stage.maxFiles           12  INVALID
live contract maximum         10
~~~

A006 worked because its seven-path candidate used the default/capped value 10. A001/A002 mistakenly
raised both review/source caps to 12 to provide headroom, but VexRelay intentionally forbids a review
or selected-source set larger than 10.

A003 corrects both values to 10, exactly matching #252's ten changed paths. It preserves the exact
Product candidate and proof commands while rotating attempt/request/correlation/package identities.

No A002 Product source was executed and no Product test ran.

No GitHub Actions. No Product mutation. No A002 replay.

<!-- [VXG RealForever][PR252][TEMP-RELAY][A003][CAP-CORRECTION] -->
