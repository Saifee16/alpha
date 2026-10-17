# NASA curated playback — 2026-10-17

## Closed Windows connectivity experiment — 2026-10-17

On baseline `41a0c3063d29cbb9555b27e01c76febfc2526434`, default sandbox search failed at DNS (`curl` exit 6, HTTP000, zero bytes), before TLS/HTTP. The authorized Windows network path succeeded with default TLS certificate verification, no redirects and no retries. `HTTP_PROXY`, `HTTPS_PROXY`, `ALL_PROXY` and `NO_PROXY` were absent; no proxy values or credentials were printed. The historical Loop 5 CONNECT403 was not reproduced on this path.

Live API search returned HTTP200, 22,485 bytes and valid JSON: 53 total hits, five returned records, all single JPL/video metadata records. The reference asset manifest returned HTTP200, 4,067 bytes and the exact canonical mobile MP4. Canonical HTTPS HEAD returned HTTP200, `video/mp4`, known length 11,103,471 bytes. A bounded full GET returned HTTP200 and exactly that length; SHA-256 `f595ec7b7715da39aa141581f0b78d41b0a41a3e619f796337a91f5ba5ef68ea` matches the historical reference. Successful requests exited 0 with `ssl_verify_result=0`; there was no CONNECT, TLS, HTTP or JSON parsing failure. Legacy HTTP manifest strings were compared locally, never requested.

This is live transport/byte-integrity evidence, not fixture output, current Android playback proof, content approval or rights clearance. No network settings or app networking were changed. Raw JSON, headers, command results, SHA-256 and downloaded media are ignored under `app/build/live-validation-evidence/connectivity/` (`SUMMARY.txt`, `commands.txt`, `search-parsed.json`, `manifest-parsed.json`, `asset-integrity.json`). Actual content/credits/rights review and physical-tablet validation remain required; current runtime evidence is recorded separately in [TEST_MATRIX.md](TEST_MATRIX.md).

The subsequent disposable-emulator batch ran 10 tests: 9 safety/browser fixture passes and 1 live discovery failure. `liveDiscoveryPersistsDeduplicatedPendingCandidatesAndReleasesPreview` received "No eligible playable candidates found" from the app and failed its nonempty-result assertion before preview. No content was approved; native preview/child playback were not reached. This result does not establish a network, parser or asset-filter cause. The experiment was closed when the user redirected priority to the general controlled browser; no further NASA investigation or policy relaxation followed. The opt-in regression remains as an honest reproduction, not a passing readiness gate.

Reproduce the bounded requests in PowerShell from the repository root with authorized network access. Each request follows no redirect, makes no automatic retry and retains default TLS verification; API bodies are capped at 1MiB and media at 32MiB. Stop on a nonzero curl exit, unexpected HTTP status/type/length, malformed JSON or absent exact manifest asset; do not relax controls. `--max-time` bounds each standalone command, not the app's shared operation budgets.

```powershell
$e = 'app/build/live-validation-evidence/connectivity'
New-Item -ItemType Directory -Force -Path $e | Out-Null
$api = 'https://images-api.nasa.gov'
$id = 'JPL-20230929-PSYCHEf-0002-Imager'
$asset = "https://images-assets.nasa.gov/video/$id/$id~mobile.mp4"
$bounds = @('--proto', '=https', '--tlsv1.2', '--connect-timeout', '10', '--max-time', '25', '--max-redirs', '0')
& curl.exe @bounds --max-filesize 1048576 --dump-header "$e/search-escalated.headers" --output "$e/search-escalated.json" --write-out 'http_code=%{http_code} ssl_verify_result=%{ssl_verify_result} size_download=%{size_download}\n' "$api/search?q=solar%20system&center=JPL&media_type=video&page_size=5"
& curl.exe @bounds --max-filesize 1048576 --dump-header "$e/manifest.headers" --output "$e/manifest.json" --write-out 'http_code=%{http_code} ssl_verify_result=%{ssl_verify_result} size_download=%{size_download}\n' "$api/asset/$id"
Get-Content -Raw "$e/search-escalated.json" | ConvertFrom-Json
Get-Content -Raw "$e/manifest.json" | ConvertFrom-Json
& curl.exe @bounds --head --dump-header "$e/asset-head.headers" --output "$e/asset-head.txt" --write-out 'http_code=%{http_code} ssl_verify_result=%{ssl_verify_result}\n' $asset
& curl.exe --proto '=https' --tlsv1.2 --connect-timeout 10 --max-time 45 --max-filesize 33554432 --max-redirs 0 --dump-header "$e/asset-download.headers" --output "$e/psyche-mobile.mp4" --write-out 'http_code=%{http_code} ssl_verify_result=%{ssl_verify_result} size_download=%{size_download}\n' $asset
Get-FileHash -Algorithm SHA256 "$e/psyche-mobile.mp4"
```

## Historical Loop 5: bounded operations and live-access blocker

