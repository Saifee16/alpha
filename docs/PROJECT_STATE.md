# Project state — 2026-10-17

FamilySafe has completed the dedicated emulator test pass. Baseline: `a0d4dee`; NASA feasibility implementation: `5ae2dcc`. Tablet flow/browser test work is committed as `c4d144e`; final evidence is recorded in the following documentation commit. No push or publication occurred.

## Verified

- Toolchain: JDK 17.0.20.1+1, Gradle 8.13, Android SDK 36 and Build-Tools 35.
- Final Gradle build: 45 JVM tests pass; app and instrumentation APKs compile; lint 0 errors, 16 warnings (7 dependency notices, 1 intentional debug YouTube JavaScript notice, 8 NASA debug `SetTextI18n` notices). Final command `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain --no-daemon --max-workers=1` succeeded in 5m33s (84 tasks: 10 executed, 74 up-to-date).
- Fourteen instrumentation tests ran and passed on API 35 Android 15 AOSP x86_64 (1280×800, 160 dpi; WebView 124.0.6367.219; WHPX). See [TEST_MATRIX.md](TEST_MATRIX.md) for timings and observed results.
- Process restart preserved identical settings hash (PID 3172 → 3216); child query remained visible while parent session was absent. Runtime fatal-error buffer had 0 lines and no FamilySafe fatal entry.
- NASA playback in the isolated adult debug harness passed playback, pause/resume, seek, completion, and background-release checks.
- NASA Space Place rendered fully and BBC Bitesize navigation was visible. National Geographic Kids returned HTTP 200 but rendered blank; keep it disabled. Do not weaken the browser policy or enable JavaScript to make it work.
- YouTube API flag remains false, no key is built in, and child playback remains disabled.

## Remaining work

- Validate installation and usability on a physical tablet; none was available for this run.
- Keep National Geographic Kids disabled pending a safe rendering fix or replacement.
- Next product milestone: test a parent-curated NASA collection and review third-party rights. YouTube API discovery needs child-directed setup and a restricted key; YouTube child playback remains blocked.

Evidence files are in ignored `app/build/device-evidence/*.txt` and `evidence/*.png`.

Final debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 10,838,751 bytes. SHA-256: `742322060b740ef090e0f65f1bdc40a90593cc470285bfa6b68801d104d618c6`; adjacent checksum file generated. APK v2 signature verified; package `family.safe.debug`, version 0.1.0, minimum26/target36. The dedicated emulator was stopped after collecting evidence; its AVD remains available.
