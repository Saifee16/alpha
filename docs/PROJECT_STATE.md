# Project state — 2026-10-18

## Current: return to the controlled, multi-subject browser

Product goal: a simple private Android browser where parents define subjects, learning niches, complete search phrases, blocks and trusted sources. NASA remains an optional native-playback proof-of-concept. No backend, account system, AI moderation or provider framework was added.

Baseline verified/fetched: `41a0c3063d29cbb9555b27e01c76febfc2526434` from `feature/familysafe-loop5`. Current implementation commit: `af23c6319c04959abc6fc8d03515cd3018f4470f`, on `codex/familysafe-live-validation`; the original branch/history is preserved. This documentation update follows that verified implementation.

- Fixed a reproduced general-search defect: denied submitted queries fell back to all enabled website results. Two new emulator regressions failed at visible website buttons before the one-line result-gate fix and passed afterward. Empty-search directory browsing remains restricted to approved sources.
- Parent Topics now offers **Suggest search phrases** for explicit niche phrases and synonyms. Four finite wordings are added to editable text, including `football passing drills for kids`; review and **Save phrases** are required. Suggestions preserve originals and are idempotent; blocked/invalid input and count/length overflow reject the whole suggestion. No automatic expansion of saved rules, new matching rule, persistence migration, provider or dependency was introduced.
- Fresh verification **PASS**: 89 JVM tests/14 classes, 0 failures/errors/skips; debug and instrumentation APKs built; both APK signatures verified; lint 0 errors/17 warnings. Full final command took 3m2s, 84 tasks (14 executed/70 up-to-date). Seven version notices plus ten existing UI/player warnings account for the host's 17 findings; no new security-critical finding. Debug APK SHA-256: `86ea78ae622001e97a02558c7cc9ed16b83d5f06212c22ca57805abbdcdec431`.
- Final core emulator batch **PASS**, 8/8 in 61.364s: two real parent/search regressions plus six WebView restriction tests. Verified API35 `FamilySafe_Tablet_API35`, `ranchu`, WebView 124.0.6367.219; launched on `emulator-5560` with a temporary read-only disk overlay and no snapshot save. No physical tablet was connected or tested. Existing signing certificate matched before data-preserving emulator upgrades. No family installation was reset.
- Preserved completed reliability work: an additional JVM regression proves cancellation returns and rejects results while disconnect blocks/throws. This does not prove Android socket cleanup timing or explain the historical native-preview timeout.
- Closed NASA experiment: authorized Windows API/manifest/MP4 requests passed and the reference SHA matched. Its earlier selected emulator batch recorded 9 safety/browser passes and 1 live discovery failure (no eligible playable candidates); preview/child playback were not reached and nothing was approved. The opt-in failing reproduction is retained. No further NASA-specific diagnosis followed the user's priority correction.

Evidence stays ignored under `app/build/live-validation-evidence/`: `core-final-build.txt`, `core-jvm-summary.json`, `core-final-runtime.txt`, `core-red-runtime.txt`, signatures, WebView/crash evidence, prior `focused-runtime.txt` and `connectivity/`. Full instrumentation suite and physical-tablet/adult content/rights checks are **NOT RUN**. See [TEST_MATRIX.md](TEST_MATRIX.md) for exact distinctions.

Remaining product gap: accepted phrases currently select a trusted category website directory, not matching pages searched inside a source. A parent must define a specific niche; enabling Football alone does not authorize arbitrary appended words. Source/category labels do not classify every allowed page, and filtered video discovery is not individual approval. PIN, host/path restrictions, per-video review and SHA-256 gates remain in force.

Next milestone: validate one useful trusted source's permitted structured search and bounded destination results, reusing the existing policy/source model; add only the integration actually needed. Then execute the supervised non-destructive physical checklist. Replacing per-video approval requires explicit user approval of a concrete content-suitability, rights and playback-isolation approach; no change to that boundary is authorized here. Keep National Geographic Kids and YouTube child playback disabled. Do not claim unsupervised readiness.

## Historical cloud Loop 5

Starting Git commit: `5f230ef`. Fresh validation **PASS**: 81 JVM tests across 13 classes, 0 failures/errors/skips; both APKs built; lint 0 errors/12 warnings unchanged. The 10 new network-operation tests passed, including a scheduled-deadline test with blocking fake I/O (not Android). Full required command: 1m5s, 84 tasks (21 executed/63 up-to-date). All 21 instrumentation methods compiled; none executed. The Loop 4 baseline below remains historical.

Current APK: `app/build/outputs/apk/debug/app-debug.apk`, v2 signature PASS, SHA-256 `59be53e95fcdd6548e538c8e35a385c56f3841b8b74ffaa095f412dc0fbe6648`; instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Evidence stays ignored under `app/build/loop5-evidence/`: `final-build.txt`, `jvm-summary.json`, `live-access.txt` and response headers.

