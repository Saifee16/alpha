# Decisions

## V1 continuation decisions — 2026-10-18

- Keep existing settings schema and PIN/review records. Supported search sources are disabled on fresh setup; existing installations add missing sources explicitly in parent Websites. No silent re-enabling or reset migration.
- Authorize complete approved niches with at most one finite educational prefix and suffix. Send the canonical approved niche to the provider, never arbitrary appended words. Blocks override permission. Parent Test rules explains the decision locally without a network call.
- Integrate only two actual structured sources: English Vikidia and Wikijunior through [MediaWiki Search](https://www.mediawiki.org/wiki/API:Search). Require all normalized query words in a canonical title, with conservative plural/math handling; reject unrelated full-text hits. Five results/source, one page, no snippets/thumbnails/HTML cards, no fallback search. Narrow matching deliberately leaves some niches empty.
- Community editing requires explicit adult consent before enabling either source. Approved queries and ordinary connection metadata reach that provider; no accounts, cookies, analytics or saved search history. Results live only in foreground memory and clear on background. Neither source promises child suitability.
- Article selection narrows navigation to the exact selected canonical article, including anchors but excluding descendants and other articles. Read article bodies using the sanctioned [Parse API](https://www.mediawiki.org/wiki/API:Parsing_wikitext); require returned identity, positive revision/page IDs, bounded JSON and no redirect resolution. Preserve attribution, revision, source URL and license notice. [Vikidia reuse](https://en.vikidia.org/wiki/Vikidia:Reuse_of_Vikidia_content) uses CC BY-SA 3.0; [Wikibooks copyright](https://en.wikibooks.org/wiki/Wikibooks:Copyrights) generally uses CC BY-SA 4.0. Article-specific exceptions remain visible in body content. This adapted text view blocks images, scripts, interactive controls and third-party resources; no upload-host exception was introduced.
- Search requests and article API reads have a 20-second operation deadline, 5-second socket caps, a 1MiB response bound, no redirect/cookie/cache/retry and an identifying User-Agent. Main-frame URL authorization remains separate from internal exact API requests; children cannot navigate to `/w` APIs/edit pages.
- Independent QA reproduced a selected-article descendant escape in the initial prefix rule. Replaced it with exact article equality and retained the regression. Generic approved directories still expose their explicit host/path scope; they do not classify page content. Local known adult/social destination checks are intentionally incomplete defense in depth.
- Preserve optional NASA individual review and SHA-256 playback. Hide disabled YouTube setup controls. No defensible non-NASA video collection was verified: Commons access failed during bounded research and formats need codec validation; Internet Archive metadata did not establish a suitable rights-reviewed bounded MP4 collection. Do not substitute source trust for per-video review or claim low-maintenance video discovery is finished.
- Topic Save/Add and rule Test clear editor focus so the keyboard cannot hold the long parent page away from navigation. Test helpers explicitly scroll the vertical page before the horizontal tab row and assert the selected tab; no blind delays or direct state writes replace touch navigation.
- Optional settings export/import was omitted to avoid adding private-data transfer and merge complexity. No dependency, backend or generic provider framework was added.


## Historical decisions

## Product boundary

- Keep the product goal a controlled, private, multi-subject Android learning browser with local parent setup and no backend.
- Start categories and sources disabled. Allow only complete normalized phrases assigned to enabled categories, with blocked terms taking precedence. The current result is a trusted category website directory, not actual within-site search; allowed host/path scope does not automatically certify page content.
- Store settings in an app-private file with atomic writes. Persist PIN attempt delays; lock on malformed data rather than resetting parent setup. Exclude app data from Android backup.
- Use a strict read-only WebView restricted to an approved HTTPS host and path. Disable scripts, storage, forms, downloads, permissions and popups; deny off-source navigation.

## Search direction

- Parent-defined topics/niches retain exact complete-query matching. An explicit parent action suggests four finite wordings into the existing phrase editor; only Save authorizes them. Preserve originals, reject blocked/invalid inputs and over-limit output atomically, and never expand existing settings automatically. Keep the persistence schema and shared query evaluator unchanged.
- A denied submitted query must hide website launch results, just as it hides video results. An empty search may still show the approved directory; every destination retains the same host/path restrictions.
- Next consider one reviewed provider-specific structured search adapter for actual matching pages. This remains proposed work. Reuse the current local policy model; do not build a broad adapter framework.
- Keep NASA playback an optional proof-of-concept supporting the browser goal. Replacing per-video approval requires explicit user authorization plus a concrete suitability, rights and playback-isolation strategy; current approval gates stay in place.

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
