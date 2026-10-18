# Focused performance repair

**Goal:** Profile cold launch and PIN separately, repair demonstrated bottlenecks, and supply a compatible private review APK while PR #2 stays draft/unmerged.

**Scope:** Existing `codex/familysafe-product-rescue` checkout is clean and already contains the requested PR. Work here to retain its existing build and signing setup. No providers, NASA expansion, search redesign, family-data reset, merge, or public release.

- [x] Collect three force-stopped process launches, isolated PIN verification and UI/input/article timings on the API35 disposable emulator. Check physical-device availability without altering family data.
- [x] Compare the platform PBKDF2 implementation with equivalent standard HMAC work. Retain existing known-answer, legacy-record and negative tests; no cryptographic implementation or format change is justified by this pass. Keep 600000 SHA256 rounds, 16-byte random salt, 32-byte key, constant-time comparison, v1 records and durable pre-hash failed-attempt reservation.
- [x] Use startup phase/frame evidence to choose one narrow startup/build optimization. Investigate input and article delay, with no speculative search refactor.
- [x] Run JVM, lint, affected security/lifecycle/browser and upgrade regressions; repeat the same measurements. Capture public-fixture screens only.
- [x] Build/sign/compare the APK, write timings and exact regression boundaries, update the draft PR, and provide the review artifact. Physical experience approval remains the merge gate.

Security/data review focus: malformed and unsupported records deny; wrong PIN persists throttle; background completion never restores parent access; same-signature replacement preserves exact settings bytes; browser/source revocation still denies. No resetting instrumentation on physical hardware.

## Evidence ledger

- Baseline isolated PIN: **22078 ms**, provider BC, existing v1 public fixture, 600000 iterations. Emulator only; no physical tablet connected.
- Initial sandbox Gradle daemon connection failed before tests. Rerun with local runtime access; retain failure log.
- Execution continues inline under the user's authorized focused repair request. Higher-priority instructions favor completing reversible authorized work without an additional planning approval.
- Root-cause hypothesis: debuggable review packaging adds runtime overhead across PIN and Compose drawing. Test one build flag before changing any application algorithm. Equivalent HMAC also took 18199 ms; no cryptographic replacement or migration is justified.
- Independent read-only review found no Critical/Important issue in build configuration, signing identity or guarded emulator testing. Real APK checks and measurements remain required.
- Initial fresh build: 148 JVM tests, two existing short-wait search-test failures (one-second worker entry and 100 ms completion assertion). Preserve logs; rerun serially without changing search code/tests.
- Serial gate: 148 JVM pass, 0 failures/errors/skips; APK/test builds pass; lint 0 errors/11 warnings. Source commit `3574ac501712a6d33c51d3f1edab93d9ec6ef70e`; application code and hash format unchanged.
- Controlled after batch: cold 3532/2981/2764 ms; isolated PIN 8366/10690/10128 ms; input median 185.5 ms (54–337); article 6661 ms. Three PIN budget assertions still fail at 5000 ms. Performance remains partial.
- Native HMAC standalone probe: 5865/5745/6101 ms, equal 600000-round public fixture key. It also misses the budget; do not ship a custom derivation or migrate credentials on this evidence.
- In-place upgrade passed exact synthetic legacy settings bytes/timestamp and existing PIN verification without writing; no family hardware touched.
- Owner explicitly authorized APK and checksum only to the existing unpublished draft release. Those two assets were uploaded and downloaded again with matching SHA-256; no logs/keys/personal data uploaded. PR #2 remains unmerged; no main change.
- Selected device batch: 29 pass, 6 opt-in live skips, 1 failure caused by an outdated Archive helper scrolling the fixed home-header parent entry. Corrected only that selector; test APK rebuild passed and the complete affected case passed in 49.357 s. Retain the initial failing batch; do not claim a clean full rerun. Uploaded application bytes/source remain unchanged.
- Final device inventory: emulator only. Physical tablet/API26 remain NOT RUN; PIN and article delays remain material, and this delivery grants no independent-child-use or merge approval.
