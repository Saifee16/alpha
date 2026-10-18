# Test matrix

## V1 completion gate — 2026-10-18

Classification: **supervised debug pilot with one mathematics video; physical deployment/overall V1 partially complete**. Starting commit `4a6b7ee73967ae23fe4243dfd67bcceaf6e80d04`. Evidence below supersedes earlier current-status statements, while preserving historical failures/results.

| Check | Actual result | Evidence and limit |
|---|---|---|
| Clean JVM execution | PASS | `cleanTestDebugUnitTest testDebugUnitTest`: 128 tests/19 classes; 0 failures/errors/skips. XML under app/build/test-results/testDebugUnitTest |
| App/test APKs, lint | PASS | final-build.txt: 5m14s, 85 tasks (19 executed/66 up-to-date); both APKs. Lint 0 errors/13 warnings. Local SDK metadata-version warning remains |
| Signing and upgrade key | PASS | final-signature.txt/final-test-signature.txt; same existing certificate SHA-256 f88398895f95d7a226ff8a38bd96197686fbf6cf2cf88b3acd1f14836b332444; app v2 |
| Core integrated regression batch | PASS | integrated-web-runtime.txt: explicit OK (17 tests), 207.513s, no skips: V1ParentDeviceTest 2, EducationalSearchDeviceTest 1, FamilyFlowTest 8, BrowserDeviceTest 6 |
| Commons then one alternative | BOUNDED | Commons returned no candidate after Windows DNS/TLS failures. Sole alternative Internet Archive returned actual structured search/metadata and the exact Addition 3 MP4. No general repository approval |
| Source/rights/codec verification | PASS with limits | Exact original-ID official publisher metadata, publisher CC BY-NC-SA 4.0 policy, actual 7,794,483-byte file/hash, ffprobe H.264 Constrained Baseline/AAC-LC, sampled watermark/lesson frames. No per-item Archive license field; full adult content/credits/rights review NOT RUN |
| Actual native codec probe | PASS | archive-native-runtime.txt: OK (1 test), 8.316s. Matching local file: progress, pause, seek/resume, stop; 537403ms. Separate from product integration and approval |
| Archive initial production batch | 1 PASS / 1 FAIL | archive-integrated-runtime.txt: 2 tests, 1 failure, 157.822s. Real consent/discovery/pending denial reached preview; transfer failed SocketException, no playback. Late discovery could not cross parent lock or approve itself. Original failure retained |
| Archive controlled live rerun | PASS | archive-live-rerun.txt: OK (1 test), 103.003s, no skips. Real source consent cancel/confirm, production API discovery, pending child denial, exact-hash native parent preview, Close/temp cleanup, explicit engineering fixture approval, real child playback, background stop/lock/hash clearing/private-file deletion, source disable/revocation. Not adult content approval |
| Existing individual-review regressions | PASS | legacy-review-runtime.txt: OK (5 tests), 89.352s, no skips. Pending approval/rejection persistence, late discovery/private candidates, PIN/confirmation/topic bypass denial, preview lock/recreation/background and rotation/control reachability. No live NASA discovery or expansion |
| Independent security review | No confirmed regression | Reviewed provider, identity/rights bounds, URL/length/hash gates, shared model/migration, source/topic authorization, parent confirmation and reused lifecycle/cancellation. Does not prove content suitability or physical worker timing |
| Emulator obstruction | Observed | com.android.systemui ANR captured in archive-systemui.png and earlier probe screenshot; Wait selected, successful live rerun afterward. Cause of initial SocketException remains unresolved; do not attribute it to the ANR without evidence |
| Physical tablet, adult review, household controls | NOT RUN | No physical device connected. All resets/fixture approvals used selected API35 ranchu temporary read-only/no-snapshot overlays only. Touch, both orientations, install/upgrade, PIN latency, reboot, network recovery and household protection configuration require PHYSICAL_DEVICE_CHECKLIST.md |

**25 distinct selected emulator scenarios passed across these separate/overlapping batches**, with the initial live failure retained. APK SHA-256: `9e8ad806b40c7e8d4735e069f4bd006e48a7c6fa5aef02bd0d370ab37a2ed399`. Raw evidence remains ignored under `app/build/video-gate/`. Test approval is a deliberately simulated consent fixture; inclusion, discovery, codec success and this test do not authorize a family child's access. No clean full instrumentation-suite claim.

