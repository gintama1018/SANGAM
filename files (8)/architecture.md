# Architecture — Sangam

## High-level model
Single Android app, two modes on the same codebase:
- **Host mode**: runs an embedded local server + advertises the room
- **Client mode**: joins a room, watches its own Gallery, syncs to host

No separate backend server, no cloud infra. The host's phone *is* the server for the duration of the event — this is what makes it genuinely offline-capable and free to run.

## Components

| Module | Responsibility |
|---|---|
| Gallery Watcher | `ContentObserver` on MediaStore; filters new photos to `DCIM/Camera` within the room's active time window |
| Sync Client | Talks to host over local HTTP/WebSocket; uploads thumbnails, requests full-res on demand |
| Embedded Server (host only) | Ktor server + WebSocket; receives uploads, indexes metadata, broadcasts new-photo events |
| Crypto Layer | Session-key derivation (X25519 ECDH) from a QR-shared secret; encrypts payloads at the app layer, independent of network trust |
| Local Store | Per-device cache of thumbnails + metadata; host additionally stores full-res originals in app-sandboxed storage |
| Room/Grid UI | Compose-based thumbnail grid, live wall mode, reaction taps, attribution chips |
| Archive Module | Freezes room to read-only, exports local zip, generates highlight reel from top-reacted photos |

## Why this shape (rejected alternatives, and why)
- **BLE mesh** (reusing MeshWhisper's core): rejected for bulk transfer — BLE tops out around 1–2 Mbps, fine for text/metadata, too slow for dozens of full-res JPEGs at event scale.
- **Wi-Fi Direct group formation**: rejected as primary transport — GO-negotiation flakiness is a known pain point from MeshWhisper's own Wi-Fi module. Plain local HTTP over shared Wi-Fi/hotspot is simpler and far more demo-reliable.
- **In-app camera capture**: rejected — OEM camera pipelines (HDR+, Night mode) aren't fully reachable from third-party Camera2/CameraX, so a custom capture UI produces visibly worse photos than the stock camera app.
- **Pure browser/PWA** (the GuestPix/Fotify model): rejected as the primary path — a browser tab cannot silently watch the device Gallery in real time; that needs native storage permissions. Native app is a hard requirement for the zero-manual-upload differentiator. A read-only browser view for pure spectators is a fine stretch goal, not the core.

## Deployment model
- Host: one designated device per event (organizer's phone), running its own Wi-Fi hotspot or joined to shared venue Wi-Fi
- Guests: same app, client mode, connect via QR (embeds host IP:port + room ID + session key fragment)
- No app-store backend, no user accounts, no persistent server — the room exists only while the host's app is running

## Known structural limitation
The host is a single point of failure *and* a single point of trust (sees/controls all photos). This is an acceptable MVP trade-off — the host is a socially-trusted organizer, not a stranger — but it's a real limitation, not an oversight. Don't pitch this as infinitely scalable to large public events without addressing it (v2: optional distributed index, host handoff, multi-host redundancy).
