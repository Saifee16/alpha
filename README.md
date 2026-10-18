# FamilySafe

FamilySafe is a private Android learning browser for a parent-supervised tablet. Parents configure subjects, learning niches, blocked phrases and trusted websites behind a PIN. Children search approved sources and read permitted articles inside the app. No backend, registration, advertising or analytics. Categories and sources start disabled; there is no default PIN.

**V1 status: partially complete.** Real multi-subject educational search and parent configuration are implemented. Low-maintenance, non-NASA video discovery is still missing. Physical-tablet validation and adult suitability/rights review are NOT RUN. This is an adult-led evaluation build, not an independently child-safe device configuration. See [project state](docs/PROJECT_STATE.md) and [test evidence](docs/TEST_MATRIX.md).

## Parent setup and search

1. Create a private PIN. PIN verification uses salted PBKDF2-HMAC-SHA256, 600,000 rounds; failed-attempt delays persist. Settings save atomically, malformed settings lock the app, and Android backup is disabled.
2. In **Topics**, enable chosen subjects; add/rename custom subjects, niches and synonyms. Save phrase edits explicitly. **Suggest search phrases** only edits the reviewable draft. A small built-in grammar also permits one prefix (`how to`, `learn about`, `tell me about`) and one suffix (`for kids`, `for children`, `for beginners`, `explained`, `tutorial`, `lessons`) around a complete approved niche.
3. In **Words / Blocked content**, manage blocked phrases. Blocks override permission, including obfuscated separators. Unknown or mixed queries fail closed. Approving `football passing drills` permits `football passing drills for kids`, but not `football passing drills and violent gaming`. Enabling Football alone does not approve arbitrary extra words.
4. In **Websites**, explicitly consent before enabling **Vikidia** or **Wikijunior**, then associate enabled topics. Existing installations use **Add supported search sources** to add missing disabled entries without changing their current choices. Other sites remain clearly labeled website directories. National Geographic Kids stays disabled because no safe usable rendering path is verified.
5. **Test rules** previews the decision, matched topic, canonical niche and available sources locally. Requests never grant access automatically. Delete/reset/restore actions require confirmation and preserve the PIN. **PIN / Security** shows protection status and allows PIN changes. Lock parent mode before handing over the tablet; backgrounding locks it too.

