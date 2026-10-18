# FamilySafe UX fix pilot — review before merge

Status: **SUPERVISED PILOT; human UX approval pending. Video discovery PARTIAL.** No merge, public APK publication or replacement of the previous release is authorized.

Base: `18b4bc7cb2bbd0ff9d5c1ae085c862c6dd9c5059`; branch: `codex/familysafe-product-rescue`.

| Final delivery field | Value |
|---|---|
| Exact build commit | `bf1d0968dad88c4f588fad909ac31098140a5540` |
| APK | `FamilySafe-V1-UX-Fix-Pilot.apk` — private review artifact |
| APK SHA-256 | `e604f7cf1682b73ec9ed414bd917e3c0bd4a6ebf0d1afad3a7a0ee5cc307a1f8` |
| Package | `family.safe.debug` |
| Signing-certificate SHA-256 | `f88398895f95d7a226ff8a38bd96197686fbf6cf2cf88b3acd1f14836b332444` — final signature verified |
| Verification | 148 JVM pass; APK/test builds pass; lint 0 errors/10 warnings; 34-device run 30 pass/4 fail, 3 failures resolved by focused passes; live NASA discovery still failed |

## What changed

Discover starts with a question field and Read search, without the parent's topic, phrase, block or website directory. Actual result cards show their source for attribution. The parent entry still requires the PIN. Set up reading offers explicit subject and community-source consent inside parent settings; existing controls remain available.

Independent Vikidia and Wikijunior requests run concurrently with separate cancellable clients and a 12-second whole-search deadline. The first validated provider results publish without waiting for the other. New input, new searches and backgrounding cancel previous work; opening an article preserves foreground results for Back. Whole-query authorization, blocked-term precedence and exact destination/path restrictions remain active. Structured snippets help relevance; they do not certify suitability. Community-edited reading still requires adult supervision.

Watch searches the individually approved local video library. It appears only when eligible approved videos exist. It does not discover arbitrary videos from a child's query. Parents discover and review candidates separately; pending or rejected videos never become child recommendations.

## Actual API 35 emulator search observations

The following are predetermined engineering fixture searches from `live-run1/rescue-live-timings.json`, not logged household queries. All ten returned real structured-provider results. Counts include weaker metadata matches; examples such as Poland for football and Water for fractions show that relevance is imperfect. This table does not label every returned article suitable for a child.

| Search | Results | Example actual title | First result state (s) | Whole search (s) | First displayed observed (s) |
|---|---:|---|---:|---:|---:|
| Animals | 9 | Animal | 5.600 | 6.263 | 18.399 |
| Dinosaurs | 10 | Dinosaur | 2.449 | 4.346 | 9.293 |
| Planets | 10 | Planet | 2.077 | 2.574 | 7.060 |
| Solar system | 10 | Solar System | 1.687 | 2.986 | 5.814 |
| Mathematics | 10 | Mathematics | 1.959 | 2.640 | 5.083 |
| Fractions | 10 | Wikijunior:Introduction to Mathematics/Fractions | 1.401 | 2.696 | 5.358 |
| Geography | 9 | Geography | 1.544 | 1.762 | 3.211 |
| Continents | 10 | Continent | 1.490 | 1.899 | 3.445 |
| Football | 4 | Football | 1.173 | 1.343 | 2.678 |
| Football rules | 1 | Football | 1.636 | 1.712 | 2.794 |

First result state measures the monotonic production search interval; displayed observation includes UI interaction, test synchronization and rendering, and is not a pure provider latency. The real Vikidia Animal article rendered, scrolling/readability checks passed and Back retained results; article observation took 6.174 seconds. Vikidia served sports in this fixture; Wikijunior sports was not enabled.

**Performance is not passed.** Input-to-draw observations ranged from 97–539 ms (state assignment 0–1 ms), missing the 100–200 ms goal on several attempts. Three corrected-build cold launches reported Android `TotalTime` of 11.114, 11.739 and 10.939 seconds. The previous APK had two launch timeouts and one 12.322-second launch; that is insufficient evidence for a reliable launch-speed improvement. First-frame observations vary strongly with emulator load: 312 ms in one startup fixture, 7.109 seconds in the ten-search run. PIN verification in the upgrade fixture took **28.026 seconds**; production unlock during the video run took **27.658 seconds**. PBKDF2 remains at 600,000 iterations. Settings are no longer rewritten merely by startup. Emulator load does not establish physical-tablet latency, and slow PIN/startup remain material usability risks.

A second run on the final APK again returned 9, 10, 10, 10, 10, 10, 9, 10, 4 and 1 results for the same queries. First validated result state ranged from **1.061–2.498 seconds**, input drawing from **117–359 ms**, and observed first displayed cards from **2.347–5.530 seconds**. Animal article opening took **6.157 seconds**. Its warm activity fixture measured first draw **455 ms** and settings readiness **334 ms**; these are not cold process-launch timings. The ten searches/article opened, but the earlier final Back assertion failed because Animal was below the lazy list viewport when Wikijunior arrived first. The corrected test checks retained result data and scrolls before asserting visibility; its focused rerun passed. No child searches or family settings were logged.
A third focused run also passed all ten searches and actual article/Back retention. First result state ranged from **1.419–3.446 seconds**, input drawing from **133–356 ms**, and article opening took **6.052 seconds**. These repeated measurements remain variable; none certifies tablet performance.

## What video actually works