## Previous V1 continuation — historical evidence

Readiness: **PARTIALLY COMPLETE**. The multi-subject web path works; low-maintenance non-NASA video discovery is missing and physical-tablet checks are NOT RUN. Source/fixture checks do not certify content suitability.

| Check | Actual result | Evidence and limit |
|---|---|---|
| Delivery JVM gate | PASS | 111 tests / 17 classes; 0 failures, errors or skips. Final XML in app/build/test-results/testDebugUnitTest |
| APKs and lint | PASS | delivery-build.txt: 58s, 84 tasks (14 executed/70 up-to-date); both APKs built; 0 lint errors/13 warnings |
| App and test APK signatures | PASS | Both verify; app v2; same debug certificate as prior installation. delivery-app-signature.txt and delivery-test-signature.txt |
| Eight FamilyFlow and six BrowserDevice methods | PASS | runtime-final.txt; full-query denial, phrase save, request persistence, PIN/throttle/lifecycle and real WebView/transport restrictions. These 14 methods passed after the stale blocked-request assertion was corrected |
| Actual educational search | PASS | Both sources, five queries each; real Android API responses. Vikidia: Animal; Science/Computer science; Mathematics; Geography; Football/Association football. Wikijunior: animal/science/mathematics titles; geography and football empty after strict relevance filtering |
| Both article API transports | PASS | Restricted reader returned HTML200 for Vikidia Animal and Wikijunior Animal Kingdom, with headings/body, CSP and source/revision/license attribution. final-source-results.json. Wikijunior visual inspection NOT RUN |
| Live child source-consent/search/read/return/background flow | PASS | runtime-corrected.txt. Cancel consent stays disabled; explicit approval enables; real Animal opens; Back retains results; background clears them. v1-animal-reader.png visually readable, no wiki shell/system dialog; images intentionally unavailable |
| Five existing video-approval fixtures | PASS | runtime-corrected.txt: pending/rejected persistence, late discovery lock, preview/lifecycle, direct-call eligibility and rotation. Fixture review hashes are not adult content/rights approval |
| Native reference player smoke | PASS | runtime-corrected.txt: 265645ms video; real download/play/pause/seek/completion/background release/temp-file deletion. nasa-player.png visually shows a launch frame. No catalog approval; not dynamic discovery or physical-device proof |
| Final editor-focus/dashboard and phrase-save regression | PASS | runtime-delivery.txt: OK (4 tests), 53.611s, no skips; strict real-touch navigation, focus clearing, rule outcomes, reset Cancel/Confirm and authentication/request preservation |
| Exact selected-article boundary | PASS | Independent QA reproduced descendant-path escape red; exact equality fix and V1SecurityTest retain it. Scripts/forms/storage/popups/external schemes and generic unapproved hosts remain denied |
| Independent reader review | No confirmed bypass | Exact official URL/title checks, bounded parse, deny-by-default CSP and native WebView capabilities reviewed. Dedicated hostile-body CSP runtime case not added; existing browser fixtures cover capability and navigation restrictions |
| Physical tablet, household configuration and actual adult reviews | NOT RUN | No physical device connected; use PHYSICAL_DEVICE_CHECKLIST.md. No unsupervised-readiness claim |

Runtime history is preserved, not counted as one clean suite: initial 25-method batch had 7 failures and 3 live-NASA assumption skips. A persistent System UI ANR dialog obstructed UI input; later batch after dismissal had 3 failures and 3 skips (nested-scroll navigation and a renamed heading). Corrected 9-method batch passed 8, failed the remaining parent navigation case; editor focus was then addressed in production and the final focused run is recorded above. No safety assertion was removed. The blocked-query test now correctly requires no parent-request button for blocked content. All 23 distinct selected runtime scenarios passed across overlapping batches after corrections; final focused 4/4 passed. This is not one clean full-suite run. Three opt-in live NASA review/discovery scenarios remain unexecuted beyond assumption skips; the separate bounded native reference test passed.

Device: API35 ranchu AOSP tablet, WebView124.0.6367.219, dedicated FamilySafe_Tablet_API35 on emulator-5560, temporary read-only disk overlay/no snapshot save. Existing AVD/family data preserved; same-signature install -r used. Never run resetting suites on household hardware. Evidence remains ignored under app/build/v1-evidence; no private logs/APKs committed.