- Shared monotonic operation budgets: 60s for discovery search/manifests/HEAD, 120s for player manifest/download/SHA-256. Scheduled deadline/cancellation marks the operation terminal immediately and queues best-effort active-connection disconnect on two daemon threads (queue capacity 16), keeping UI/timer threads separate; existing 30s native preparation watchdog follows successful verification. No automatic retries or redirects. Existing PIN/category/metadata/approval/digest gates remain required.
- Live evidence: the configured host list includes `images-api.nasa.gov`, `images-assets.nasa.gov`, `images.nasa.gov` and `www.nasa.gov`; runtime `network.state=unknown` does not prove effective readiness. API GET and canonical media HEAD actually fail at proxy CONNECT with HTTP 403 (`curl` exit 56; initial strict attempt also exit 22/403). No NASA JSON/media response was reached. Captured headers stay ignored under `app/build/loop5-evidence/`. No network configuration changes, push or publication occurred.
- No device runtime is available. JVM fixtures and APK compilation cannot prove Android socket cleanup timing, native preparation or physical-tablet behavior; no hard real-time guarantee is established. Connect/read timeouts are capped to the remaining budget and zero-progress reads deny. Historical preview timeout cause remains unresolved; a retry alone is not a diagnosis.

Next milestone: resolve the external proxy denial without weakening verification, repeat live official discovery/asset checks, then run explicitly disposable runtime tests and the non-destructive physical-tablet checklist with adult content/rights and device-protection review. Keep National Geographic Kids and YouTube child playback disabled.

## Historical cloud Loop 4

Starting Git commit: `aa8ac42` (GitHub migration merge; engineering baseline `fe18c73`). The completed cloud onboarding baseline was 54 JVM tests, both APKs built, lint 0 errors/12 warnings. Loop 4 fresh integration validation **PASS**: 71 JVM tests across 12 classes, 0 failures/errors/skips; both APKs built; lint 0 errors/12 warnings. Final command: 42s, 84 tasks (19 executed/65 up-to-date). Of the tests, 17 target new discovery/library/attempt behavior (8 discovery, 6 library/migration/adversarial, 3 attempt). All 21 instrumentation source tests compiled, including the new late-lock discovery fixture, but none executed. Historical Loop 3 evidence below does not validate these changes.

Loop 4 debug APK: `app/build/outputs/apk/debug/app-debug.apk`, v2 signature PASS, SHA-256 `fcdc46df32d05fc33af9a18bef5a224ffbb5e98995149000d2e526c1f1a6cd0e`. Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Build evidence: ignored `app/build/loop4-evidence/final-build.txt`. Scoped security review found no new bypass; this is source/fixture evidence, not Android runtime assurance.

- Parent-only NASA search selects an enabled topic and an exact approved phrase; blocked phrases still deny. One page examines at most five JPL/video records, validates bounded plain metadata, resolves only exact official mobile assets and probes MP4 type/known length ≤32MiB.
- Store at most 30 local candidates with stable NASA ID, category, title, description, credit and rights warning. The original two Space candidates and existing decisions migrate unchanged. Discovery adds candidates without automatically approving or silently replacing reviewed metadata/category. Candidate removal also removes its decision.
- Existing per-item parent preview/confirmation and SHA-256 approval remain required. Children see only approved enabled-category items with unblocked metadata; downloads must match the approved digest. No child feed/discovery, thumbnail or external playback route is added.
- Media preparation now has a 30-second watchdog and per-attempt callback ownership. Fixture regression tests cover timeout/stale attempts; the earlier 180-second live-preview timeout's cause remains unresolved. A successful retry is not proof of a fix. The prepare timer starts after verified download; a total network deadline and prompt disconnect/cancellation remain limitations.
- Live API/media validation is blocked by the cloud network policy until `images-api.nasa.gov` and `images-assets.nasa.gov` are permitted. Draft allowlist additions also include `images.nasa.gov` and `www.nasa.gov` for official documentation/rights review; they are saved but not applied or published. No new credentials or NASA API key are required. No `/dev/kvm`, emulator runtime or physical tablet is available; instrumentation compilation is not execution.

Next milestone: apply the NASA host allowlist additions, run live discovery on an explicitly disposable test device, then perform non-destructive physical-tablet validation and adult content/credits/rights review. Reproduce and diagnose historical preview reliability with measured transfer/prepare/lifecycle evidence. Keep National Geographic Kids and YouTube child playback disabled.

## Historical Loop 3 evidence