Discovery shares a monotonic 60s budget across search/manifests/HEAD; player manifest/download/SHA-256 share 120s. Deadline/cancellation immediately marks the operation terminal and queues best-effort active-connection disconnection on two daemon threads (queue capacity 16), separate from UI/timer work. Connect/read timeouts are capped to the remaining budget; zero-progress reads deny. Android cleanup timing is unverified; there is no hard real-time guarantee. Existing per-request bounds, exact assets, hash approval, no redirects, no automatic retries and the separate native prepare 30s watchdog remain. Fresh validation **PASS**: 81 JVM tests/13 classes, 0 failures/errors/skips; both APKs built; lint 0 errors/12 unchanged warnings. Ten new operation tests include scheduled expiry with blocking fake I/O. All 21 instrumentation methods compiled without execution; no Android runtime is claimed.

The four official hosts are configured (`images-api.nasa.gov`, `images-assets.nasa.gov`, `images.nasa.gov`, `www.nasa.gov`), while runtime `network.state=unknown`. Actual API GET and canonical media HEAD attempts received HTTP 403 at proxy CONNECT (`curl` exit 56; initial strict attempt also 22/403). No NASA JSON/media was obtained, so current provider availability or live discovery cannot be assessed. Headers are ignored under `app/build/loop5-evidence/`. No network settings were changed, pushed or published. Resolve this external access blocker and rerun the same verified official paths; do not substitute feeds or relax transport/content policy.

## Historical Loop 4: bounded dynamic discovery

Parent settings now uses an enabled topic plus an entered exact approved phrase to discover candidates from the official NASA API. One request asks for five JPL/video records; there is no pagination or child-visible search/feed. Revalidate publisher/type, stable ID, bounded plain metadata and blocked terms. Resolve the asset manifest to the exact per-ID `images-assets.nasa.gov/video/{id}/{id}~mobile.mp4` path and require a successful no-redirect HEAD with MP4 type and known length ≤32MiB before saving a candidate. Reject malformed/unsupported records. An HTTP manifest string may only be upgraded to that exact HTTPS path; no HTTP request is made.

Persist at most 30 candidates with metadata, category and rights warning, never arbitrary URLs. Existing Europa/NISAR entries and approvals survive old-settings migration. Rediscovery does not overwrite reviewed metadata/category. Candidate removal removes approval. The parent still previews and individually confirms content/credits/rights; source and category are neither an age rating nor a license. Approval binds SHA-256, and child redownload must match. Pending/rejected/blocked/disabled-category/removed items remain unavailable.

Cloud validation **PASS**: 71 JVM tests (0 failures/errors/skips), both APKs built and lint 0 errors/12 warnings. New fixture coverage includes 8 discovery, 6 library/migration/adversarial and 3 playback-attempt checks; 21 instrumentation source tests compiled without execution. Live discovery is **BLOCKED** by the current network policy: allow `images-api.nasa.gov` and `images-assets.nasa.gov` before a meaningful live check. No NASA key is required. No Loop 4 device/runtime success is claimed. A 30-second prepare watchdog and stale-attempt guards address bounded preparation behavior, while the historical live-preview timeout remains unexplained. The timer starts after verified download; a total network deadline and prompt disconnect/cancellation are still limitations.

## Historical Loop 3 decision and evidence

**Decision: permit only parent-reviewed fixed candidates, with approval tied to downloaded bytes.** Source/category metadata does not establish suitability or rights. No general NASA feed is exposed. Existing YouTube behavior remains disabled. Final build passed 54 JVM tests and lint (0 errors/17 warnings). All 20 distinct emulator scenarios passed across overlapping batches, not a single clean 20/20 run. Final live test passed in 145.835s: both actual candidate previews, then Europa approval and child pause/seek/resume/completion/background release. An earlier live-preview timeout remains unexplained. The child-player screenshot showed a spacecraft frame; opening black preview screenshots are not visual proof. Physical-tablet checks remain NOT RUN. See [TEST_MATRIX.md](TEST_MATRIX.md) for separate evidence and incidents.

## Historical Loop 3 parent-curated Space workflow

| Candidate | Fixed NASA ID |
|---|---|
| Europa Clipper Mission Animations | `JPL-20220310-CLIPPRf-0001-Europa Clipper Mission Animations` |
| NISAR Animation Reel | `JPL-20250318-NISARf-0001-NISAR_Animation_Reel` |

In PIN-protected parent settings, enable **Space** in **Topics**, then open **Videos**. Select a candidate and preview its complete native local download. Preparation captures SHA-256; close preview, review full content/credits/rights, check the confirmation and approve or reject. Close/Back from preview returns to authorized foreground review; Back from settings locks access. Candidate IDs, decisions and approved SHA-256 persist in app-private settings. Approval is a parent's reviewed choice, not an automatic rights or suitability certification; fixture test approvals are not human review of the actual candidates.

Only a known approved item with Space enabled and metadata not blocked can reach child playback. The child redownload must match the approved SHA-256; replaced bytes fail closed and require re-review. Unknown/rejected/unreviewed items are unavailable. No child thumbnails/feed, arbitrary URLs, autoplay-next or external-app fallback exist.

