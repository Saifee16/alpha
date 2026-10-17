# Decisions

## Product boundary

- Keep FamilySafe a native, private Android app with local parent setup and no backend.
- Start categories and sources disabled. Allow only complete normalized phrases assigned to enabled categories, with blocked terms taking precedence.
- Store settings in an app-private file with atomic writes. Persist PIN attempt delays; lock on malformed data rather than resetting parent setup. Exclude app data from Android backup.
- Use a strict read-only WebView restricted to an approved HTTPS host and path. Disable scripts, storage, forms, downloads, permissions and popups; deny off-source navigation.

## Video and sources

- Keep YouTube API access disabled by default; no key ships in the app. Keep child playback unavailable because official playback cannot isolate recommendations and linked content.
- NASA playback proof stays in the unexported, debug-only adult harness; it does not enable a child playback route.
- Keep National Geographic Kids disabled: it returned HTTP 200 but displayed a blank page in visual review. Do not relax JavaScript or URL restrictions for compatibility.
- Next video milestone is a parent-curated NASA collection and third-party rights review. YouTube API discovery requires child-directed setup and restricted runtime credentials; NASA discovery requires no API key.

## Supervision and validation

- Use Family Link to restrict alternative browsers, YouTube and social apps, require app install approval, and review existing apps and device settings where controls exist.
- FamilySafe is not a device lock or universal filter. Clearing app data, uninstalling where allowed, system apps and device reset can bypass app-level restrictions; adult supervision remains necessary.
- Fourteen instrumentation tests passed on the dedicated API 35 emulator. The reset guard requires the debug package and explicit runner opt-in, resets app-private settings, and does not verify emulator identity; use it only on a disposable emulator.
