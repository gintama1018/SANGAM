# Sangam — Build Plan
*(working title — rename before the pitch if you land on something else)*

## Assumptions (read before building)
- Native Android app is required for uploaders — gallery auto-sync needs storage/media permissions a browser cannot get. This is a firm architectural constraint, not a style choice.
- A web-based, install-free view IS possible for pure spectators (read-only gallery link) — kept as a stretch feature, not MVP.
- No fixed hackathon deadline was given, so phases below assume a generic 2–3 day build window. Compress by cutting Phase 3–4 items if the window is shorter — never cut Phase 1 or 2.

## Phase 0 — Foundation (few hours)
- Repo scaffold: Kotlin + Jetpack Compose
- Pick embedded server library — Ktor server recommended (same language as the app, fewer moving parts than NanoHTTPD + a separate WebSocket lib)
- Permissions setup: `ACCESS_WIFI_STATE`, media permissions for Android 13+ (`READ_MEDIA_IMAGES`)

## Phase 1 — Core Room + Manual Fallback (Day 1)
- Host: create room → generates session key, starts embedded server, shows QR (host IP:port + room ID + session token)
- Guest: scan QR → joins over LAN → registers display name
- Manual upload button — this is your demo-day safety net if gallery-watch has bugs, do not skip it
- Thumbnail grid view, tap to view full photo

## Phase 2 — Gallery Auto-Detect (Day 1–2, the core differentiator — protect this time slot above everything else)
- `ContentObserver` on MediaStore, filtered to `DCIM/Camera` + room's active time window
- Detected-photo queue → bulk-confirm chip UI ("3 new photos — add to room?")
- Thumbnail-first sync (compress client-side before sending), full-res only on explicit request

## Phase 3 — Social Layer (Day 2)
- Per-photo attribution (uploader name/tag)
- Single reaction (heart/fire tap), aggregate count synced live to all clients
- Live wall mode — full-screen auto-refreshing grid for projector casting

## Phase 4 — Archive + Polish (Day 2–3)
- Host "close room" → freezes to read-only archive, generates a local export (zip)
- End-of-event highlight reel — top-reacted photos, a simple auto-slideshow is enough for a demo; skip real video rendering if time is short
- Empty states, error states, a performance pass at 100+ dummy photos

## Cut list, in order, if time runs out
1. Highlight reel video (slideshow is fine, or drop entirely)
2. Live wall projector mode
3. Reactions
4. Bulk-confirm UI — fall back to silent auto-upload, but say the privacy trade-off out loud in your pitch, don't hide it

Never cut Phase 1 or Phase 2 — they're not features, they're the product.
