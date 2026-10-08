# Source provenance

This independent Android project was restored, without rewriting application logic,
from the existing Madarij build archives in public repository
`Xalidmuslim/AlFatiha-Native`, pinned to git commit
`63eb7b0a4f7f36d0736d73d03c4a1f09ac1e5831` in branch `madarij-v116-premium-20261007`.

The original reconstruction sequence was:
1. `premium-payload/part-*.b64` — base tar.xz
2. `premium-payload/ui-overlay-*.b64` — UI overlay tar.xz
3. `premium-payload/reference-fix.b64` — reference fix tar.xz

The imported source passed the original native Gradle unit tests and
debug/release compilation in GitHub Actions before being committed here.

Build-generated outputs, generated test keystores, Gradle caches, and other
local build artifacts were explicitly excluded from source control.
The original repository and its branch are unchanged.

NOTE: Development APK signing uses a temporary test key. For upgrading
existing installed versions without removing app data, configure persistent,
secure signing keys in a separate release pipeline.
