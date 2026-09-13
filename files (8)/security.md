# Security & Privacy — Sangam

## Threat model

| Threat | Mitigation |
|---|---|
| Random stranger joins the room (guesses/overhears a code) | Join requires scanning the QR itself — no shareable short code exists; host can see and kick participants |
| Man-in-the-middle on open venue Wi-Fi | App-layer encryption via an X25519-derived session key — security doesn't depend on network trust, even on an open/shared hotspot |
| A photo gets uploaded that someone didn't consent to share | Bulk-confirm step before every sync (never fully silent auto-upload by default) + a per-photo "mark private" toggle before it ever leaves the device |
| Host device lost or stolen mid-event | Full-res photos stored in app-private storage, not public Gallery, on the host side; add local encryption at rest (same pattern proven in the MeshWhisper build: SQLCipher-backed store), reimplemented independently |
| Malicious upload (path traversal, oversized file, corrupt EXIF) targeting the embedded server | Server validates MIME type + EXIF before accepting, writes only to app-sandboxed storage, enforces a per-photo size cap |
| Host has full visibility/control over all photos (trust concentration) | Documented as an accepted MVP trade-off, not hidden — the host is a socially-trusted organizer, not a stranger; flag as a v2 item if pitching beyond hackathon scope |
| Room persists indefinitely, photos linger on the host's device | "Close room" freezes uploads but does not auto-delete; add an explicit in-app data-retention notice and a manual "delete room data" action — don't let this silently become a permanent archive of every attendee's face |

## What this product explicitly does NOT do
State these plainly in the pitch — don't wait to be asked:
- No cloud storage — photos never leave the local network in v1
- No account system — no phone numbers, no emails collected
- No AI content moderation (unlike Fotify) — the private/hide toggle is the only content control in v1
- No facial recognition or auto-tagging

## Data retention default
Recommended default: room data auto-purges from the host's app storage a fixed number of days after `closed_at`, unless the host explicitly exports it first. This is what stops the tool from quietly turning into a permanent surveillance-style archive of everyone who attended.