Reader API operations may continue until their 20-second bound after WebView Close/background; immediate transport cancellation/worker exit is not established. Search cancellation and stale-generation guards remain separate. Broad approved directories expose their explicit host/path scope; article body content is not semantically moderated. Lint warnings: one AGP version notice, two dependency notices, one existing debug JavaScript warning, eight existing debug TextView notices and one player constructor warning.

## Earlier iteration evidence (historical)

## Current core-browser verification

Implementation: `af23c6319c04959abc6fc8d03515cd3018f4470f`, based on verified Loop 5 `41a0c306`. These results are fresh; historical sections below remain separate.

| Scenario | Result | Evidence / limit |
|---|---|---|
| Full JVM suite | PASS | 89 tests/14 classes; 0 failures/errors/skips; actual XML summarized in `core-jvm-summary.json` |
| Finite parent phrase suggestions | PASS | 7 JVM tests: explicit save, exact whole-query matching, category/disabled gates, unknown/mixed/URL/audience denial, blocks, Unicode, idempotence and atomic size limits |
| Denied-query website regressions before fix | FAIL as expected | Both new device tests failed at website buttons remaining visible; `core-red-runtime.txt`. Helper save/restart and positive category routing passed before the denial assertion |
| Core parent/search + browser device batch after fix | PASS | Explicit `OK (8 tests)`, 61.364s, no skips: two new FamilyFlow methods plus six BrowserDevice methods; `core-final-runtime.txt` |
| Both APK builds / signatures | PASS | Final full build 3m2s, 84 tasks (14 executed/70 up-to-date); app and test signatures verified, app v2; existing emulator signing certificate matched before upgrade |
| Lint | PASS | 0 errors/17 warnings: 7 version notices, existing debug JavaScript notice, programmatic player constructor and 8 debug TextView notices; no new security-critical findings |
| Cancellation with blocked/throwing disconnect | PASS (JVM) | New real-operation fixture checks terminal state and nonblocking cancel; no Android worker-exit/timing guarantee |
| Preserved NASA transport experiment | PASS (host HTTPS) | API/manifest/HEAD/full MP4 HTTP200; reference 11,103,471 bytes and SHA match; not app discovery or playback proof |
| Earlier selected emulator batch with live NASA opt-in | 9 PASS / 1 FAIL | `focused-runtime.txt`: new live test returned no eligible playable candidates; failed before preview. No approval. Cause unestablished; investigation closed on priority correction |
| Current native preview / approved child playback | NOT RUN to completion | Live discovery failed before preview; historical Loop3 evidence below is not a current pass |
| Full instrumentation suite / physical tablet / adult content and rights review | NOT RUN | Only selected emulator methods executed; physical checklist still open; no unsupervised-readiness claim |

Final emulator: API35 AOSP x86_64 tablet, `ranchu`, AVD `FamilySafe_Tablet_API35`, WebView 124.0.6367.219. The lead launched an isolated `-read-only -no-snapshot` instance on port 5560; original AVD data was preserved. Test APK upgrades used `install -r` after certificate comparison. Raw evidence remains ignored under `app/build/live-validation-evidence/`; JVM XML remains under `app/build/test-results/testDebugUnitTest/`.

Debug APK SHA-256: `86ea78ae622001e97a02558c7cc9ed16b83d5f06212c22ca57805abbdcdec431`. Final full Windows command, using the README's workspace JDK/SDK/cache paths:

