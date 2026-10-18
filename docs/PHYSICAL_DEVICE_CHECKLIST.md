# Physical tablet checklist

Status: **NOT RUN** for V1 (2026-10-18). No physical tablet was available or validated. Emulator results in [TEST_MATRIX.md](TEST_MATRIX.md) do not establish physical-device behavior. Record PASS, FAIL or NOT RUN for each item; do not mark the tablet verified while required checks remain open. Non-NASA video access is limited to one vetted addition lesson; broader discovery remains unavailable.

At this completion gate, the device inventory returned **no connected devices**. The installation, upgrade and manual checks below are instructions for a future adult-led session, not completed results.

## Before installing

1. Use the tablet with its adult owner present. Agree whether this is a **new evaluation installation** or an **upgrade of the existing FamilySafe package**. Note the existing topic/source choices privately and confirm the parent can still unlock the current app. Do not copy settings files or record the PIN.
2. Use the reviewed APK and its recorded SHA-256 from [PROJECT_STATE.md](PROJECT_STATE.md). This APK is a supervised **debug evaluation** build, package `family.safe.debug`, requiring **Android 8.0 / API 26 or newer**. `family.safe` is a different release package; installing the debug APK does not upgrade it or transfer its settings. No signed production release is supplied by these instructions.
3. For an upgrade, keep the same package and signing certificate. A matching filename or package name alone is insufficient. Follow the certificate comparison below before installation. Stop if the earlier certificate cannot be established or the parent intended to upgrade a different package.

