# Project state — 2026-10-18

## Final Git and family handoff

The subsequent release task authorizes merging the complete verified feature history into `main` after fresh checks, preserving feature branches, and delivering the compatible debug APK through an owner-accessible draft GitHub release. No public release, APK in Git history, new feature development or safety relaxation is authorized. [FAMILY_INSTALL_CHECKLIST.md](../FAMILY_INSTALL_CHECKLIST.md) is the concise adult handoff; exact merged commit, final artifact checksum and fresh results are recorded in the release handoff notes. The earlier completion-gate sections below preserve their historical scope and limitations.

## Completion gate — supervised debug pilot, limited video catalog

This gate starts at `4a6b7ee73967ae23fe4243dfd67bcceaf6e80d04` on `codex/familysafe-live-validation`, retaining [draft PR #1](https://github.com/Saifee16/alpha/pull/1) and all earlier history. No merge or public APK publication is authorized/performed.

The practical web-learning pilot is preserved. **Khan Academy via Internet Archive now supplies one vetted mathematics lesson, Addition 3**, through structured parent discovery without manual URLs. Commons produced no candidate in its bounded transport check; Archive was the sole alternative. Exact publisher identity/policy, actual media, H.264/AAC codecs and a native Android probe were checked. This is a one-lesson catalog, not broad video search or an approved collection. Full adult suitability/credits/rights review and physical-tablet validation remain **NOT RUN**.

Parents explicitly save `addition` in an enabled topic, opt into the source for that topic, discover, preview and individually approve. Source consent/discovery never approves. Known identity, exact file metadata, complete bounded HTTPS download and pinned SHA-256 gate native preparation; children additionally require the parent's matching digest approval and current topic/source eligibility. Unknown results, changed bytes, redirects and unsupported providers fail closed. Legacy settings default to NASA identities with the new source disabled; PIN, attempts, requests and prior review decisions are preserved. No working web-search/parent-policy refactor, NASA expansion, new dependency or cloud service was added.

Maintenance: watch the complete approximately nine-minute lesson and review its license before approval; disabling source/topic or rejecting/removing the item revokes access. Adding another video requires a separately verified engineering catalog change. Automatic collection access remains unavailable. See [Archive evidence](ARCHIVE_FEASIBILITY.md) and [parent installation/checklist](PHYSICAL_DEVICE_CHECKLIST.md).

Fresh final build: `cleanTestDebugUnitTest testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` **PASS**, 5m14s, 85 tasks (19 executed/66 up-to-date). **128 JVM tests/19 classes, zero failures/errors/skips; lint 0 errors/13 warnings.** App and instrumentation APK signatures verify with the existing certificate. The local SDK metadata-version warning remains; it did not fail the build. The integrated app's 17 core parent/search/WebView regressions passed in 207.513s. Actual Archive discovery/preview/fixture-child-playback/background cleanup/source revocation passed in 103.003s, and late-search/parent-lock denial passed separately. The first live transfer failed with `SocketException`; cause unresolved. A System UI ANR was observed and dismissed before the successful rerun, without claiming causation. Independent code review found no confirmed authorization/integrity/transport regression. See [TEST_MATRIX.md](TEST_MATRIX.md) for batch distinctions and remaining limits.

Local debug APK: `D:\Alpha\FamilySafe\app\build\outputs\apk\debug\app-debug.apk`, package `family.safe.debug`, API26+. SHA-256: `9e8ad806b40c7e8d4735e069f4bd006e48a7c6fa5aef02bd0d370ab37a2ed399`. Signing-certificate SHA-256 remains `f88398895f95d7a226ff8a38bd96197686fbf6cf2cf88b3acd1f14836b332444` (app v2). Test APK is not needed on household hardware. Evidence is ignored under `app/build/video-gate/`; no raw media, keys or private settings are committed.

No physical tablet was connected. Only the explicitly selected API35 ranchu AVD ran in temporary read-only/no-snapshot overlays. Resetting instrumentation/engineering approvals were confined to those overlays. Physical touch, both orientations, PIN latency, installation/upgrade persistence, network interruption, reboot and household restrictions remain NOT RUN. Use `install -r` only after package/certificate/checksum comparison; never uninstall/clear/reset family data to recover an error.

Five existing review/PIN/lifecycle/rotation regressions also passed (89.352s), giving 25 distinct selected emulator scenarios passing across separate/overlapping batches, with the initial transfer failure retained. No full-suite pass is claimed.

Release classification: **supervised debug evaluation pilot with one mathematics video; overall V1/physical deployment remains partially complete**. Broader subjects and unattended child use are not verified. The adult must complete full content review, the physical checklist and household device restrictions before handing over the tablet.

## Previous V1 delivery — historical evidence

Starting commit: `31d112488ec99c62236c90c0d1290e0dfc33a7eb`. Implementation commits: `37e04e3` (policy, official clients, boundaries) and `4a169b95ee64a15c030ddca9a360cbf14d8a924c` (integrated parent/child experience and regressions); this documentation commit follows. Branch `codex/familysafe-live-validation`; existing [draft PR #1](https://github.com/Saifee16/alpha/pull/1), base `feature/familysafe-loop5`. Preserve history; no merge, force-push or public APK publication.

The core web learning experience is implemented. Essential low-maintenance non-NASA video discovery is still missing, so the overall V1 goal is not functionally complete. Adult-led physical evaluation can proceed with the checklist, but independent child-safe deployment is not verified.

- [x] Preserve baseline history, local schema, PIN, attempt delays, requests and individual video decisions; no family data reset or silent source re-enabling.
- [x] Parent custom topic rename/remove, phrases/synonyms, blocks, website associations, explicit community-source consent, request review, scoped reset/default confirmations, PIN/security status and local rule preview. Topic Save and rule Test clear editor focus; real-touch regressions verify navigation afterward. Disabled YouTube setup controls are hidden. Optional export/import omitted.
- [x] Bounded whole-query policy: at most one finite educational prefix/suffix around a full approved niche, typed decisions, blocked precedence, Unicode/length validation and mixed/unknown rejection. Canonical approved niche alone goes to providers.
- [x] Official Vikidia/Wikijunior structured search: one page, five results/source, all-token title relevance, validated title-only cards, no thumbnails/snippets/unrestricted fallback. Existing installs add missing sources disabled through parent Websites.
- [x] Exact selected-article routing and official Parse API text reader with source/revision/license attribution, responsive text and strict CSP. Other articles, descendants, API/edit pages, social/short-form/known adult roots and external apps remain denied. Generic approved directories retain their explicit host/path scope. No global JavaScript/storage or upload-host exception.
- [x] Final JVM/APK/lint gates and signature verification; selected disposable-emulator flows and independent reviews completed. Prior failures retained rather than hidden.
- [x] Installation/privacy/provider limits and non-destructive physical checklist updated.
- [ ] Verify a practical non-NASA educational video source/collection under suitability, rights, API, format and playback-isolation requirements.
- [ ] Physical-tablet install/upgrade, reboot, rotation, latency, network recovery, household Family Link/Android protections and actual adult content/rights review: NOT RUN.

### Actual verification

Delivery gate `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`: PASS, 58s, 84 tasks (14 executed/70 up-to-date). **111 JVM tests/17 classes, 0 failures/errors/skips. Both APKs built. Lint 0 errors/13 warnings. App and test signatures verify; app v2, same existing debug certificate.**

**23 distinct selected emulator scenarios passed across overlapping batches**, including eight FamilyFlow, six BrowserDevice, two new parent tests, one live provider test, five existing approval fixtures and one real native-player smoke test. Final editor-focus/parent/reader/phrase-save batch: explicit **OK (4 tests)**, 53.611s, no skips. This is not one clean full-suite run. The first broad 25-method run had seven failures plus three live-NASA skips; after dismissing an observed persistent System UI ANR dialog and correcting the stale blocked-request assertion, a later run had three failures plus three skips. Nested-scroll helpers/renamed heading were corrected; remaining editor-focus failure was fixed in production and passed the final regression. Do not erase those intermediate results or count assumptions as passes.

Independent QA reproduced an exact-article descendant-path escape before the equality fix; the red/green regression remains. Independent review of the final API reader found no confirmed safety bypass. A dedicated hostile-article-body CSP runtime case was not added; existing real WebView tests cover restricted capabilities and off-source navigation. Closing/destroying the reader does not immediately cancel its locally owned HTTP call; it may continue to its 20-second budget. Worker-exit timing has no hard real-time assurance.

Live Android API results: Vikidia `animals` → Animal; `science` → Science/Computer science; `mathematics` → Mathematics; `geography` → Geography; `football` → Football/Association football. Wikijunior returned animal, science and mathematics titles; geography/football full-text hits were rejected and yielded empty results. Both Animal and Wikijunior Animal Kingdom readers returned HTML200 with body/headings/CSP/attribution. Vikidia's final screenshot is visually readable; Wikijunior visual review is NOT RUN. Images and interactive learning remain unavailable. Specific multiword niches can legitimately have no matches.

The existing native reference test downloaded and played a 265645ms video, verified pause/seek/completion/background release/temp-file deletion, and captured a visible launch frame. It did not approve a catalog entry or verify dynamic discovery. Three opt-in live NASA review/discovery scenarios were not run beyond assumption skips. Do not spend another discovery loop on NASA. Commons access failed during bounded research and codec validation remains open; Archive metadata did not establish an appropriate compatible rights-reviewed collection. No collection approval or safety relaxation was introduced.

Device: dedicated API35 ranchu `FamilySafe_Tablet_API35`, WebView124.0.6367.219, emulator-5560, temporary read-only disk overlay/no snapshot save. Same-signature `install -r`; no physical device/family configuration touched. Test PINs and review hashes were disposable fixtures, never adult content approvals.

### Local APK delivery

- App: `D:\Alpha\FamilySafe\app\build\outputs\apk\debug\app-debug.apk` (debug, package `family.safe.debug`, Android 8.0/API26 minimum).
- SHA-256: `5d06d4865bf3765f5e74439e1d5bcb831ad96a43781ef58a9edbdaf9cbec259f`.
- Signing certificate SHA-256: `f88398895f95d7a226ff8a38bd96197686fbf6cf2cf88b3acd1f14836b332444`; app v2 verification PASS, test signature PASS. Private key stays outside source control.
- Test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, not required for household installation.
- Evidence remains ignored under `app/build/v1-evidence/`: delivery-build.txt, delivery signatures, final-source-results.json, runtime-v1.txt/runtime-final.txt/runtime-corrected.txt/runtime-delivery.txt, v1-animal-reader.png and nasa-player.png. See TEST_MATRIX.md for exact distinctions.

Use README.md and PHYSICAL_DEVICE_CHECKLIST.md for manual serial selection, checksum/signature comparison and `install -r`. Stop on mismatch; never uninstall/clear data to recover an upgrade. Never run disposable/reset instrumentation on a family tablet. Both providers are community edited, approved queries leave the device, and source trust is not semantic moderation. Adult review and household controls remain necessary.

Next priority: one evidenced non-NASA video integration or defensible collection policy, then the supervised physical checklist. Keep National Geographic Kids and YouTube child playback disabled; no unrestricted fallback or automatic video approval.

## Previous verified milestone: controlled multi-subject browser foundation

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
