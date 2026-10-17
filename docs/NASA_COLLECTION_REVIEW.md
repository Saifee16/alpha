# NASA Space review collection

Research checked 2026-10-17. This is a fixed parent-review shortlist, not a child-safe feed or a completed content/rights clearance. No item starts approved. Parents must preview the entire item, inspect its credits and rights, and explicitly confirm content and rights review before approving it. Approval is local to this family and does not grant copyright permission.

## Official candidates

| NASA ID | Display title | Credit | HTTPS MP4 bytes |
| --- | --- | --- | ---: |
| `JPL-20220310-CLIPPRf-0001-Europa Clipper Mission Animations` | Europa Clipper Mission Animations | NASA/JPL-Caltech | 27,146,112 |
| `JPL-20250318-NISARf-0001-NISAR_Animation_Reel` | NISAR Mission Animation Reel | NASA/JPL-Caltech; animation by Koji Kuramura and Michael Stetson; directed by Paul A. Rosen | 25,401,378 |

Descriptions in the app are short editorial summaries, without metadata links. Both are mission visualizations; the NASA metadata says animation for media/public use. This supports a limited educational review shortlist, not an assurance that all audiovisual components are free of separate rights.

Official metadata and manifests:

- [Europa Clipper metadata](https://images-api.nasa.gov/search?nasa_id=JPL-20220310-CLIPPRf-0001-Europa%20Clipper%20Mission%20Animations), [asset manifest](https://images-api.nasa.gov/asset/JPL-20220310-CLIPPRf-0001-Europa%20Clipper%20Mission%20Animations), [mission animation context](https://science.nasa.gov/missions/europa-clipper/europa-clipper-resources/europa-clipper-mission-animations/).
- [NISAR metadata](https://images-api.nasa.gov/search?nasa_id=JPL-20250318-NISARf-0001-NISAR_Animation_Reel), [asset manifest](https://images-api.nasa.gov/asset/JPL-20250318-NISARf-0001-NISAR_Animation_Reel).

Each manifest supplies an exact `/video/{id}/{id}~mobile.mp4` asset on `images-assets.nasa.gov`. Manifests currently contain legacy HTTP strings; the client upgrades only this exact origin/path to HTTPS before any request. Checked HTTPS responses returned HTTP 200, `video/mp4`, and known lengths below 32 MiB. Host transfer checks also compare the actual bytes to the advertised length and inspect the MP4 `ftyp` signature; these transport checks do not establish age suitability or review credits. Parent approval stores the SHA-256 of the actual local preview file. Child playback verifies the downloaded bytes against that approved digest before opening VideoView. NASA may replace files at the same ID; changed bytes must deny playback and require a new parent preview and explicit content/rights approval.

Host `ffprobe` inspection of those exact mobile copies found H.264 video at 320 x 180 and an AAC audio stream in **both** files. Europa Clipper is 614.066667 seconds (about 10:14); NISAR is 592.8 seconds (about 9:53). An audio stream does not establish whether the content is silence, music, speech, or sound effects. No no-music claim is made. These are host-file checks, not proof of playback or completion on an Android tablet.

Recorded research copies (ignored build evidence, not packaged app assets):

| File in `app/build/loop3-evidence/` | SHA-256 |
| --- | --- |
| `europa-mobile.mp4` | `eb2f0d8a4bef187c9d2350881fbe37dabbe15e68327559712b113e6f7030ae93` |
| `nisar-mobile.mp4` | `3496c94a0643d20b19119cd4975c57fa3a489655bd42662a07cb305ace8827fc` |

Both copies decoded fully on the host with `ffmpeg -nostdin -v error -xerror -i <file> -map 0 -f null NUL`: exit 0 and no errors. Successful decode is a media-integrity check, not a human review of the audiovisual content.

These hashes identify research bytes only and are never seed approvals. A parent's actual preview file supplies each approval digest.

## Rights and limitations

[NASA media guidance](https://www.nasa.gov/nasa-brand-center/images-and-media/) generally permits educational/informational use of NASA material with attribution, but third-party material can have separate rights that NASA cannot grant. NASA hosting does not clear third-party footage, narration, music, logos, or a person's likeness. Do not imply NASA endorsement. The guidance is US-specific and is not a worldwide legal clearance.

[JPL's image use policy](https://www.jpl.nasa.gov/jpl-image-use-policy/) covers images/video, requires credit, disclaims ownership warranties, and identifies separate rules for third-party material, logos, identifiable people and commercial use. The public metadata for these two reels does not list a music license. We have not established absence of music, audited every frame/audio track, or obtained individual third-party permissions. Parents should withhold approval if the full preview or credits reveal an unresolved restriction. Release or commercial redistribution needs its own review.

The earlier engineering reference `JPL-20230929-PSYCHEf-0002-Imager` remains available only to its existing debug spike. Its [official metadata](https://images-api.nasa.gov/search?nasa_id=JPL-20230929-PSYCHEf-0002-Imager) names NASA/JPL-Caltech/ASU and producer True Story Films. Production and music rights were not individually resolved, so it is excluded from the parent shortlist. Exclusion is a conservative choice, not a finding of infringement.

## Runtime boundary

The catalog contains only IDs, text, credits and rights notes; category is fixed to `space` and source to `NASA official`. No thumbnail, arbitrary URL field, external link action, or remote player is added. Parent preview/child playback must resolve a catalog ID, check the current manifest, download only its exact HTTPS mobile MP4 with redirects/caches disabled, validate HTTP 200/type/known length, and finish a bounded local copy before a local-only player receives it. Child VideoView receives the local file only after its SHA-256 matches the digest approved from the parent's preview. Unknown IDs, changed files and unapproved child items must deny. The reference wrapper retains the same network boundary.

The inspected AOSP local-file path opens a file descriptor and uses GenericSource; HLS selection occurs on a separate URL path. The MP4 extractor uses its original data source rather than resolving external data-reference URLs. Both checked files have self-contained `dref` entries. This supports the local-file boundary, not a universal claim about every OEM implementation. Primary implementation references: [MediaPlayer](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/media/java/android/media/MediaPlayer.java#1219), [NuPlayer](https://android.googlesource.com/platform/frameworks/av/+/refs/heads/main/media/libmediaplayerservice/nuplayer/NuPlayer.cpp#311), [MPEG4Extractor](https://android.googlesource.com/platform/frameworks/av/+/refs/heads/main/media/module/extractors/mp4/MPEG4Extractor.cpp#3309).
