# Verification matrix — 2026-10-17

No connected emulator/tablet: `D:\Alpha\tools\android-sdk\platform-tools\adb.exe devices -l` returned an empty device list.

| Scenario | Expected | Observed/status | Evidence |
|---|---|---|---|
| Whole phrase, synonyms, case, NFKC, punctuation/space | Exact enabled phrase allowed | PASS | Direct Kotlin/JUnit core run, 20 tests total |
| Unknown, unrelated appended terms, disabled categories, long/empty/control input | Deny | PASS | QueryPolicyTest |
| Banned words, phrases, separator/invisible evasion, word boundaries | Block overrides approvals | PASS | QueryPolicyTest |
| Approved exact HTTPS host/path | Allow only defined scope | PASS | UrlPolicyTest |
| Deceptive host, encoding/traversal, userinfo, port, social/video/search hosts | Deny | PASS | UrlPolicyTest |
| Redirect target outside scope | Deny | PASS | UrlPolicyTest (pure URL resolution only) |
| Salted PIN, wrong PIN, malformed hash, setup length | Correct verification/rejection | PASS | PinSecurityTest |
| Retry cooldown, clock rollback | Delay persists in state | PASS | PinSecurityTest (persistence separately below) |
| Settings and requests survive new repository | Preserve saved data | PASS | SettingsStoreTest, including concurrent writers |
| Corruption / PIN removed with active permissions | Lock; never reopen setup | PASS | SettingsStoreTest |
| Verified channel identity and bounded discovery | Restrict IDs and metadata | PASS | YouTubeClientTest fixtures; not live API |
| Missing key/quota/API errors | Closed error, no fallback | PASS | YouTubeClientTest fixtures |
| Native app + instrumentation APK compile | Successful build | PASS | Final wrapper run BUILD SUCCESSFUL, both APKs exist |
| Android lint | No errors | PASS | Zero errors/eight warnings; lint-results-debug.html/xml/txt |
| Parent setup, changes, home/search/request, recreation lock | Correct UI and persistence | NOT RUN | FamilyFlowTest; requires fresh debug install/device |
| Browser JS, popups, download/upload, external schemes, subresource redirects, SSL | Block without leaving app | NOT RUN | Device required; static implementation only |
| NASA/NatGeo/BBC rendering and links | Usable permitted pages; deny external | NOT RUN | Device required; sources disabled initially |
| Player play/pause/fullscreen/completion/links | Determine recommendation escape behavior | NOT RUN | Unexported debug spike; shipping playback blocked |
| Live API + child-directed setup + restricted key | Approved calls only | BLOCKED | No credentials/Google setup provided; build flag false |
| Private install | APK installs and opens | NOT RUN | No device |

Core command used Gradle's bundled Kotlin 2.0.21 compiler and JUnit 4.13.2 on JDK17 (no Android runtime). Result: `OK (20 tests)`, exit 0, 24.887 seconds. Full Android Gradle verification remains separate.

Later Unicode regressions: policy-only run RED (12 tests, two failures), then GREEN (12/12). Corrected Android Gradle unit suite has 36 tests, zero failures/errors. Final command `.\gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain` completed successfully, exit0, 1m1s. APK signature verification passed after final rebuild. Lint's SDK-path escape error was fixed; no checks were disabled. Remaining warnings are seven dependency-version notices and intentional JavaScript in the isolated debug-only spike. Runtime/instrumentation tests remain NOT RUN.