Loop 3 starts from `eb14e4b` (baseline rebuild: 45 JVM tests, 0 lint errors/16 warnings, 1m18s). Catalog commit: `4d80430`; verified implementation/test source commit: `43bbb78` (only trailing blank lines were trimmed after verification). Final build PASS: 54 JVM tests, 0 failures/errors, both APKs built, lint 0 errors/17 warnings, 5m22s (84 tasks: 19 executed, 65 up-to-date). All 20 distinct emulator scenarios passed across overlapping batches: initial 17/20 (762.45s), focused 7/8 (577.044s), final live 1/1 (145.835s). This is not a single clean 20/20 run; an earlier live-preview timeout remains unexplained. See [TEST_MATRIX.md](TEST_MATRIX.md) for incidents and limits. Final ADB inventory contained only the emulator, with no physical tablet.

## Historical Loop 3 implementation

The parent enables **Space** in PIN-protected **Topics**, then opens **Videos** for two fixed animation candidates: Europa Clipper and NISAR. The parent previews a bounded local download, closes it, confirms full content/credits/rights review and approves or rejects. Preview Close/Back returns to authorized foreground review; Back from settings locks access. Candidate IDs, decisions and approved SHA-256 persist locally. Child playback requires a known approved item, enabled Space, unblocked metadata and a matching SHA-256 on redownload; replacement media requires re-review. There is no child feed/thumbnail discovery, manual URL, autoplay-next or external-app fallback.

Downloads require exact HTTPS paths, no redirects and a complete file no larger than 32MiB. Native playback uses a temporary local file; backgrounding stops/deletes it. Rotation/background locks the parent and clears selection, preview hash and session. Parent confirmation does not establish automatic suitability or rights clearance.

## Earlier baseline evidence

- Toolchain: JDK 17.0.20.1+1, Gradle 8.13, Android SDK 36 and Build-Tools 35.
- Earlier Gradle build: 45 JVM tests; app and instrumentation APKs compile; lint 0 errors, 16 warnings (7 dependency notices, 1 intentional debug YouTube JavaScript notice, 8 NASA debug `SetTextI18n` notices). The earlier full wrapper run took 5m33s; the Loop 3 baseline rebuild took 1m18s. Neither verifies the new workflow.
- Fourteen instrumentation tests ran and passed on API 35 Android 15 AOSP x86_64 (1280×800, 160 dpi; WebView 124.0.6367.219; WHPX). See [TEST_MATRIX.md](TEST_MATRIX.md) for timings and observed results.
- Process restart preserved identical settings hash (PID 3172 → 3216); child query remained visible while parent session was absent. Runtime fatal-error buffer had 0 lines and no FamilySafe fatal entry.
- NASA playback in the isolated adult debug harness passed playback, pause/resume, seek, completion, and background-release checks.
- NASA Space Place rendered fully and BBC Bitesize navigation was visible. National Geographic Kids returned HTTP 200 but rendered blank; keep it disabled. Do not weaken the browser policy or enable JavaScript to make it work.
- YouTube API flag remains false, no key is built in, and YouTube child playback remains disabled.

## Historical Loop 3 remaining work

- Validate installation and usability on a physical tablet; none was available for this run.
- Keep National Geographic Kids disabled pending a safe rendering fix or replacement.
- Physical PIN latency is NOT RUN. Corrected emulator authentication checks passed with measured waits about 9.7/10.2/10.6s; authentication assertions and PBKDF2's 600,000 rounds remain unchanged. Preserve the unexplained earlier live-preview timeout as a reliability limitation.
- Final process restart PASS: PID 4744 → 4788, identical settings hash, query visible and parent session absent. The first UI dump had a null root; a wake/start retry succeeded while the recorded power state was already Awake. `final-process-restart.txt` retains that retry.
- Adult-review candidate content/credits/rights, then follow [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) on the actual tablet. Human verification of Family Link/device controls remains pending. Never run destructive `FamilyFlowTest` or `NasaReviewFlowTest` on a family tablet.

Loop 3 evidence is in ignored `app/build/loop3-evidence/`; earlier files remain under `app/build/device-evidence/` and `evidence/`. Final crash buffer had 0 lines and Android reported no ANR since boot. Approved child-player screenshot showed a spacecraft frame; parent preview screenshots captured opening black frames and do not provide visual playback proof. Offline error/Retry and parent-landscape/child-portrait layouts were readable; NASA rendered fully, BBC navigation was visible and National Geographic Kids remained blank.

Historical Loop 3 debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 11,267,063 bytes, SHA-256 `4eec09be21c007871a3e76b6c0d59f0b75e5ef1ebe4cb496fbc1f3fbd94f3cd9`, v2 signature PASS. Package `family.safe.debug`, version 0.1.0, minimum26/target36.
