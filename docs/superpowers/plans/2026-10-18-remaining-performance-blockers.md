# Remaining performance blockers implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement the tasks below. An independent read-only agent investigates article/input evidence; the coordinator owns the emulator and all edits.

**Goal:** Reduce demonstrated PIN and reading latency without changing authentication resistance, saved credentials, search behavior, settings or signing identity.

**Architecture:** Compare the existing Android JCA PBKDF2 provider with a maintained upstream implementation using identical ASCII PIN bytes, salt, iteration count and key length. Keep v1 records if derivation is identical. Split article/network/renderer and input/result-clearing measurements before choosing any UI change.

**Tech stack:** Kotlin/Compose, Android API26+, JCA, candidate Bouncy Castle lightweight PBKDF2 API; API35 disposable emulator.

**Spec:** Owner's continuation request of 2026-10-18: remaining performance blockers only; new APK only after measured improvement; PR #2 stays draft/unmerged.

## Global constraints

- Continue `codex/familysafe-product-rescue`; do not modify main or merge PR #2.
- Preserve 600000 PBKDF2-HMAC-SHA256 rounds, 16-byte random salt, 32-byte key, constant-time verification and persisted failed-attempt reservation.
- Keep existing v1 records and PIN alphabet (6–32 ASCII digits). No new format/migration without first proposing authenticated migration and trade-offs to the owner.
- No provider catalogue, NASA, child UI or working search feature expansion/refactor.
- No family-device reset, saved-data deletion, signing-key change, personal query/PIN logging or raw-log upload.
- New review APK only if comparable baseline/after measurements demonstrate improvement; retain the previous review artifact.

## Review focus

- Fixed pre-existing v1 records verify with identical derived keys; unsupported/malformed records and wrong PINs deny.
- Failed-attempt throttling survives recreation/process interruption, and background authentication cannot restore parent access.
- Optimized derivation is local to PIN hashing; no global provider registration or TLS/provider-order changes.
- Article source/redirect restrictions, CSP and background/Back cleanup remain enforced.
- Query edits immediately remove stale actions; saved settings and exact-byte upgrade fixtures survive replacement.

## Task 1: Provider profile and identical-key candidate

Files: `app/src/main/java/family/safe/settings/PinSecurity.kt`, `app/build.gradle.kts`, `app/src/test/java/family/safe/settings/PinSecurityTest.kt`, `app/src/androidTest/java/family/safe/PinPerformanceDeviceTest.kt`.

- [x] Capture actual platform provider class and a sampled method trace of the same JCA derivation; benchmark a public known-answer fixture independently of Activity/UI.
- [x] Compare a supported upstream PBKDF2 implementation without lowering rounds. Discard the candidate if keys differ or no material improvement is demonstrated.
- [x] Run the existing production verifier against the explicit 5000 ms device budget and retain the expected failing baseline.
- [x] Add independently derived legacy-record/ASCII-length vectors and negative cases. If the candidate succeeds, replace only the private derivation backend, clear temporary PIN bytes, and retain `MessageDigest.isEqual` and the v1 parser.
- [x] Run relevant PIN/security/persistence tests and repeat the budget measurement. Document dependency size/support and faster legitimate verification versus unchanged offline work factor.

## Task 2: Article and input phase evidence

Files: `app/src/androidTest/java/family/safe/PerformanceBlockersDeviceTest.kt`; production browser/input files only if phase evidence identifies a repair with a failing regression.

- [x] Add opt-in, disposable-emulator-only measurements for actual PIN unlock, first/subsequent article open and empty/results-present input draws. Preserve public fixture settings independently of measurements.
- [x] Measure API transport/HTML preparation separately from hardened WebView construction, intercept return, visible commit and complete rendering. Record WebView version and hardware/layer details; retain safety restrictions.
- [x] Collect distributions under the same emulator configuration with no build running; label first-render and network variability explicitly. Do not mistake test synchronization for app latency.
- [x] Implement only a demonstrated narrow fix, with a failing regression first; otherwise report remaining environmental/network cost and leave working UI/search intact.

## Task 3: Integrated validation and review delivery

- [x] Run the full JVM suite, lint/build, authentication/throttle/background, browser, persistence and exact-byte upgrade/device regressions; report every failure and skip.
- [x] Recheck physical-device availability read-only. If accessible, use non-destructive measurements and in-place same-signature replacement only; never run resetting fixture instrumentation on family hardware.
- [x] Measure at least ten cold process starts and repeated unlock/article samples before/after where feasible; report sample counts, median, range and percentile method, without treating timeouts as completed launches.
- [x] If improvement is demonstrated, commit exact application source, build the non-debuggable same-package/same-key APK, verify embedded source revision/package/certificate/hash, and deliver a separate review artifact. Keep the existing GitHub release unpublished and upload only verified APK/checksum within the owner's existing authorization.
- [x] Record exact code changes, evidence, regressions, unchanged security parameters and remaining gaps; update the draft PR and leave merge pending actual-tablet approval.

## Execution ledger

- Platform provider is Android BC's UTF8 PBKDF2 factory; sampled trace places about 83% of recorded CPU in native digest final/update/init entry points. This includes native hash work, not isolated JNI overhead.
- Five upstream probe keys matched the independent fixture. Median wall time 5515 ms versus platform 10247 ms; some upstream samples still miss 5000 ms. Actual unchanged production verification failed the existing budget at 14987 ms.
- Ruling: retain v1 and replace only its private derivation backend with pinned upstream BC lightweight PBKDF2. Identical ASCII input/parameters/key means migration is unnecessary. No global provider registration. Validate compatibility and actual app timings before delivery.
- Settled baseline: ten completed cold starts; actual unlock median 15133 ms (11868–23655); ten article opens passed, first 15308 ms and remaining nine median 3423 ms. Input median 123.5 ms without results versus 416.5 ms clearing one result. No working search/UI changes are justified by those observations alone.
- New reader phase fixture was initially denied before fetch because it omitted required community-source consent. Retain its failure as a test-harness diagnostic; fix fixture only, require URL/title identity and HTTP status evidence, then rerun against the unchanged APK.
- First legacy-fixture preparation skipped without its opt-in flag; the explicitly opted-in rerun passed and refreshed the preparation evidence. Do not treat the skipped attempt or stale earlier evidence as a pass.
- Independent production review found no Critical/Important defect. It identified process-interruption coverage as a validation gap; add a guarded two-stage interruption after durable reservation and verify the saved throttle blocks a correct PIN after restart.

- Completed validation and delivery evidence is in `docs/REMAINING_PERFORMANCE.md`. Fresh full gate: 150 JVM tests, lint 0 errors/18 warnings, application/test packaging passed. Final selected device batch: 24/24, no failures/skips; separate exact-byte upgrade, interrupted-authentication reservation and distribution controls passed.
- Candidate is pinned to application commit 252c809c00511aa1eabf857c9c3e48638cb49bc0. No versioned migration was implemented: identical v1 output preserves both existing records and installation rollback compatibility. Only the private PBKDF2 backend/dependency changed in production.
- Article/input distributions and local-HTML controls establish remaining rendering/environment costs; no causal production reader/search/input change was justified. Improved observations are not attributed to the PIN backend.
- Physical-device inventory was rechecked read-only: only the API35 ranchu emulator was present. Actual-tablet and API26 runtime tests remain NOT RUN; no family-device resetting instrumentation was used. Completion of this bounded repair does not approve merge or independent child use.
