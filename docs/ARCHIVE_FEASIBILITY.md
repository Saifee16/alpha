# Bounded non-NASA video — 2026-10-18

The implementation supports one source-correlated Khan Academy lesson through Internet Archive, with individual parent approval. This is a limited catalog, not general Archive search, an approved collection, or automatic suitability/rights certification. Actual integrated parent discovery, verified preview, fixture-approved child playback, background cleanup and source revocation passed on the emulator after one initial transfer failure; the separate local-file Android codec probe passed too.

## Provider investigation

Wikimedia Commons was checked first using its official [TimedMediaHandler API](https://www.mediawiki.org/wiki/Extension:TimedMediaHandler/API): namespace 6, `filetype:video birds`, at most two results, with video URLs, derivatives and license metadata requested. The sandbox failed DNS (`curl` 6, HTTP000); the single authorized Windows network attempt reset during transport (`curl` 35, HTTP000, zero bytes). No actual Commons candidate/media was obtained. This does not establish that Commons is blocked on Android. No repeated diagnostics or control bypass was used. Evidence: ignored `app/build/video-gate/commons/`.

Internet Archive was the sole alternative. Its official advanced search and [item metadata API](https://archive.org/developers/md-read.html) returned actual HTTP200 JSON with verified TLS. An initial three-item animals/license query yielded unrelated uploads with no MP4/WebM within 32MiB; uploader license labels were not accepted as rights or educational provenance. One explicitly targeted query, `collection:khanacademy AND mediatype:movies AND title:addition`, then returned 21 hits with three records examined. All three had small MP4s; only **Addition 3** was selected for engineering validation. No pagination, further provider, broad import or automatic approval followed.

API requests used a descriptive project User-Agent, verified HTTPS, 5-second connect/20-second total bounds, a 1MiB JSON cap, no redirects and no retries. Targeted search returned 540 bytes; the three metadata responses were 8,746, 5,641 and 9,338 bytes. Raw queries, JSON, headers and exact outcomes are ignored under `app/build/video-gate/archive/`.

## Exact selected snapshot

| Field | Verified value |
|---|---|
| Lesson | Addition 3 — practice carrying digits to add multi-digit numbers |
| Item / local video ID | `KA-converted-e_SpXIw_Qts` / `archive:KA-converted-e_SpXIw_Qts` |
| Original file | `e_SpXIw_Qts.mp4` |
| Bytes | 7,794,483; within 32MiB |
| SHA-256 | `a1a9a7538fcdeac9f8cefca54fbd9aea42f4edacde63163ea444c8df127816e7` |
| Metadata MD5 | `f1a8b98462f0b63a78988073fbf32d94` |
| Metadata SHA-1 | `665f6d41202559974ba7876b6612bbee618b7fec` |
| Container / codecs | MP4; H.264 Constrained Baseline, 640×480, yuv420p; AAC-LC, 44.1kHz mono |
| Duration | ffprobe 537.402630 seconds; Android 537,403ms |

The [canonical download endpoint](https://archive.org/download/KA-converted-e_SpXIw_Qts/e_SpXIw_Qts.mp4) returned HEAD302; its redirect was not followed. The actual metadata `d1` and `dir` instead established [this exact official data-server file](https://dn601208.us.archive.org/0/items/KA-converted-e_SpXIw_Qts/e_SpXIw_Qts.mp4). Direct HEAD returned HTTP200, `video/mp4`, length 7,794,483; one bounded full GET returned exactly that length with TLS verified. Its MD5 matched metadata and its independently calculated SHA-256 is pinned above. The data-server host may change; the client must revalidate metadata and the fixed snapshot before building the endpoint.

## Publisher identity and license limits

Official [video oEmbed metadata for the exact original ID](https://www.youtube.com/oembed?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3De_SpXIw_Qts&format=json) returned HTTP200, title **Addition 3**, author **Khan Academy** and its official channel URL. This was a research identity check; production does not request YouTube or embed its player. The original Khan Academy lesson URL returned a generic application shell and was not treated as independent lesson-body proof. Sampled start/end frames show a `khanacademy.org` watermark and multi-digit arithmetic; they are not complete content/credits review.

The Archive item's individual `creator` and `licenseurl` fields are absent. That absence is not replaced with a fabricated per-item license. Khan Academy's current [official reuse policy](https://support.khanacademy.org/hc/en-us/articles/202262954-Can-I-use-Khan-Academy-s-videos-name-materials-links-in-my-project), updated July 15, 2026, expressly refers video/exercise use to [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/), its terms and brand policy. Attribution, license links, noncommercial use and clear unofficial/non-endorsement wording are required; adaptations have additional obligations. The policy was read through official web browsing; a separate raw curl request received HTTP403, which is recorded rather than represented as policy HTML.

This publisher policy plus exact original-video identity supports the engineering candidate; it is not a legal clearance or assurance that every archived Khan Academy item has the same provenance. [Archive's rights guidance](https://help.archive.org/help/rights/) explicitly disclaims copyright guarantees for item/collection statements. Adults must review this entire approximately nine-minute lesson, credits and applicable rights before approval. No catalog label, source consent or successful codec test replaces that review.

## Implemented boundary and remaining gates

Parent **Topics** must contain an enabled topic and saved approved phrase `addition`. In **Videos**, the parent opts into **Khan Academy via Internet Archive** for that topic, accepts query/network disclosure and selects **Find vetted lesson**. Discovery is restricted to the exact known identifier, collection, movie type and matching title, one result; all other uploads are discarded. No manual media URL, new provider framework or collection-wide approval is available.

Metadata resolution requires the exact lesson identity, original filename, byte count, MD5 and SHA-1. Only validated `ia`/`dn` numeric data hosts under `us.archive.org` and the exact item/file path are permitted. Download requires HTTP200, MP4, known exact length and complete bounded transfer with no redirects, cookies or cache. The existing private-file player separately verifies pinned SHA-256 before preview/approval/playback. Pending/rejected/revoked items, disabled source/category, blocked metadata and changed bytes remain unavailable. Existing operation deadlines and parent lifecycle gates apply.

The isolated API35 emulator probe passed native AVC/AAC playback progression, pause, seek/resume and stop. It used the downloaded local file, not the integrated family workflow. `probe-result.json` explicitly records background-policy testing and content approval as false. The separate production workflow test subsequently passed real API discovery, pinned-hash preview, explicit disposable fixture approval, child playback, background release/private-file deletion and source revocation in 103.003s. The initial two-test run had one live transfer failure (`SocketException`, no playback) and one passing late-search/parent-lock test; its cause remains unresolved. A System UI ANR was also observed and dismissed before the successful single-case rerun. Do not infer that it caused the transfer failure or claim a clean combined two-test run. Full adult review and physical-tablet validation remain separate gates. Exact build/test outcomes belong in [TEST_MATRIX.md](TEST_MATRIX.md).

Evidence: `app/build/video-gate/archive/SUMMARY.txt`, `publisher-policy.txt`, `khan-original-video.body`, `addition3-integrity.json`, `addition3-ffprobe.json`, `probe-result.json`, sampled frame PNGs, and exact request/response files. Raw media and evidence are ignored, not public APK/media distribution.
