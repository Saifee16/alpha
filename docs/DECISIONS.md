# Decisions

Recorded 2026-10-17. One app module and a testable atomic JSON file keep the private MVP small; a backend, cloud accounts and a large dependency-injection framework were rejected as unnecessary. The cost is local-only setup and no remote recovery.

## Product boundary

- Build a private, native Android app with local parent setup and no backend.
- Begin with categories and sources disabled. Allow learning queries only when the complete normalized phrase matches a parent-enabled category and no blocked phrase matches.
- Keep settings in an app-private file, written atomically. Persist PIN attempt delays; malformed saved data locks access instead of resetting setup. Do not enable Android backup.
- Use a strict, read-only WebView: exact approved HTTPS host and path, GET only, no JavaScript, storage, forms, downloads, permissions, popups, or off-source redirects. Site rendering is unverified and may fail.

## Video

- Keep the official YouTube API disabled by default; no key ships in the app. Discovery cannot make playback safe: official player recommendations and links cannot be isolated, so playback remains blocked.
- Exact full-title authorization is deliberately conservative; many educational results will be rejected. Do not broaden metadata acceptance to a single topic keyword. Live source/topic tuning is gated behind API setup and testing.
- If discovery is enabled later, require Google's child-directed setup and applicable privacy disclosures, a restricted runtime key, bounded requests, parent-approved channels, and metadata expiry at 30 days. Never add a metadata cache.

## Parent supervision

- Recommend Family Link to restrict alternative browsers, YouTube and social apps, require approval for app installs, review installed apps, and restrict unknown sources/settings where the device allows it.
- FamilySafe is not a device lock or universal filter. System apps, clearing app data, uninstalling the app, or resetting the device can bypass app-level restrictions. Supervision remains necessary.

## Next work

1. Build, 36 unit tests, lint and both APK compilations passed. Next run the unverified device checks recorded in `PROJECT_STATE.md`.
2. Check the three starter sites on a device and adjust only the smallest necessary host/path rules.
3. Consider a controllable lawful video source only if it can meet the same isolation requirement.