Internet Archive discovery is restricted to three exact Khan Academy snapshots: **Addition 3**, **Multiplication 2: The Multiplication Tables**, and **Fractions in lowest terms**. Other uploads and collections are unavailable. This is a finite Mathematics catalog, not broad subject coverage. Source/topic permission, individual parent review, exact identity/length, pinned integrity and approved SHA-256 remain required; there is no external playback fallback or automatic collection approval.

Both added files completed download/integrity verification and full desktop decode. API 35 native probes verified H.264/AAC playback, progression, pause, seek, resume and stop for Fractions (341.078 seconds) and Multiplication (927.312 seconds).

`final-device/rescue-fractions-video-result.json` records production Fractions discovery, pending isolation, preview, simulated individual approval, child playback/controls, background cleanup and revocation **PASS**. Preview transfer/preparation took 9.404/0.220 seconds; the separate child playback transfer/preparation took 15.943/0.246 seconds. This used **simulated engineering consent**; `humanContentRightsReview=false`. It does not approve the lesson for any family. Archive transfers were intermittent during investigation; incomplete downloads failed closed. Multiplication's local native probe is not a claim that its full live parent flow passed.

Parent maintenance remains individual: enable the topic/source, search an approved phrase, preview the complete lesson, review attribution/license/content, then approve that exact digest. New or changed material requires another review. The publisher reuse policy and attribution/noncommercial conditions still require the parent's attention; The Archive items lack an individual license field; publisher reuse policy does not replace adult rights/content review. Broader video coverage remains **PARTIAL**.

## Verification boundaries

Current verification passed **148 JVM tests**, debug/test APK builds and lint (**0 errors, 10 warnings**). The final rebuild passed all four verification tasks; the delivered APK includes the device-verified parent focus fix at `bf1d096`. The first full device run caught a real deferred LazyColumn null-decision crash; the production fix captures the rendering decision, while click handlers still authorize against current settings/query. Its deterministic repeated query-edit/stale-action device regression passed.

The subsequent API35 regression ran **34 tests: 30 passed, 4 failed**. Two assertions were corrected: an offscreen lazy result after Back and an authorized custom-site card that the old test incorrectly expected hidden. Both passed their focused rerun. The new reading setup consent/scope/preservation UI test also passed. A third failure was live NASA discovery returning no eligible new candidates; no approval/access changed and this remains an unresolved live-provider failure. The fourth was a parent rule-test field retaining focus; this reproduced in the focused rerun. A minimal focus-dismissal fix passed the full affected parent rule-preview/reset/preservation device flow (1 test, 62.18 seconds). Existing native video playback, offline retry, parent locking/throttling, rotation, actual article reading/background cleanup and all six browser security tests passed. Isolated 600,000-iteration PIN verification took **18.315 seconds** in this later run, still unacceptably slow. These results are not a clean full-suite pass.
The real old-to-new in-place upgrade fixture preserved exact settings bytes and timestamp, verified the existing PIN and retained 600,000 iterations. This synthetic fixture did not read or reset family settings. Actual screenshots and raw logs accompany delivery. API 26 testing: **NOT RUN** (image unavailable). Physical-tablet validation, touch behavior and family protection configuration: **NOT RUN**. Use [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) for the supervised tablet session; never run resetting instrumentation on family hardware.

## Windows installation and rollback

Use the final reviewed private APK and its final SHA-256 above. In PowerShell, select the actual physical tablet and follow the checklist's manufacturer/model/API and installed-certificate comparison before installation. Keep the original APK. Never uninstall, clear data, change signing keys or add a downgrade flag to bypass an error.

```powershell
$adb = 'D:\Alpha\tools\android-sdk\platform-tools\adb.exe'
$apksigner = 'D:\Alpha\tools\android-sdk\build-tools\35.0.0\apksigner.bat'
$env:JAVA_HOME = 'D:\Alpha\tools\jdk\jdk-17.0.20.1+1'
$apk = (Resolve-Path '.\FamilySafe-V1-UX-Fix-Pilot.apk').Path
Get-FileHash $apk -Algorithm SHA256
& $apksigner verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'Stop: APK signature verification failed.' }
& $adb devices -l
$serial = 'REPLACE_WITH_VERIFIED_PHYSICAL_SERIAL'
if ($serial -eq 'REPLACE_WITH_VERIFIED_PHYSICAL_SERIAL' -or $serial.StartsWith('emulator-')) { throw 'Select the physical tablet first.' }
& $adb -s $serial shell pm path family.safe.debug
# Compare this APK's certificate with the installed base.apk as the checklist describes.
# Proceed only after matching the final checksum, package and installed signing certificate.
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Stop: preserve the existing app and investigate the installation error.' }
& $adb -s $serial shell am start -n family.safe.debug/family.safe.MainActivity
```

After upgrade, the adult checks the existing PIN, settings, source choices, requests and approval decisions before changing them. Unexpected fresh setup or settings lock is a failed upgrade; preserve data for diagnosis.

**Rollback prerequisite:** while the corrected APK still opens, use PIN-protected parent controls to remove the two newly supported Archive candidates (Fractions and Multiplication) and their decisions before installing the previous APK. The previous version rejects these unfamiliar candidate IDs and can lock settings if they remain. Install the preserved same-certificate previous APK with `install -r`, preserving remaining data. If rollback locks settings, reinstall the corrected APK with `install -r` to recover access and remove those new candidates; never clear storage. Removing candidates intentionally removes their reviews, so record that agreed rollback loss privately. Rollback restores the previous usability limitations.

Android System UI also showed an ANR on Launcher during emulator boot, before FamilySafe launch. That does not establish an app-caused ANR or explain the historical SocketException; no shared cause was proven.

The tablet trial and the user's actual UX approval remain required before merge. This document supplies no merge approval.