The shared native path accepts exact HTTPS asset URLs without redirects, a known MP4 length no larger than 32MiB and a complete bounded copy. Playback receives a private local file. Background/pause releases playback and deletes the file; parent rotation/background locks access and clears selection, preview hash and session. The older unexported debug spike remains separate adult engineering evidence, not approval for these candidates.

See [TEST_MATRIX.md](TEST_MATRIX.md) for new verification status and [PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md) for physical checks. `NasaReviewFlowTest` deletes settings under disposable opt-in and must never run on a family tablet.

## Official source and terms

[NASA API v1.22 documentation](https://images.nasa.gov/docs/images.nasa.gov_api_docs.pdf) supports subject searches (`q`, `title`, `keywords`), publishing `center`, and `media_type=video`; `/album/{name}` is a named collection endpoint, not an age rating. `/asset/{nasa_id}` supplies media manifests. The implementation requests one five-result page for solar-system/JPL/video candidates for adult engineering evaluation, rechecks center/type, and follows no pagination or API-returned navigation URLs. Trusting JPL establishes publisher metadata, not ownership of every component.

[NASA media guidelines](https://www.nasa.gov/nasa-brand-center/images-and-media/) generally allow educational/informational reuse of NASA content, require source acknowledgment, reserve third-party rights, prohibit implied endorsement, and describe identifiable-person/publicity limitations. [NASA's brand center](https://www.nasa.gov/nasa-brand-center/) explicitly warns that licensed music/footage may require permission for further use. Direct MP4 delivery is not a separate license. Review credits and permissions before adding material to a family catalog; this prototype does not certify clearance or make a legal determination.

## Earlier proof-of-concept host checks

Network checks used curl with redirects disabled, after the sandbox DNS restriction required authorized network escalation.

| Check | Observed |
|---|---|
| `/search?q=solar%20system&center=JPL&media_type=video&page_size=3` | PASS: HTTP200 JSON, 53 hits; three returned records were JPL video |
| `/asset/JPL-20230929-PSYCHEf-0002-Imager` | PASS: HTTP200; mobile/preview/small/medium/large/original MP4 entries |
| HTTPS asset `collection.json` | PASS: HTTP200; legacy HTTP strings also present |
| Exact mobile asset over HTTPS HEAD | PASS: HTTP200, `video/mp4`, 11,103,471 bytes, no redirect |
| Byte range GET | PASS: HTTP206, 32-byte response, `Content-Range: bytes 0-31/11103471` |
| Full HTTPS MP4 + ffprobe | PASS: 265.4 seconds, H.264 320×180 and AAC |
| Full ffmpeg decode to null | PASS: exit0, no decoder errors |

Reference: [How Will #MissionToPsyche See Its Target Asteroid?](https://images.nasa.gov/details/JPL-20230929-PSYCHEf-0002-Imager). Metadata credits NASA/JPL-Caltech/ASU and True Story Films. Content, music and third-party rights have **not** been fully reviewed; the reference is exclusively an adult technical sample. Downloaded media lives under ignored `app/build/`, not source control. SHA-256: `f595ec7b7715da39aa141581f0b78d41b0a41a3e619f796337a91f5ba5ef68ea`.

## Earlier proof-of-concept implementation and evidence

`NasaClient` defaults disabled, uses injected transport for fixtures, bounds JSON to 1MiB, enforces HTTPS API transport, timeouts, no redirects/cache, and fails closed on errors. Manifest strings are accepted only for the exact official per-ID mobile path; legacy HTTP metadata is upgraded to HTTPS before any request. It never fetches HTTP.

Debug-only `NasaPlaybackSpikeActivity` has no child/parent-screen link. Register it unexported in the debug manifest and launch from an explicit in-process instrumentation intent; there are no extras. The adult taps **Download and test reference video**. Native controls remain inside the activity. Download requires the exact reference URL, HTTP200, MP4 type, a known length ≤32MiB and a complete bounded copy. Redirects, errors and unknown lengths deny. `VideoView` receives a private local file only, avoiding implicit network redirects by Android's media stack. Background/stop cancels, deletes and closes; no external intent or next-video fallback exists.

Nine JVM tests cover default denial, fixed discovery, metadata revalidation, manifest spoofing, invalid IDs, errors, size bounds, redirects, MIME/length/truncation, and successful local transfer. Root integration records actual Gradle/device outcomes in `TEST_MATRIX.md`; compilation is not runtime playback proof. Actual API35 tablet execution PASS: native duration 265645ms; playback advanced, pause held at 1072ms, seek/resume from 132822ms, completion and background release/cache deletion verified. The isolated live test passed in 23.314 seconds. Screenshot inspected: native video frame visible. Separate back/recreation behavior for this experimental player was not exercised; the family parent lifecycle suite passed.

The earlier spike did not review either Loop 3 animation candidate or verify its new child route. For the current loop, remaining gates are fresh integration evidence, live discovery/runtime verification, actual adult content/credits/rights review and physical-tablet validation. No second provider or unrestricted fallback is needed.