The [Windows build/install guide](../README.md#build-and-install-on-windows) lists the local JDK/SDK setup. The household installation needs only the app APK, not the test APK. Keep USB debugging access supervised; manage any temporary access afterward through the adult's existing device controls.

## Select the tablet and preserve its setup

An adult identifies the intended tablet and authorizes temporary USB debugging/installation. Do not change host security, device administration, account controls, install permissions or Family Link settings as part of these commands. Never clear app data, uninstall, delete `settings.json`, or pass `disposableInstrumentation=true` on a family tablet. `FamilyFlowTest`, `V1ParentDeviceTest` and `NasaReviewFlowTest` delete debug settings under that opt-in and must never run here. Do not run the entire connected instrumentation suite.

Run PowerShell from the repository root. The first inventory command has no target; **every subsequent ADB command explicitly selects the verified physical serial**. Do not choose an `emulator-*`, offline or unauthorized entry. Stop if identity is uncertain.

```powershell
$adb = 'D:\Alpha\tools\android-sdk\platform-tools\adb.exe'
& $adb devices -l
$serial = 'REPLACE_WITH_VERIFIED_PHYSICAL_SERIAL'
if ($serial -eq 'REPLACE_WITH_VERIFIED_PHYSICAL_SERIAL' -or $serial.StartsWith('emulator-')) { throw 'Select the physical tablet serial first.' }
& $adb -s $serial get-state
& $adb -s $serial shell getprop ro.product.manufacturer
& $adb -s $serial shell getprop ro.product.model
& $adb -s $serial shell getprop ro.build.version.release
& $adb -s $serial shell getprop ro.build.version.sdk
& $adb -s $serial shell dumpsys webviewupdate
& $adb -s $serial shell pm path family.safe.debug
```

Confirm the model against the tablet in hand and record Android/API, current WebView provider/version, orientation and display size. API must be at least 26. Keep the serial private in evidence shared outside the family.

Install only the reviewed APK from the verified build. `install -r` requests an in-place upgrade retaining the existing package's data; it does not guarantee migration correctness. `family.safe.debug` is a separate debug installation from `family.safe`. Cloud and Windows debug builds may use different signing keys. Compare signing-certificate fingerprints with the installed/reviewed build before an upgrade; never commit or transfer signing keys through source control. If signature/version checks reject the upgrade, stop and investigate; do not uninstall or clear data to make installation succeed. Debug builds are for supervised evaluation.

First verify the reviewed APK's signature and checksum. These commands read the APK and do not install it. Compare the checksum with the recorded build evidence; keep the printed signing-certificate SHA-256 digest for the next step.

```powershell
$apk = (Resolve-Path app\build\outputs\apk\debug\app-debug.apk).Path
$env:JAVA_HOME = 'D:\Alpha\tools\jdk\jdk-17.0.20.1+1'
$apksigner = 'D:\Alpha\tools\android-sdk\build-tools\35.0.0\apksigner.bat'
Get-FileHash $apk -Algorithm SHA256
& $apksigner verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed. Do not install.' }
```

For an **existing debug installation**, the earlier `pm path family.safe.debug` output identifies its installed `base.apk`. Copy that exact public APK path into the placeholder below, without the `package:` prefix. Pull only the app binary, not private app data. Compare `Signer #1 certificate SHA-256 digest` from both APKs; it must be identical. A checksum difference between APK versions is expected and does not replace this certificate comparison. If there is no existing debug package, skip this comparison only after the adult confirms that a separate new debug installation is intended.

```powershell
New-Item -ItemType Directory -Force app\build\physical-device-evidence | Out-Null
$installedBaseApk = 'REPLACE_WITH_BASE_APK_PATH_FROM_PM_PATH'
if ($installedBaseApk -eq 'REPLACE_WITH_BASE_APK_PATH_FROM_PM_PATH' -or !$installedBaseApk.EndsWith('/base.apk')) { throw 'Copy the verified package base.apk path first.' }
& $adb -s $serial pull $installedBaseApk app\build\physical-device-evidence\installed-base.apk
if ($LASTEXITCODE -ne 0) { throw 'Could not read the installed APK. Do not attempt an upgrade.' }
& $apksigner verify --verbose --print-certs app\build\physical-device-evidence\installed-base.apk
if ($LASTEXITCODE -ne 0) { throw 'Installed APK signature verification failed. Do not attempt an upgrade.' }
```

Proceed only after the adult has verified API 26+, physical identity, reviewed checksum and (for an upgrade) identical package/certificate. Do not use a downgrade flag, uninstall, clear storage, or create a new signing key to bypass an installation error.

```powershell
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Installation failed. Preserve the existing app and data; investigate the error.' }
& $adb -s $serial shell dumpsys package family.safe.debug | Select-String 'versionCode=|versionName='
& $adb -s $serial shell am start -n family.safe.debug/family.safe.MainActivity
```

Require ADB's explicit `Success`, confirm the expected installed version, and open the app under adult supervision. On an existing installation, confirm its PIN, failed-attempt state, chosen topics/blocks/sources, requests and prior review choices still work before changing anything. The parent session must start locked; an unexpected setup screen or settings-lock error is a failed upgrade check. Stop and preserve the installation for diagnosis rather than entering replacement setup or clearing storage. On a new installation, the adult creates a private PIN and deliberately selects only the trial categories/sources. Do not record the PIN or copy private settings files.

## Optional fixture instrumentation

The reviewed `BrowserDeviceTest` uses an isolated debug fixture activity and does not reset family settings. It exercises a real WebView with local fixture responses, including approved navigation, off-source/social/intent denial, disabled capabilities and restricted transport. It is not a live-site or complete family-flow test. Recheck the class's setup before running a changed test APK.

```powershell
& $adb -s $serial install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb -s $serial shell am instrument -w -r -e class family.safe.BrowserDeviceTest family.safe.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Require an explicit JUnit success and expected test count; skipped assumptions or a command that merely exits are not a pass. Keep the output locally. Do not add a disposable-reset flag or broaden the class filter. New review/player tests require source review for settings mutation and device guards before use here.

## Supervised manual checks

Use the adult's existing choices or a deliberately agreed trial setup. Restore agreed temporary choices through the PIN-protected UI afterward. Keep National Geographic Kids disabled: earlier emulator visual review found a blank page.

| Check | Action and expected result | Result / evidence |
|---|---|---|
| Touch and keyboard usability | On the actual tablet, type/edit a complete query, rename an agreed trial topic, save and switch parent tabs after scrolling a long page. Check touch targets, keyboard dismissal, tab selection, confirmation Cancel/Confirm and readable errors in both orientations. Do not count emulator clicks as physical touch verification. | NOT RUN |
| Home, categories and search | Check enabled/disabled topics. Save an agreed niche, review optional suggestions, and check finite approved wording in parent Test rules. Try "football passing drills for kids", an unknown gaming extension and a blocked/social phrase. Denied submitted searches show no result links; blocked searches have no request button. Requests do not unlock parent settings. | NOT RUN |
| Supported search consent and real results | Websites: on an existing install, add missing supported sources explicitly. Cancel the community-source consent first and verify it stays disabled; then deliberately enable/associate an agreed source/topic. Try animals, science, mathematics, geography and football where approved. Expect actual relevant titles, or an honest empty/error state. No Google/browser fallback. Queries leave the device only to approved providers. | NOT RUN |
| Read selected article | Open a real result; check headings, readable paragraphs, source/revision/license attribution, scrolling and zoom. Images and interactive content are intentionally unavailable. Links to other articles, subpages, namespaces, login/edit pages and external apps must remain blocked. Return retains foreground results; background/relaunch clears results. Check both Vikidia and Wikijunior where agreed. | NOT RUN |
| Parent edits and reset confirmation | Add/rename/remove a trial custom topic, manage phrases/synonyms/blocks/source associations, and preview sample decisions. Cancel a scoped reset and verify choices remain; confirm only a deliberately agreed reset. PIN, failed-attempt state, requests and video candidates must survive. Defaults disable access; do not reset real family choices for a test. | NOT RUN |
| Parent PIN and delay | With the adult present, enter a small agreed number of wrong PINs. Parent access stays locked and retry delay is visible/enforced. Wait for it before using the correct PIN; do not repeatedly lock out the parent. Record setup/unlock/wrong-PIN processing duration separately from the enforced retry delay; slow emulator hashing does not establish physical-tablet latency. | NOT RUN |
| Parent lifecycle | Unlock settings, then use Back from settings, Home/app switch and return. Parent access is locked again. Rotate during authentication; backgrounded authentication must not restore parent access. Preview Close/Back while foregrounded instead returns to the authorized review screen. | NOT RUN |
| Persistence | Note selected settings privately, force-stop/relaunch using the commands below, and confirm PIN, choices, review decisions and any active retry delay persist while the parent session is locked. | NOT RUN |
| Reboot | Only if the adult chooses a convenient time, reboot manually and repeat persistence/locked-session checks. Otherwise record NOT RUN; do not reboot remotely without that choice. | NOT RUN |
| Portrait and landscape | Rotate by hand on home, search, PIN, parent review, browser and player. Controls remain reachable, readable and unclipped; rotation does not bypass parent access. Use the tablet's existing rotation setting. | NOT RUN |
| Strict web | Open an enabled approved site and inspect real content. Back returns safely. Off-source/social links, redirects, popup, download, form and external-app intents stay blocked without browser/app fallback. Pair manual observations with fixture results; do not enable scripts or loosen scope to fix blank pages. | NOT RUN |
| Dynamic candidate discovery | Parent settings → Topics: enable an agreed topic/phrase, then Videos: select that topic and enter an exact approved phrase. Search NASA; at most five records are examined and candidates remain parent-only/unapproved. Confirm attribution/rights warning and local persistence. Unavailable API/assets show a closed error/empty state without external fallback. Existing two Space candidates/decisions remain present on upgrade. | NOT RUN |
| Vetted non-NASA lesson | In Topics, enable Maths and explicitly save `addition`. Videos → Khan Academy via Internet Archive: select Maths; cancel source consent first and verify disabled, then deliberately enable it. Find vetted lesson without a URL. Only Addition 3 may appear, still unapproved. Watch the complete approximately nine-minute lesson and review credits/license before deciding. Verify pending denial, deliberate approval, child playback and source disable/rejection. Other Archive results and collections remain unavailable. See [ARCHIVE_FEASIBILITY.md](ARCHIVE_FEASIBILITY.md). | NOT RUN |
| Curated video gate | Preview a discovered trial candidate, close preview, review content/credits/rights, confirm and approve; reject another. Only approved eligible content reaches children. Recheck after restart, disabling its category, blocking metadata and revoking/removing the trial item. Pending/rejected items stay hidden. Do not manufacture changed remote files on the tablet; digest mismatch remains a fixture/device-test case. Fixture approvals do not replace human review. | NOT RUN |
| Parent preview | Preview requires parent access. Close/Back from preview returns to authorized review while foregrounded. Back from settings locks access. Home, rotation/background and return always stop/delete playback, lock parent access and clear selection/preview hash; no hidden preview continues. | NOT RUN |
| Native playback | For an adult-approved trial item, check visible playback, pause/resume, seeking if offered, completion, Back and background release. No autoplay-next, recommendations, social links or external fallback. Check the distinct phase budgets: discovery search/manifests/HEAD share 60 seconds; player manifest/download/SHA-256 share 120 seconds; native media preparation has a separate 30-second watchdog. Deadline/cancellation marks the operation terminal and queues best-effort connection cleanup on workers; physical worker cleanup and UI responsiveness still require observation, with no hard real-time cleanup guarantee. Record timings and exact phase for a reproduced timeout. | NOT RUN |
| Network recovery | Adult manually disconnects/reconnects a chosen test network at a convenient time. Attempt download while offline and interrupt a download. Failure stays closed, UI remains usable, and retry recovers without stale/partial playback. A completed local download may keep playing offline; record the actual behavior. Do not change device/host security to simulate failure. | NOT RUN |

```powershell
& $adb -s $serial shell am force-stop family.safe.debug
& $adb -s $serial shell am start -n family.safe.debug/family.safe.MainActivity
```

The separate `NasaPlaybackDeviceTest` is a live adult engineering spike, not catalog approval. It downloads the fixed reference, consumes network data, captures a player screenshot and manages its own temporary media. Run it only after separate adult consent and review of its current source, preferably on a dedicated test installation rather than real family configurations. Its `nasaLive` opt-in does not establish content suitability, third-party rights or physical family-flow safety. See [NASA_FEASIBILITY.md](NASA_FEASIBILITY.md). A curated NASA workflow offers no automatic content/rights guarantee; YouTube child playback remains disabled.

## Evidence and device protections

Create a private local evidence folder and record date, app commit/APK SHA-256, model, Android/API, WebView, test counts/duration, scenario results and exact reproduction steps for failures. Capture crash/ANR diagnostics without clearing existing buffers:

```powershell
New-Item -ItemType Directory -Force app\build\physical-device-evidence | Out-Null
& $adb -s $serial logcat -b crash -d -v threadtime > app\build\physical-device-evidence\crash.txt
& $adb -s $serial logcat -d -v threadtime 'AndroidRuntime:E' 'ActivityManager:W' '*:S' > app\build\physical-device-evidence\runtime.txt
& $adb -s $serial shell dumpsys activity exit-info family.safe.debug > app\build\physical-device-evidence\exit-info.txt
```

`exit-info` may be unavailable on older Android versions; record that limitation. Inspect crash/ANR timestamps and package identity rather than attributing unrelated device failures to FamilySafe. Logs can contain private device data: redact before sharing, and keep raw logs/screenshots out of commits. Screenshots may be blocked by `FLAG_SECURE`; leave that protection enabled. Use non-sensitive external-camera photos only with the adult's consent, without PINs, names, account details or other apps in frame.

The adult separately verifies actual Family Link/account/app restrictions, installation approvals, alternate browsers/social apps, guest/account switching and available Android protections. Record the observed controls and any gaps; this checklist does not configure them and app tests cannot prove they are configured. The adult manages any temporary debugging/install access afterward. FamilySafe remains a supervised app, not a device lock or universal filter.

Completion requires the physical tablet, adult-led setup/content review, the manual rows above and human verification of device protections. Until then, report physical validation and protection configuration as pending.
