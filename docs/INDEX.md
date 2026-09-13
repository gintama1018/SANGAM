# Documentation Index — FrameRoom (Sangam)

This directory indexes the internal specifications, product requirements, system design, and security architecture documents for the FrameRoom project.

---

## Core Documentation

| Document | Description | Source File |
|---|---|---|
| **Product Requirements (PRD)** | User stories, non-goals, problem statement, success metrics, and competitive framing | [`files (8)/PRD.md`](../files%20(8)/PRD.md) |
| **System Architecture** | High-level host/client model, module decomposition, and transport rationale | [`files (8)/architecture.md`](../files%20(8)/architecture.md) |
| **System Design** | Data models (Room, Participant, Photo, Reaction), API endpoints, and core execution flows | [`files (8)/system-design.md`](../files%20(8)/system-design.md) |
| **Security & Privacy** | Threat model, mitigations, payload encryption, data retention, and non-goals | [`files (8)/security.md`](../files%20(8)/security.md) |
| **Build Plan** | Phased engineering roadmap, component milestones, and cut lists | [`files (8)/plan.md`](../files%20(8)/plan.md) |
| **UI Design System** | Stitch design tokens, color palette, typography scales, and component layouts | [`stitch_frameroom_event_gallery_app/frameroom/DESIGN.md`](../stitch_frameroom_event_gallery_app/frameroom/DESIGN.md) |

---

## Document Summaries

### [PRD.md](../files%20(8)/PRD.md)
Defines the core problem of event photo sharing, target users (campus events, college fests, small gatherings), key metrics (sub-10s photo availability over local Wi-Fi), and explicit v1 non-goals (no cloud accounts, no facial recognition, no AI moderation).

### [architecture.md](../files%20(8)/architecture.md)
Outlines the single-codebase dual-mode architecture (host mode vs. client mode), embedded Ktor server on the host, background MediaStore `ContentObserver`, X25519 session-key derivation, and why alternative transports (BLE mesh, Wi-Fi Direct GO) were rejected in favor of local HTTP over shared Wi-Fi/hotspot.

### [system-design.md](../files%20(8)/system-design.md)
Details the REST and WebSocket endpoints exposed by the host's embedded server (`/join`, `/photo/thumbnail`, `/photo/{id}/full`, `/reaction/{photo_id}`, `/room/close`, `/ws`), thumbnail-first synchronization, and full-resolution on-demand streaming.

### [security.md](../files%20(8)/security.md)
Documents threat mitigations against unauthorized room entry, MITM snooping on open Wi-Fi via application-layer encryption, accidental sync prevention via client-side bulk-confirmation, and data-retention lifecycles.
