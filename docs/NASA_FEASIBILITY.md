# NASA proof of concept — 2026-10-17

**Decision: technically feasible for controlled native playback; child access remains disabled.** Source/category metadata does not establish suitability or rights. No general NASA feed is exposed. Existing YouTube behavior is unchanged.

## Official source and terms

[NASA API v1.22 documentation](https://images.nasa.gov/docs/images.nasa.gov_api_docs.pdf) supports subject searches (`q`, `title`, `keywords`), publishing `center`, and `media_type=video`; `/album/{name}` is a named collection endpoint, not an age rating. `/asset/{nasa_id}` supplies media manifests. The implementation requests one five-result page for solar-system/JPL/video candidates for adult engineering evaluation, rechecks center/type, and follows no pagination or API-returned navigation URLs. Trusting JPL establishes publisher metadata, not ownership of every component.

[NASA media guidelines](https://www.nasa.gov/nasa-brand-center/images-and-media/) generally allow educational/informational reuse of NASA content, require source acknowledgment, reserve third-party rights, prohibit implied endorsement, and describe identifiable-person/publicity limitations. [NASA's brand center](https://www.nasa.gov/nasa-brand-center/) explicitly warns that licensed music/footage may require permission for further use. Direct MP4 delivery is not a separate license. Review credits and permissions before adding material to a family catalog; this prototype does not certify clearance or make a legal determination.

## Actual host checks

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

## Minimal implementation and gates

`NasaClient` defaults disabled, uses injected transport for fixtures, bounds JSON to 1MiB, enforces HTTPS API transport, timeouts, no redirects/cache, and fails closed on errors. Manifest strings are accepted only for the exact official per-ID mobile path; legacy HTTP metadata is upgraded to HTTPS before any request. It never fetches HTTP.

Debug-only `NasaPlaybackSpikeActivity` has no child/parent-screen link. Register it unexported in the debug manifest and launch from an explicit in-process instrumentation intent; there are no extras. The adult taps **Download and test reference video**. Native controls remain inside the activity. Download requires the exact reference URL, HTTP200, MP4 type, a known length ≤32MiB and a complete bounded copy. Redirects, errors and unknown lengths deny. `VideoView` receives a private local file only, avoiding implicit network redirects by Android's media stack. Background/stop cancels, deletes and closes; no external intent or next-video fallback exists.

Nine JVM tests cover default denial, fixed discovery, metadata revalidation, manifest spoofing, invalid IDs, errors, size bounds, redirects, MIME/length/truncation, and successful local transfer. Root integration records actual Gradle/device outcomes in `TEST_MATRIX.md`; compilation is not runtime playback proof. Tablet play/pause/completion/back/recreation checks remain **NOT RUN** until executed there.

Next gate: adult review of a small Space collection's content/credits, then tablet codec/control/lifecycle validation. Only that reviewed catalog should reach child navigation; no new universal requirement to approve every educational video is introduced. No second provider or unrestricted fallback is needed.