```powershell
.\gradlew.bat --offline --daemon --max-workers=4 testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

Reproduce the selected core batch **only after independently verifying the intended dedicated disposable emulator** and installing the matching verified APKs. The two FamilyFlow methods reset debug settings; never run this command on a family tablet or substitute its serial. The core batch has no live NASA opt-in and grants no video approval.

```powershell
adb -s emulator-5560 shell am instrument -w -r -e disposableInstrumentation true -e class 'family.safe.FamilyFlowTest#deniedQueriesHideApprovedWebsiteResults,family.safe.FamilyFlowTest#parentSuggestedPhrasesNeedSavingAndPreserveQueryBoundaries,family.safe.BrowserDeviceTest' family.safe.debug.test/androidx.test.runner.AndroidJUnitRunner
```

The six browser cases overlap with the earlier 10-test batch; do not add the batches to claim distinct coverage. Website results remain a category directory, not within-site search results. Pending videos remain inaccessible; suggestions and source labels do not certify content suitability or rights.

## Historical cloud Loop 5

| Scenario | Result | Evidence / limit |
|---|---|---|
| Fresh JVM suite / both APKs / lint | PASS | 81 tests/13 classes; 0 failures/errors/skips; both APKs built; lint 0 errors/12 unchanged warnings; 1m5s, 84 tasks (21 executed/63 up-to-date) |
| Whole-operation deadlines/cancellation fixtures | PASS (10 new JVM tests) | Includes actual scheduled expiry with blocking fake I/O; discovery 60s/player 120s budgets; native prepare remains separate 30s; no Android timing proof |
| Live API GET and canonical asset HEAD | BLOCKED | HTTP 403 at proxy CONNECT; curl 56 (initial strict attempt also 22/403); no provider JSON/media obtained |
| Configured host list | OBSERVED; runtime unknown | Four NASA hosts configured; `network.state=unknown` is not proof of effective access; settings unchanged |
| Emulator / instrumentation / physical runtime | NOT RUN | All 21 instrumentation methods compiled, none executed; Android cleanup timing and historical preview timeout unverified |

Current APK: `app/build/outputs/apk/debug/app-debug.apk`, v2 signature PASS, SHA-256 `59be53e95fcdd6548e538c8e35a385c56f3841b8b74ffaa095f412dc0fbe6648`. Test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Build log, JVM summary, live-access log and proxy response headers remain ignored in `app/build/loop5-evidence/`. Deadline/cancellation marks terminal state immediately and queues best-effort disconnect on separate two daemon threads with queue capacity 16; Android cleanup has no hard real-time guarantee. No push or publication occurred. Successful fixture checks must not be reported as live NASA or Android runtime verification.

## Historical cloud Loop 4

| Scenario | Result | Evidence / limit |
|---|---|---|
| Onboarding baseline | PASS (before Loop 4) | 54 JVM tests; both APKs; lint 0 errors/12 warnings |
| Loop 4 JVM suite / both APKs / lint | PASS | 71 tests / 12 classes; 0 failures/errors/skips; both APKs built; 0 lint errors/12 warnings; 42s, 84 tasks (19 executed/65 up-to-date) |
| Dynamic discovery/library fixtures | PASS (JVM) | 8 discovery + 6 library/migration/adversarial tests; bounded JPL/video search, metadata/assets, parent/category gates, persistence and digest eligibility |
| Prepare timeout and stale callbacks | PASS (3 JVM tests) | Attempt-state regression coverage; Android watchdog behavior NOT RUN; historical timeout cause unresolved |
| Live NASA API / asset / playback | BLOCKED / NOT RUN | Cloud policy requires `images-api.nasa.gov` and `images-assets.nasa.gov` |
| Cloud emulator / instrumentation runtime | NOT RUN | 21 source tests compiled, including late-lock discovery fixture; no `/dev/kvm` or supported device available |
| Physical-tablet checks / actual rights review | NOT RUN / HUMAN REVIEW REQUIRED | Follow [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) |

Loop 4 APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `fcdc46df32d05fc33af9a18bef5a224ffbb5e98995149000d2e526c1f1a6cd0e`, v2 signature PASS. Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Evidence: ignored `app/build/loop4-evidence/final-build.txt`; JVM XML under `app/build/test-results/testDebugUnitTest/`; lint XML/text under `app/build/reports/`. Warnings are unchanged: 2 dependency notices, 1 isolated debug YouTube JavaScript notice, 1 programmatic player `ViewConstructor`, 8 debug NASA `SetTextI18n` notices. Scoped source/fixture security review found no new bypass; native runtime remains unverified.

Loop 4 must not be presented as device-verified. Retain the Loop 3 overlapping-batch results and unexplained preview timeout below as historical evidence. The new media-prepare watchdog bounds one failure mode; it does not establish why the earlier live flow timed out.

## Historical build and JVM evidence

Loop 3 baseline `eb14e4b`: rebuild PASS, 45 JVM tests, debug and Android-test APKs compiled, lint 0 errors/16 warnings, 1m18s. Final build: **PASS**, 54 JVM tests, 0 failures/errors, both APKs built, lint 0 errors/17 warnings, 5m22s; 84 tasks (19 executed/65 up-to-date), `app/build/loop3-evidence/diagnostic-build.txt`. An intermediate verified build took 2m33s with the same counts. Warnings: 7 dependency notices, 1 isolated debug YouTube JavaScript notice, 8 debug NASA-spike `SetTextI18n` notices and 1 `ViewConstructor` notice for programmatic-only `NasaPlayerView` (never XML-inflated). That constructor warning is contextual, not a security finding.

Historical Loop 3 APK: 11,267,063 bytes; SHA-256 `4eec09be21c007871a3e76b6c0d59f0b75e5ef1ebe4cb496fbc1f3fbd94f3cd9`; v2 signature PASS. Loop 3 raw evidence/artifacts stay ignored under `app/build/loop3-evidence/`.

## Historical Loop 3 verification

| Scenario | Result | Required evidence |
|---|---|---|
| Final JVM suite / app and test APKs / lint | PASS | 54 JVM tests; both APKs built; lint 0 errors/17 warnings; 5m22s |
| First full runtime run | 17 PASS / 3 FAIL of 20 | 762.45s; `app/build/loop3-evidence/full-runtime.txt`; failures detailed below |
| New review authentication gate, persisted decisions/rejection and rotation | PASS in first full run | Fixture assertions, not actual human content/rights review |
| Focused runtime rerun | 7 PASS / 1 FAIL of 8 | 577.044s; six review-flow checks plus two corrected family-flow checks; live preview timed out |
| Corrected authentication and offline recovery | PASS in focused run | Stale authentication/wrong PIN/throttle; offline denial then same-video Retry; original data/Wi-Fi states restored |
| Final live review → approved SHA → child playback | PASS, 1/1 | 145.835s; `live-diagnostic-runtime.txt`: both actual previews; Europa child pause/seek/resume/completion/background |
| Runtime coverage total | All 20 distinct scenarios passed across batches | Initial 17 + focused 7 + final 1 overlap; **not a single clean 20/20 run** |
| Final process restart | PASS | PID 4744 → 4788, identical settings hash, query visible and parent session absent; `final-process-restart.txt` |
| Final crash/ANR diagnostics | PASS for captured buffers | Crash buffer 0 lines; Android: no ANR since boot |
| Physical installation/usability/network recovery | NOT RUN | No physical tablet in ADB inventory; [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) |
| Actual content/credits/rights and device protections | PENDING HUMAN REVIEW | Adult review and observed Family Link/Android controls; no automatic guarantee |

`FamilyFlowTest` and `NasaReviewFlowTest` reset debug settings with explicit `disposableInstrumentation=true`. Never run them on a family tablet or installation containing settings to preserve; the opt-in does not establish device identity. Select the dedicated disposable emulator explicitly. The physical checklist permits only separately reviewed non-resetting subsets.

The first full run completed **17 PASS / 3 FAIL of 20**, 762.45s (`app/build/loop3-evidence/full-runtime.txt`). Two `FamilyFlowTest` authentication checks (stale/background authentication and wrong-PIN throttling) timed out in the generic 15s saved-state wait before expensive hashing finished. They now use a separate 60s authentication-finished wait, consistent with existing setup/unlock deadlines; the generic write wait remains 15s. Both passed in the focused rerun, with measured emulator authentication waits around 9.7/10.2/10.6s. All security assertions and PBKDF2's 600,000 rounds remain unchanged. Physical-tablet PIN latency is NOT RUN.

The third failure did not establish an offline condition: disabling emulator Wi-Fi left MOBILE HSPA/`eth0` fallback. The corrected test, guarded to `ranchu`/`goldfish` disposable emulator hardware, disables both data and Wi-Fi and restores their original states in `finally`. Offline denial and same-video Retry passed; final state was mobile data `1` and Wi-Fi enabled. Do not perform these network mutations on a family tablet.

The focused eight-test rerun finished **7 PASS / 1 FAIL**, 577.044s: the live flow's first Europa preview timed out at 180s. Its cause remains unexplained; do not claim a diagnosed fix. After the final diagnostic build, that single live flow passed **1/1 in 145.835s**, previewing both Europa and NISAR and then exercising Europa child approval/playback. Thus all 20 distinct scenarios have passed across overlapping batches; this is not a single clean 20/20 run. A minor parent-only action restores a previously removed Space topic disabled, with a regression check; the hint now distinguishes Science from Space. Review fixtures are not actual human content/rights approval.

The first native screenshot was black after clearing `FLAG_SECURE` before compositor update. Final captures waited 250ms: `nasa-approved-child-player.png` visibly showed a spacecraft frame. Parent-preview captures had ready status but opening black frames, so are not visual playback proof. The offline error/Retry screen, parent landscape and child portrait layouts were readable. NASA rendered fully and BBC navigation was visible; National Geographic Kids remained blank and fails usability. Final inventory showed only the emulator. The process-restart UI dump initially returned a null root; a wake/start retry succeeded even though power state was already Awake. Preserve the retry in `final-process-restart.txt`; do not infer the device had been asleep.

An earlier partial attempt was aborted after a 60,000ms PIN-setup timeout while building concurrently; approximately two later checks passed before abort. That incident is separate from the completed full, focused and final single-test batches.

After independently confirming `emulator-5554` is the intended disposable AVD, install the built app and test APKs and run all tests explicitly against it:

```powershell
adb devices -l
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r -e disposableInstrumentation true -e nasaLive true -e liveSites true family.safe.debug.test/androidx.test.runner.AndroidJUnitRunner
```

This command destroys that debug installation's settings and uses live network data. Without the two live opt-ins, those tests skip under assumptions rather than establish live behavior. Never substitute a family tablet serial.

## Earlier baseline emulator evidence

Device: API 35 / Android 15 AOSP x86_64 tablet, 1280×800, 160 dpi, WebView 124.0.6367.219, WHPX. No physical device was available. Instrumentation reset requires the debug package plus `-Pandroid.testInstrumentationRunnerArguments.disposableInstrumentation=true`; it clears app-private settings and does not verify emulator identity. Use the flag only on a dedicated disposable emulator.

| Scenario | Result | Evidence |
|---|---|---|
| Family flows and browser restrictions | PASS, 12 tests | `FamilyFlow` 6 + `Browser` 6; JUnit `OK (12 tests)`, 200.958 s |
| Isolated NASA playback | PASS, 1 test | JUnit `OK (1 test)`, 23.314 s; media duration 265,645 ms; pause/resume, seek, completion, and background release observed |
| Live-site transport/render smoke | PASS, 1 test | JUnit `OK (1 test)`, 28.799 s; all sites HTTP 200 |
| Process restart and settings persistence | PASS | PID 3172 → 3216; settings hash identical; child query visible and parent session absent after restart |
| Android runtime fatal-error log | PASS | 0 lines; no `FamilySafe FATAL` entry |
| Instrumentation total | PASS, 14/14 | 12 + 1 + 1 tests; all assertions passed |

Site response sizes/request counts: NASA Space Place 21,985 bytes / 54 requests; National Geographic Kids 207,802 bytes / 1 request; BBC Bitesize 629,965 bytes / 1 request. Visual review: NASA rendered fully and BBC navigation was visible. National Geographic Kids rendered blank and **fails usability review** despite HTTP 200; keep it disabled. Do not relax JavaScript or browser policy to make it render.

Emulator logs are under ignored `app/build/device-evidence/*.txt`; screenshots are under ignored `evidence/*.png`.

## Earlier baseline unit and static checks

| Scenario | Result | Evidence |
|---|---|---|
| Exact approved phrases, normalization, blocked terms, URL scope, redirects | PASS | Included in 45 JVM tests; URL resolution tests are pure policy tests |
| PIN verification/throttling, settings persistence and corruption handling | PASS | PIN and settings store tests |
| Bounded YouTube discovery and metadata checks | PASS | Fixture tests only; no live API/key |
| App and Android-test APK builds | PASS | Most recent fully verified wrapper run |
| Lint | PASS | 0 errors; 16 warnings as detailed above |

YouTube API calls are blocked until child-directed setup and restricted credentials exist. Child YouTube playback remains blocked by design. Passing instrumentation does not make the blank National Geographic Kids page usable or establish behavior on a physical tablet.
