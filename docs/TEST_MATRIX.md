# Verification matrix — 2026-10-17

## Build and JVM evidence

Current baseline is `a0d4dee`, followed by NASA feasibility implementation `5ae2dcc` and tablet test commit `c4d144e`. Final Gradle run: 45 JVM tests pass; debug and Android-test APKs compile; lint has 0 errors and 16 warnings (7 dependency notices, 1 intentional JavaScript notice in the isolated YouTube spike, 8 NASA debug `SetTextI18n` notices). Final wrapper command succeeded in 5m33s, 84 tasks (10 executed/74 up-to-date); `app/build/device-evidence/final-build.txt`.

## Emulator evidence

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

## Unit and static checks

| Scenario | Result | Evidence |
|---|---|---|
| Exact approved phrases, normalization, blocked terms, URL scope, redirects | PASS | Included in 45 JVM tests; URL resolution tests are pure policy tests |
| PIN verification/throttling, settings persistence and corruption handling | PASS | PIN and settings store tests |
| Bounded YouTube discovery and metadata checks | PASS | Fixture tests only; no live API/key |
| App and Android-test APK builds | PASS | Most recent fully verified wrapper run |
| Lint | PASS | 0 errors; 16 warnings as detailed above |

YouTube API calls are blocked until child-directed setup and restricted credentials exist. Child YouTube playback remains blocked by design. Passing instrumentation does not make the blank National Geographic Kids page usable or establish behavior on a physical tablet.
