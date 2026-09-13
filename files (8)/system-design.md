# System Design — Sangam

## Data model

**Room**
- `room_id` (UUID)
- `name`, `theme/banner` (optional)
- `session_key` (derived, never stored raw)
- `created_at`, `active_window_start`, `closed_at` (nullable)
- `host_device_id`

**Participant**
- `device_id`
- `display_name`
- `joined_at`
- `role` (host / guest)

**Photo**
- `photo_id`
- `uploader_device_id`
- `thumbnail_blob` (synced to all clients)
- `full_res_path` (host-only, served on request)
- `exif_captured_at`
- `visibility` (public / private)
- `reaction_count`

**Reaction**
- `photo_id`, `device_id` — one reaction per device per photo, toggle on/off

## Core flows

**1. Room creation (host)**
1. Host taps "Create Room" → generates `room_id` + `session_key`
2. Embedded server starts, binds to local IP
3. QR generated encoding `{host_ip}:{port}/{room_id}/{session_key_fragment}`

**2. Join (guest)**
1. Scan QR → app extracts host IP/port/room_id/key fragment
2. HTTP handshake → X25519 ECDH using the key fragment → derives shared session key
3. Guest registers `display_name` → added to Participant list
4. Guest's Gallery Watcher activates, scoped to `active_window_start` onward

**3. Auto-upload**
1. `ContentObserver` fires on new MediaStore entry in `DCIM/Camera`
2. Filter: `exif_captured_at >= active_window_start`
3. Client generates a compressed thumbnail locally
4. Bulk-confirm chip shown ("3 new photos — add to room?") → one tap
5. Thumbnail (encrypted with session key) POSTed to host
6. Host indexes the photo, broadcasts a `new_photo` event over WebSocket to all connected clients
7. Full-res stays on the uploader's device until explicitly requested

**4. Download**
1. Any client taps a thumbnail → sends `request_full_res(photo_id)` to host
2. Host proxies the request to the original uploader if not cached, or serves directly if cached
3. File streamed back encrypted, saved to the requester's own device

**5. Room close**
1. Host taps "Close Room" → broadcasts `room_closed` to all clients
2. Gallery Watchers deactivate on all devices
3. Host generates a local manifest (JSON) + offers a zip export
4. Room becomes a read-only archive (viewable, not uploadable) until host deletes the app data

## API surface (host's embedded server)

| Endpoint | Method | Purpose |
|---|---|---|
| `/join` | POST | Guest handshake + registration |
| `/photo/thumbnail` | POST | Upload a thumbnail |
| `/photo/{id}/full` | GET | Request full-res download |
| `/reaction/{photo_id}` | POST | Toggle reaction |
| `/room/close` | POST | Host-only, freezes the room |
| `/ws` | WebSocket | Live events: `new_photo`, `new_reaction`, `room_closed` |

## Performance considerations
- Thumbnail-first sync keeps the live wall responsive even with 100+ photos
- Full-res transfer is pull-based and rate-limited per request, so simultaneous downloads from many clients don't saturate the host's radio
- Target: grid stays responsive up to ~500 photos per room without pagination changes; beyond that, add lazy-loading (not needed at hackathon demo scale)
