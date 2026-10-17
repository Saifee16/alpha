# Physical tablet checklist

Status: **not run**. No physical tablet was connected during Loop 3's ADB check. Emulator results in [TEST_MATRIX.md](TEST_MATRIX.md) do not establish physical-device behavior. Record PASS, FAIL or NOT RUN with evidence for each item below; do not mark the whole tablet verified while required checks remain open.

## Select the tablet and preserve its setup

An adult identifies the intended tablet and authorizes temporary USB debugging/installation. Do not change host security, device administration, account controls, install permissions or Family Link settings as part of these commands. Never clear app data, uninstall, delete `settings.json`, or pass `disposableInstrumentation=true` on a family tablet. `FamilyFlowTest` and `NasaReviewFlowTest` delete debug settings under that opt-in and must never run here. Do not run the entire connected instrumentation suite.

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

Install only the reviewed APK from the verified build. `install -r` requests an in-place upgrade retaining the existing package's data; it does not guarantee migration correctness. `family.safe.debug` is a separate debug installation from `family.safe`. If signature/version checks reject the upgrade, stop and investigate; do not uninstall or clear data to make installation succeed. Debug builds are for supervised evaluation.

```powershell
Get-FileHash app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
& $adb -s $serial install -r app\build\outputs\apk\debug\app-debug.apk
& $adb -s $serial shell am start -n family.safe.debug/family.safe.MainActivity
```

On an existing installation, confirm its PIN and chosen settings still work before changing anything. On a new installation, the adult creates a private PIN and deliberately selects only the trial categories/sources. Do not record the PIN or copy private settings files.

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
| Home, categories and search | Check enabled/disabled topics; try an approved whole phrase, an unknown extension, and a blocked/social phrase. Only allowed results appear; unknown/blocked input cannot open arbitrary sites. Requesting approval does not unlock parent settings. | NOT RUN |
| Parent PIN and delay | With the adult present, enter a small agreed number of wrong PINs. Parent access stays locked and retry delay is visible/enforced. Wait for it before using the correct PIN; do not repeatedly lock out the parent. Record setup/unlock/wrong-PIN processing duration separately from the enforced retry delay; slow emulator hashing does not establish physical-tablet latency. | NOT RUN |
| Parent lifecycle | Unlock settings, then use Back from settings, Home/app switch and return. Parent access is locked again. Rotate during authentication; backgrounded authentication must not restore parent access. Preview Close/Back while foregrounded instead returns to the authorized review screen. | NOT RUN |
| Persistence | Note selected settings privately, force-stop/relaunch using the commands below, and confirm PIN, choices, review decisions and any active retry delay persist while the parent session is locked. | NOT RUN |
| Reboot | Only if the adult chooses a convenient time, reboot manually and repeat persistence/locked-session checks. Otherwise record NOT RUN; do not reboot remotely without that choice. | NOT RUN |
| Portrait and landscape | Rotate by hand on home, search, PIN, parent review, browser and player. Controls remain reachable, readable and unclipped; rotation does not bypass parent access. Use the tablet's existing rotation setting. | NOT RUN |
| Strict web | Open an enabled approved site and inspect real content. Back returns safely. Off-source/social links, redirects, popup, download, form and external-app intents stay blocked without browser/app fallback. Pair manual observations with fixture results; do not enable scripts or loosen scope to fix blank pages. | NOT RUN |
| Curated video gate | Parent settings → Topics: enable Space, then open Videos. Preview a complete candidate download, close preview, review content/credits/rights, check confirmation and approve. Reject the second candidate. Only the approved eligible item reaches child playback; rejected/unreviewed items stay unavailable. Recheck after restart, after disabling Space and with metadata blocked. Changed download hashes must fail closed and need re-review. Fixture-test approvals do not replace this human review. | NOT RUN |
| Parent preview | Preview requires parent access. Close/Back from preview returns to authorized review while foregrounded. Back from settings locks access. Home, rotation/background and return always stop/delete playback, lock parent access and clear selection/preview hash; no hidden preview continues. | NOT RUN |
| Native playback | For an adult-approved trial item, check visible playback, pause/resume, seeking if offered, completion, Back and background release. No autoplay-next, recommendations, social links or external fallback. | NOT RUN |
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
