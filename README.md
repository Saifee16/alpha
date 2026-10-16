# FamilySafe

FamilySafe is a private, native Android learning browser for a parent-supervised tablet. It has no backend. Parent choices and approval requests stay in an app-private local settings file; categories and websites start disabled. There is no built-in parent PIN.

The prototype includes exact approved learning phrases, blocked-phrase checks, a read-only WebView restricted to an approved HTTPS host and path, and local PIN protection. The PIN uses salted PBKDF2-HMAC-SHA256 (600,000 rounds); failed-attempt delays persist. Settings are written atomically, malformed settings lock the app, and Android backup is not configured. The starter catalog is NASA Space Place (`spaceplace.nasa.gov`), National Geographic Kids (`kids.nationalgeographic.com`), and BBC Bitesize (`www.bbc.co.uk/bitesize`). All sources begin off.

## Build on Windows

Install JDK 17, Android SDK Platform 36, and Android SDK Build-Tools 35. Open PowerShell in this folder, ensure `JAVA_HOME` points to JDK 17 and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) points to the SDK, then run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

Verified on 2026-10-17: `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest` completed successfully through the wrapper. All 36 JVM tests passed. Lint has zero errors and eight warnings (dependency update notices and intentional JavaScript in the debug player spike). Both app and instrumentation APKs compile; no device tests were executed. The project uses Android Gradle Plugin 8.13.2, Kotlin 2.2.21, compile/target SDK 36, minimum SDK 26 and Gradle 8.13.

The workspace-local toolchain is `D:\Alpha\tools\jdk\jdk-17.0.20.1+1`, `D:\Alpha\tools\android-sdk` and `D:\Alpha\tools\gradle-cache`. To rebuild here, set `JAVA_HOME`, `ANDROID_HOME` and `GRADLE_USER_HOME` to those paths respectively. On another machine, Android Studio can generate `local.properties`; manual Windows paths must escape the drive colon, for example `sdk.dir=D\:/Android/sdk`.

Install a built debug APK on a connected device with:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

APK: `app/build/outputs/apk/debug/app-debug.apk`, debug variant, package `family.safe.debug`, Android 8.0+. SHA-256: `fb7fc8667457884a773f7611be47bbbdeb8fe372265ffd6fb5971f12bb3ebd7c`. Signature verification passed. Installation itself is NOT RUN. Use a parent-controlled device and do not hand it to a child before running the device scenarios in the test matrix. To execute the compiled UI test on a fresh debug installation, run `.\gradlew.bat connectedDebugAndroidTest` with a connected authorized tablet/emulator; the test refuses an already-configured installation and does not erase saved family settings.

Set up the parent PIN first, then enable only the categories and sources you intend to allow. This app is a supervised learning tool, not a device lock or universal content filter. JavaScript, forms, media, downloads, external links, and redirects outside the exact approved source are blocked, so some pages may not render. No connected-device compatibility checks have been run.

Source category associations control the directory and search results. Once a source is enabled with an enabled associated topic, its entire approved host/path scope is available; FamilySafe cannot classify every page inside that scope. Site-specific form searches and interactive games are unavailable in this strict reader. Unsupported sites should stay disabled. The catalog domains were checked against official site references and returned HTTPS 200 responses; rendering remains untested.

## Video status

YouTube discovery is disabled in the build (`YOUTUBE_API_ENABLED=false`), no API key is built in, and child playback is always unavailable. Official YouTube playback cannot isolate recommendations and linked content. Any future API setup requires a dedicated restricted key, Google's child-directed-client setup and privacy work; stored API metadata expires after 30 days and is removed on load. There is no video metadata cache.

For device supervision, use [Google Family Link](https://families.google/familylink/) with an age-appropriate supervised Google account on the tablet. Labels and controls vary by device/account:

1. Select the child → Screen time → Time limits → App limits. Block alternative browsers, ordinary YouTube, Instagram, Facebook, TikTok and Snapchat. Review already-installed/previously-approved apps; some system apps cannot be blocked. [Official app controls](https://support.google.com/families/answer/7103028).
2. Controls → Google Chrome and Web → Only allow approved sites. Review approved/blocked sites and existing site permissions; this Chrome setting does not make other browsers safe. [Official site controls](https://support.google.com/families/answer/7087030).
3. Controls → Google Play → Purchases & download approvals → Require approval for → All content. Review age ratings too. Existing approvals/family-library apps may not prompt again. [Official approval controls](https://support.google.com/families/answer/7039872).
4. Review the tablet's available account, guest-profile and unknown-app-install settings with the parent controlling access. Do not assume Family Link can restrict every setting or system app. After private installation, disable USB debugging and temporary install permissions when no longer needed.

FamilySafe enforces only its own navigation and local rules. Family Link/Android configuration must restrict the rest of the device. Clearing app data, uninstalling where allowed, or resetting the device can bypass app-only setup; adult supervision remains necessary.

See [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) for implementation and verification status, and [docs/VIDEO_FEASIBILITY.md](docs/VIDEO_FEASIBILITY.md) for the video decision.
