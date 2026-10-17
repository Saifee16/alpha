# Project state — 2026-10-17

Loop 3 starts from `eb14e4b` (baseline rebuild: 45 JVM tests, 0 lint errors/16 warnings, 1m18s). Catalog commit: `4d80430`; verified implementation/test source commit: `43bbb78` (only trailing blank lines were trimmed after verification). Final build PASS: 54 JVM tests, 0 failures/errors, both APKs built, lint 0 errors/17 warnings, 5m22s (84 tasks: 19 executed, 65 up-to-date). All 20 distinct emulator scenarios passed across overlapping batches: initial 17/20 (762.45s), focused 7/8 (577.044s), final live 1/1 (145.835s). This is not a single clean 20/20 run; an earlier live-preview timeout remains unexplained. See [TEST_MATRIX.md](TEST_MATRIX.md) for incidents and limits. Final ADB inventory contained only the emulator, with no physical tablet.

## Loop 3 implementation

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

## Remaining work

- Validate installation and usability on a physical tablet; none was available for this run.
- Keep National Geographic Kids disabled pending a safe rendering fix or replacement.
- Physical PIN latency is NOT RUN. Corrected emulator authentication checks passed with measured waits about 9.7/10.2/10.6s; authentication assertions and PBKDF2's 600,000 rounds remain unchanged. Preserve the unexplained earlier live-preview timeout as a reliability limitation.
- Final process restart PASS: PID 4744 → 4788, identical settings hash, query visible and parent session absent. The first UI dump had a null root; a wake/start retry succeeded while the recorded power state was already Awake. `final-process-restart.txt` retains that retry.
- Adult-review candidate content/credits/rights, then follow [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) on the actual tablet. Human verification of Family Link/device controls remains pending. Never run destructive `FamilyFlowTest` or `NasaReviewFlowTest` on a family tablet.

Loop 3 evidence is in ignored `app/build/loop3-evidence/`; earlier files remain under `app/build/device-evidence/` and `evidence/`. Final crash buffer had 0 lines and Android reported no ANR since boot. Approved child-player screenshot showed a spacecraft frame; parent preview screenshots captured opening black frames and do not provide visual playback proof. Offline error/Retry and parent-landscape/child-portrait layouts were readable; NASA rendered fully, BBC navigation was visible and National Geographic Kids remained blank.

Final debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 11,267,063 bytes, SHA-256 `4eec09be21c007871a3e76b6c0d59f0b75e5ef1ebe4cb496fbc1f3fbd94f3cd9`, v2 signature PASS. Package `family.safe.debug`, version 0.1.0, minimum26/target36.
