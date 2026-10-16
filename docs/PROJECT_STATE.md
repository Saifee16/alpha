# Project state — 2026-10-17
Phase: verified debug build ready for parent-led device testing; full MVP device acceptance remains incomplete. Empty starting workspace; new Git repository initialized.
Host: Windows. Broken Java PATH replaced for commands by workspace JDK17.0.20.1+1, Gradle8.13 and SDK36/build-tools35 installed in D:\Alpha\tools. No connected device/emulator.

## Execution plan
User supplied complete product specification in attached master prompt; execution authorized without another design approval.
- [x] Build shell and official wrapper: Gradle8.13 / AGP8.13.2 / JDK17 / SDK36 / Kotlin2.2.21 / Compose BOM2025.08.00, minimum Android26.
- [x] Source: atomic local settings, PBKDF2 PIN, persisted attempt throttling, parent/child Compose flows. Android runtime behavior unverified.
- [x] Exact whole-query approved phrases/synonyms; normalized blocked phrase checks; unknown terms deny. Core JVM tests pass.
- [x] Strict URL policy and read-only WebView source; URL unit tests pass, device behavior unverified.
- [x] Real bounded YouTube API client + package/certificate identity headers, default disabled pending child-directed setup; isolated debug player spike; shipping playback denied.
- [x] Integrate, run unit tests/APK/lint, fresh review, document device tests honestly and produce checksum.

## Review focus
Parent session must lock on background/recreation. Failed PIN cooldown survives restart.
Corrupt settings must lock, never reset parent setup. Categories/sources start disabled.
Query approval must never approve arbitrary video metadata. No child content fixtures represented as live.
Third-party site compatibility and player behavior require a device; no device currently available.

## Active ownership
Main: build, ui, data, settings, policy, docs except VIDEO_FEASIBILITY.md.
browser_policy: browser package/tests. video: video package/tests, debug spike/manifest, VIDEO_FEASIBILITY.md.

## Verification
- Gradle wrapper generation: BUILD SUCCESSFUL, 9m47s (first dependency resolution).
- Direct Kotlin2.0.21/JUnit4 core run: OK (20 tests), exit0; query/URL/PIN suites.
- Final command: .\gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain. BUILD SUCCESSFUL, exit0, 1m1s, 84 tasks (27 executed/57 up-to-date). JDK/SDK/Gradle cache environment paths documented in README.
- adb devices -l: empty list. Device/UI/player behavior NOT RUN. Live API blocked: no key/Google child-directed setup.
- Starter source HEAD checks: NASA, National Geographic Kids and BBC Bitesize returned direct HTTPS200 from this host. This does not verify WebView rendering or content suitability.
- API/default fixtures never shown as live discovery. No unrestricted browser/player fallback.
- Corrected full unit suite: 36 tests, zero failures/errors (browser8, settings4, query12, PIN3, video9). Initial full suite caught two Unicode combining-mark regressions; fixed and rerun.
- Final debug APK: app/build/outputs/apk/debug/app-debug.apk. Package family.safe.debug, version0.1.0, minimum26/target36. apksigner verify succeeds (v2 signature); aapt badging confirms metadata. Device-test APK compiled at app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk.
- APK SHA-256: fb7fc8667457884a773f7611be47bbbdeb8fe372265ffd6fb5971f12bb3ebd7c. Adjacent .apk.sha256 file generated.
- Lint: zero errors, eight warnings (seven newer-version notices, one intentional JavaScript warning in isolated debug player). Fixed original local SDK property escape error, added app icon and explicit Android12+ backup/transfer exclusions; no lint baseline/suppression added.
- Final independent source review found no confirmed blocking defects; runtime safety remains unverified.

## Review fixes
Independent security review identified overlapping temp writes, empty-PIN configured state, password keyboard semantics and stale async search results. Added process-wide file lock/unique temp files, configured-state validation, password input options and result query check. Integrated fixes passed the full JVM suite and Android build/lint. Final independent review found no confirmed blocking source defects, including the final backup configuration.

## Remaining
External: connect a tablet/emulator, install APK, execute TEST_MATRIX device scenarios (including browser escape attempts, setup/restart and background PIN locking) and review site rendering before child access. Configure Family Link. Optional API discovery needs child-directed setup/restricted credentials/rebuild and live validation. YouTube child playback remains intentionally blocked. No known confirmed source defect remains; unverified runtime behavior is a material limitation. Highest-value next work is device testing, followed by evaluating a controllable lawfully hosted educational video source if video remains essential.
