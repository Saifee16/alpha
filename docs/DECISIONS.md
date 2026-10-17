# Decisions

## Product boundary

- Keep FamilySafe a native, private Android app with local parent setup and no backend.
- Start categories and sources disabled. Allow only complete normalized phrases assigned to enabled categories, with blocked terms taking precedence.
- Store settings in an app-private file with atomic writes. Persist PIN attempt delays; lock on malformed data rather than resetting parent setup. Exclude app data from Android backup.
- Use a strict read-only WebView restricted to an approved HTTPS host and path. Disable scripts, storage, forms, downloads, permissions and popups; deny off-source navigation.

## Video and sources

- Keep YouTube API access disabled by default; no key ships in the app. Keep YouTube child playback unavailable because official playback cannot isolate recommendations and linked content.
- Permit bounded parent-only NASA discovery for an enabled topic and an exact approved phrase: one page, at most five JPL/video records, validated metadata and exact official mobile MP4 assets. Save at most 30 candidates locally; preserve the original two Space items and existing decisions on migration. Never expose pending results or discovery to children.
- Discovery never approves, replaces reviewed metadata or expands a known item's category. Require per-item PIN-protected adult preview, content/credits/rights confirmation and digest approval. Candidate removal revokes its decision. Child eligibility requires the stored category enabled, unblocked metadata and approved SHA-256; redownload must match. Unknown, rejected, removed, changed and disabled-category items fail closed.
- Apply one monotonic 60-second deadline across discovery search/manifests/HEAD, and one 120-second deadline across player manifest/download/hash. Deadline and cancellation immediately mark the operation terminal and queue best-effort active-connection disconnect on a bounded two-thread executor, separate from UI/timer work; retain per-request bounds, no redirects and no automatic retries. Verify Android behavior separately; configured hosts or unknown runtime status do not establish access.
- Bound media preparation with a 30-second watchdog and attempt ownership so timed-out/cancelled/stale callbacks cannot revive playback. The historical live-preview timeout remains undiagnosed without runtime reproduction.
- Use exact HTTPS bounded local downloads (32MiB maximum), no redirects, child feed/thumbnails, manual URLs, autoplay-next or external-app fallback. Parent rotation/background clears selection and preview hash and locks access; backgrounding releases playback and deletes its temporary file.
- Keep National Geographic Kids disabled: it returned HTTP 200 but displayed a blank page in visual review. Do not relax JavaScript or URL restrictions for compatibility.
- NASA source labels and parent confirmation do not automatically establish suitability or third-party rights. Actual adult review and physical-tablet validation remain required. YouTube API discovery requires child-directed setup and restricted runtime credentials; the NASA workflow requires no API key.

## Supervision and validation

- Use Family Link to restrict alternative browsers, YouTube and social apps, require app install approval, and review existing apps and device settings where controls exist.
- FamilySafe is not a device lock or universal filter. Clearing app data, uninstalling where allowed, system apps and device reset can bypass app-level restrictions; adult supervision remains necessary.
- All 20 distinct Loop 3 emulator scenarios passed across overlapping batches, not a single clean 20/20 run; preserve the earlier unexplained live-preview timeout. `FamilyFlowTest` and `NasaReviewFlowTest` reset app-private settings with explicit disposable opt-in; never run them on a family tablet. Network-mutating review tests additionally require emulator hardware guards. Follow [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) for non-destructive physical checks.
