# FamilySafe Product Rescue implementation plan

> For agentic workers: use subagent-driven-development for bounded search/video/test work and executing-plans for lead integration. No merge: the user reviews the real experience first.

**Goal:** Deliver a usable child Discover → Search → Read/Watch experience and a signed-compatible tablet-test APK.

**Spec:** User's complete FamilySafe Product Rescue brief in this chat (Phases A–G).

**Architecture:** Retain Kotlin/Compose, local settings, exact destination reader and verified private-file player. Replace the child configuration directory with content views. Run independently cancellable source searches in parallel, publish early results and enforce a 12-second whole-search budget. Keep video review default and extend a vetted catalog only after actual identity/media/runtime checks.

**Constraints:** Base remote main 18b4bc7; branch codex/familysafe-product-rescue. Preserve settings/PIN/approvals/schema, package family.safe.debug and certificate f88398895f95d7a226ff8a38bd96197686fbf6cf2cf88b3acd1f14836b332444. No auto collections, unrestricted providers/navigation, public APK or main merge. No destructive family-device tests. No raw child queries/settings in production logs.

- [x] Reproduce disclosure, title-only relevance, sequential search and startup rewrite with regressions; retain baseline APK.
- [x] Search agent: EducationalSearchClient/ChildSearchState + JVM tests. Structured bounded relevance, parallel clients, early results, deadline/cancellation and in-memory timings. Lead runs red/green builds.
- [x] Lead: ChildHome/FamilyApp Discover UI, lazy results, prominent search, friendly states, conditional honest Watch, PIN parent entry. No phrase/category/source configuration lists.
- [x] Lead: bounded question wording tests; parent quick reading setup with explicit consent; remove unnecessary startup rewrite; monotonic load/frame/PIN/player timings without weakening hashing.
- [x] Video agent: at most two legitimate repositories; verify new known originals, licensing, bytes, hashes/codecs. Lead integrates only after native playback; otherwise mark PARTIAL.
- [x] Acceptance agent: guarded device tests, ten real query outcomes, screenshots, monotonic repeated measurements, navigation/cancellation, private Watch/revocation and upgrade/restart preservation.
- [x] Lead: current JVM/build/lint/signatures; reviewed API35/API26 if available, physical NOT RUN without explicit authorization. Review full diff and fix failures.
- [x] Commit/push new branch; draft PR only. New separate private draft artifact FamilySafe-V1-UX-Fix-Pilot.apk, verified download/hash/cert, rollback instructions. Show evidence; await user UX approval before merge.

Review focus: stale callbacks after background or settings edits; blocked snippets/results; approved query without configured sources; source/topic revocation during playback; migration/relaunch retaining all security state. Test these boundaries rather than just happy paths.


Final boundaries: supervised pilot only. Broad video PARTIAL, live NASA discovery failed, performance targets not passed, API26 and physical tablet NOT RUN. See docs/UX_FIX_PILOT.md for evidence and unresolved limits. No merge or public APK publication.