Both educational integrations use official [MediaWiki search](https://www.mediawiki.org/wiki/API:Search), with five results/source and one page. Cards contain validated plain titles, source and category, with no unsafe snippet or thumbnail. All normalized query words must match the title, using conservative plural/math aliases. Narrow matching can produce no results for specific niches; the app never fabricates matches or falls back to Google.

Live API checks demonstrated Vikidia resources for animals, science, mathematics, geography and football (including Animal, Science, Mathematics, Geography and Association football). Wikijunior complements animals/science/mathematics; geography/football full-text hits can be rejected for weak title relevance. Availability and community content can change.

These are **community-edited sources**, not suitability-certified catalogs. Approved search phrases, article choices, IP address and ordinary connection metadata reach the chosen provider. The app sends no PIN, account, cookies or analytics. Search text/results stay in foreground memory, survive returning from an article and clear when backgrounded. Local approval requests remain until a parent clears them. Review [Vikidia legal matters](https://en.vikidia.org/wiki/Vikidia:Legal_matters), [Wikimedia privacy](https://foundation.wikimedia.org/wiki/Policy:Privacy_policy) and [API etiquette](https://www.mediawiki.org/wiki/API:Etiquette) before enabling.

## Protected reading

Selecting a search result permits only that exact canonical article and its anchors. Other articles, descendants, edit/API pages and external destinations remain denied. The internal reader obtains the article through the official [Parse API](https://www.mediawiki.org/wiki/API:Parsing_wikitext), requires matching returned identity and includes source/contributor attribution, revision and license information. Vikidia generally uses [CC BY-SA 3.0](https://en.vikidia.org/wiki/Vikidia:Reuse_of_Vikidia_content); Wikibooks generally uses [CC BY-SA 4.0](https://en.wikibooks.org/wiki/Wikibooks:Copyrights), subject to article notices.

The adapted text view removes the global navigation shell. **Images, scripts, forms, storage, media, downloads, popups and external apps remain unavailable.** This limits illustrated/interactive learning. Requests are HTTPS-only, bounded, cookie-free and have no unrestricted redirect/fallback. Search and article API responses are limited to 1MiB and a 20-second operation budget. Article body content is not semantically moderated.

Generic parent-approved directories retain their explicit exact host/path scope, which can include multiple pages. Their category labels do not classify every page. Known adult/social/short-form host blocks are a small local defense, not a comprehensive internet filter. Disable sites that do not render usefully under these restrictions.

## Video limits

NASA remains an optional individual-review playback proof of concept. Parent **Videos** can discover bounded candidates, preview them, approve/reject and revoke review. Discovery is not approval. Children can access only reviewed items in enabled categories with unblocked metadata; playback downloads must match the approved SHA-256. No autoplay-next, unrestricted feed, YouTube stream extraction or external playback fallback.

Review content, attribution and rights yourself before approving. The app does not enforce watching an entire preview. Existing review decisions/candidates are preserved on upgrade. Discovery uses a 60-second operation budget, verified media transfer 120 seconds and native preparation a separate 30-second watchdog; all fail closed. Android worker cleanup and hardware behavior still require device observation.

YouTube discovery/playback is unavailable and its disabled setup controls are hidden. No compatible, suitability-reviewed non-NASA educational collection has been verified. Commons/Internet Archive research did not establish a safe working collection under current playback constraints. Manual NASA review does **not** fulfill the requested broad automatic video-search experience. See [video feasibility](docs/VIDEO_FEASIBILITY.md).

## Build and install on Windows

Requires JDK 17, Android SDK Platform 36 and Build-Tools 35. The project uses Gradle 8.13, AGP 8.13.2 and Kotlin 2.2.21. From this workspace:

```powershell
$env:JAVA_HOME='D:\Alpha\tools\jdk\jdk-17.0.20.1+1'
$env:ANDROID_HOME='D:\Alpha\tools\android-sdk'
$env:GRADLE_USER_HOME='D:\Alpha\tools\gradle-cache'
.\gradlew.bat --offline --daemon --max-workers=2 testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

On another machine use installed JDK/SDK paths and allow Gradle to obtain dependencies. Android Studio can create local.properties; manual SDK paths must escape the drive colon, e.g. `sdk.dir=D\:/Android/sdk`.

APK: `app/build/outputs/apk/debug/app-debug.apk`; **debug**, package **family.safe.debug**, minimum **Android 8.0 / API 26**. The test APK is separate and is not needed for a household installation. Final checksum, signature and actual current results are in [project state](docs/PROJECT_STATE.md).

```powershell
$adb='D:\Alpha\tools\android-sdk\platform-tools\adb.exe'
& $adb devices -l
# Adult verifies tablet identity and selects its physical serial first.
$serial='REPLACE_WITH_VERIFIED_PHYSICAL_SERIAL'
if ($serial -eq 'REPLACE_WITH_VERIFIED_PHYSICAL_SERIAL' -or $serial.StartsWith('emulator-')) { throw 'Select the physical tablet first.' }
Get-FileHash app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
& $adb -s $serial install -r app\build\outputs\apk\debug\app-debug.apk
```

`install -r` requests a data-preserving upgrade for the same package/signature. Compare the installed/reviewed signing certificate before upgrading. **Stop on signature/version mismatch; never uninstall or clear family data to make installation succeed.** Debug and release packages are separate. Keep one private stable signing key for later updates, backed up outside source control; never commit keys, PINs, settings, logs or APKs. This debug build is intended for supervised evaluation, not public distribution.

Follow the [non-destructive physical checklist](docs/PHYSICAL_DEVICE_CHECKLIST.md). Never run resetting instrumentation suites or `disposableInstrumentation=true` on a family tablet. Emulator tests do not establish household upgrade, persistence, reboot, rotation, performance, codec or WebView behavior.

## Protect the rest of the tablet

FamilySafe restricts only its own UI. An adult must configure Android and [Google Family Link](https://families.google/familylink/) where supported; labels vary by account/device:

1. Child → Screen time → Time limits → App limits: review/block alternative browsers, ordinary YouTube and social apps. Existing approvals need review; some system apps cannot be blocked. [Official app controls](https://support.google.com/families/answer/7103028).
2. Controls → Google Chrome and Web → Only allow approved sites. Review permissions too; these settings do not secure other browsers. [Official site controls](https://support.google.com/families/answer/7087030).
3. Controls → Google Play → Purchases & download approvals → Require approval for → All content. Existing approvals/family-library apps may not prompt again. [Official approval controls](https://support.google.com/families/answer/7039872).
4. Review guest accounts, unknown-app installation and account/settings access with the parent. Disable temporary USB debugging/install permissions after private setup when no longer needed.

These household steps are **NOT RUN**. Clearing data, uninstalling or resetting the tablet can bypass app-only setup where the device permits it. Adult supervision, provider-content review and hardware testing remain necessary.
