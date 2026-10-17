# Decisions

## Product boundary

- Keep FamilySafe a native, private Android app with local parent setup and no backend.
- Start categories and sources disabled. Allow only complete normalized phrases assigned to enabled categories, with blocked terms taking precedence.
- Store settings in an app-private file with atomic writes. Persist PIN attempt delays; lock on malformed data rather than resetting parent setup. Exclude app data from Android backup.
- Use a strict read-only WebView restricted to an approved HTTPS host and path. Disable scripts, storage, forms, downloads, permissions and popups; deny off-source navigation.

## Video and sources

- Keep YouTube API access disabled by default; no key ships in the app. Keep YouTube child playback unavailable because official playback cannot isolate recommendations and linked content.
- Limit NASA child playback to the two fixed animation candidates after PIN-protected adult preview, content/credits/rights confirmation and approval. Persist decisions and approved download SHA-256 locally; require a matching hash on child redownload. Unknown, rejected, changed, blocked-metadata or Space-disabled items fail closed.
- Use exact HTTPS bounded local downloads (32MiB maximum), no redirects, child feed/thumbnails, manual URLs, autoplay-next or external-app fallback. Parent rotation/background clears selection and preview hash and locks access; backgrounding releases playback and deletes its temporary file.
- Keep National Geographic Kids disabled: it returned HTTP 200 but displayed a blank page in visual review. Do not relax JavaScript or URL restrictions for compatibility.
- NASA source labels and parent confirmation do not automatically establish suitability or third-party rights. Actual adult review and physical-tablet validation remain required. YouTube API discovery requires child-directed setup and restricted runtime credentials; the fixed NASA workflow requires no API key.

## Supervision and validation

- Use Family Link to restrict alternative browsers, YouTube and social apps, require app install approval, and review existing apps and device settings where controls exist.
- FamilySafe is not a device lock or universal filter. Clearing app data, uninstalling where allowed, system apps and device reset can bypass app-level restrictions; adult supervision remains necessary.
- All 20 distinct Loop 3 emulator scenarios passed across overlapping batches, not a single clean 20/20 run; preserve the earlier unexplained live-preview timeout. `FamilyFlowTest` and `NasaReviewFlowTest` reset app-private settings with explicit disposable opt-in; never run them on a family tablet. Network-mutating review tests additionally require emulator hardware guards. Follow [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) for non-destructive physical checks.
