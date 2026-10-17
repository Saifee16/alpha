# FamilySafe

FamilySafe is a private, native Android learning browser for a parent-supervised tablet. It has no backend. Parent choices and approval requests stay in an app-private local settings file; categories and websites start disabled. There is no built-in parent PIN.

The prototype includes exact approved learning phrases, blocked-phrase checks, a read-only WebView restricted to an approved HTTPS host and path, and local PIN protection. The PIN uses salted PBKDF2-HMAC-SHA256 (600,000 rounds); failed-attempt delays persist. Settings are written atomically, malformed settings lock the app, and app data is excluded from Android backup. The starter catalog is NASA Space Place (`spaceplace.nasa.gov`), National Geographic Kids (`kids.nationalgeographic.com`), and BBC Bitesize (`www.bbc.co.uk/bitesize`). All sources begin off.

## Build on Windows

Install JDK 17, Android SDK Platform 36, and Android SDK Build-Tools 35. Open PowerShell in this folder, ensure `JAVA_HOME` points to JDK 17 and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) points to the SDK, then run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat lintDebug
```

Historical Loop 3's final build passed on 2026-10-17: 54 JVM tests with no failures/errors, both APKs built, lint zero errors and 17 warnings, in 5m22s (84 tasks: 19 executed, 65 up-to-date). All 20 distinct emulator scenarios passed across overlapping batches: initial 17/20, focused 7/8, final live 1/1. This was not a single clean 20/20 run; an earlier live-preview timeout remains unexplained. Final live checks previewed both NASA candidates and exercised approved child playback; a spacecraft frame was visually confirmed. Keep National Geographic Kids disabled after its blank-page visual result. The project uses Android Gradle Plugin 8.13.2, Kotlin 2.2.21, compile/target SDK 36, minimum SDK 26 and Gradle 8.13. See [docs/TEST_MATRIX.md](docs/TEST_MATRIX.md) for evidence and remaining checks.

The workspace-local toolchain is `D:\Alpha\tools\jdk\jdk-17.0.20.1+1`, `D:\Alpha\tools\android-sdk` and `D:\Alpha\tools\gradle-cache`. To rebuild here, set `JAVA_HOME`, `ANDROID_HOME` and `GRADLE_USER_HOME` to those paths respectively. On another machine, Android Studio can generate `local.properties`; manual Windows paths must escape the drive colon, for example `sdk.dir=D\:/Android/sdk`.

Install a built debug APK on a connected device with:

```powershell
adb devices -l
adb -s VERIFIED_PHYSICAL_SERIAL install -r app\build\outputs\apk\debug\app-debug.apk
```

APK: `app/build/outputs/apk/debug/app-debug.apk`, debug variant, package `family.safe.debug`, Android 8.0+. Baseline runtime validation used an API 35 AOSP x86_64 tablet emulator (1280×800, 160 dpi), WebView 124.0.6367.219, with WHPX. No physical device was available for Loop 3. `install -r` requests an upgrade retaining that package's data; debug is separate from a release installation. Stop on signature/version errors rather than uninstalling. `FamilyFlowTest` and `NasaReviewFlowTest` reset settings under explicit disposable opt-in; never run them on a family tablet. The guard does not verify emulator identity. Follow [docs/PHYSICAL_DEVICE_CHECKLIST.md](docs/PHYSICAL_DEVICE_CHECKLIST.md) and keep child access supervised.

For the full suite, use the explicitly selected dedicated disposable emulator command in [docs/TEST_MATRIX.md](docs/TEST_MATRIX.md). It resets settings and enables live-network tests; never run the full suite on a family tablet.

Set up the parent PIN first, then enable only the categories and sources you intend to allow. Keep National Geographic Kids disabled: device visual review showed a blank page. NASA Space Place rendered fully and BBC Bitesize showed navigation. This app is a supervised learning tool, not a device lock or universal content filter. JavaScript, forms, media, downloads, external links, and redirects outside the exact approved source are blocked, so some pages may not render.

Source category associations control the directory and search results. Once a source is enabled with an enabled associated topic, its entire approved host/path scope is available; FamilySafe cannot classify every page inside that scope. Site-specific form searches and interactive games are unavailable in this strict reader. Keep unsupported or blank-rendering sites disabled. The smoke test received HTTP 200 from all three catalog sites; that transport result does not guarantee a usable page.

## Cloud Loop 4 status

Loop 4 adds bounded **parent-only NASA discovery**; fresh cloud validation passed: 71 JVM tests (0 failures/errors/skips), both APKs built, lint 0 errors/12 warnings. The final command took 42 seconds; 21 instrumentation test methods compiled but were not executed. The completed cloud onboarding baseline was 54 JVM tests, both APKs built and lint 0 errors/12 warnings. Loop 3 emulator evidence below is historical: no emulator or physical runtime has been run in this cloud iteration. Live NASA verification also requires the environment's network policy to allow `images-api.nasa.gov` and `images-assets.nasa.gov`; denied requests do not establish provider availability.

The existing cloud toolchain is configured by `source /workspace/toolchains/familysafe-env.sh`. From this existing checkout run `bash gradlew --no-daemon --max-workers=4 testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`; the wrapper need not be executable. Do not create a worktree or reinstall a working toolchain.

## Video status

In PIN-protected parent settings, enable an approved topic, open **Videos**, select that topic, enter one of its exact approved phrases, then search NASA. Discovery examines at most five JPL video records on one page, validates metadata and exact mobile MP4 assets, and saves at most 30 local review candidates. It follows no feed, pagination, thumbnail or API-provided navigation links. The original Europa/NISAR Space candidates and existing review decisions are preserved when older settings load. Repeated discovery does not replace reviewed metadata or expand its category; removing a candidate removes its approval.

Preview a candidate's local download, close preview, review content/credits/rights, then confirm the checkbox and approve or reject. The app does not enforce watching to completion or certify suitability/rights. Candidate metadata, category, decision and approved SHA-256 remain local. Only approved items in an enabled associated category with unblocked metadata can reach children; child downloads must match the approved hash. Pending, rejected, removed, disabled-category and changed media fail closed. There is no child discovery, manual URL entry, autoplay-next or external-app fallback.

Downloads use exact HTTPS paths, no redirects and a 32MiB limit. A 30-second media-prepare watchdog and attempt ownership guards bound stalled preparation after a verified download and reject stale callbacks; they do not diagnose the historical preview timeout. A total network deadline and prompt cancellation/disconnect remain limitations. Backgrounding releases playback and deletes the temporary file. Parent rotation/background locks access and clears preview selection/hash.

YouTube discovery is disabled in the build (`YOUTUBE_API_ENABLED=false`), no API key is built in, and YouTube child playback remains unavailable. Official YouTube playback cannot isolate recommendations and linked content. Any future API setup requires a dedicated restricted key, Google's child-directed-client setup and privacy work; stored API metadata expires after 30 days and is removed on load. There is no YouTube metadata cache.

For device supervision, use [Google Family Link](https://families.google/familylink/) with an age-appropriate supervised Google account on the tablet. Labels and controls vary by device/account:

1. Select the child → Screen time → Time limits → App limits. Block alternative browsers, ordinary YouTube, Instagram, Facebook, TikTok and Snapchat. Review already-installed/previously-approved apps; some system apps cannot be blocked. [Official app controls](https://support.google.com/families/answer/7103028).
2. Controls → Google Chrome and Web → Only allow approved sites. Review approved/blocked sites and existing site permissions; this Chrome setting does not make other browsers safe. [Official site controls](https://support.google.com/families/answer/7087030).
3. Controls → Google Play → Purchases & download approvals → Require approval for → All content. Review age ratings too. Existing approvals/family-library apps may not prompt again. [Official approval controls](https://support.google.com/families/answer/7039872).
4. Review the tablet's available account, guest-profile and unknown-app-install settings with the parent controlling access. Do not assume Family Link can restrict every setting or system app. After private installation, disable USB debugging and temporary install permissions when no longer needed.

FamilySafe enforces only its own navigation and local rules. Family Link/Android configuration must restrict the rest of the device. Clearing app data, uninstalling where allowed, or resetting the device can bypass app-only setup; adult supervision remains necessary.

The fixed-candidate Loop 3 review/child flow passed its final live emulator test; Loop 4 dynamic discovery has no device-runtime evidence. Physical-tablet validation and actual adult content/rights review remain open. Unreviewed media has no child route. See [docs/NASA_FEASIBILITY.md](docs/NASA_FEASIBILITY.md) for candidates, terms and review gates.

See [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) for implementation and verification status, [docs/TEST_MATRIX.md](docs/TEST_MATRIX.md) for device scenarios, and [docs/VIDEO_FEASIBILITY.md](docs/VIDEO_FEASIBILITY.md) for the video decision.
