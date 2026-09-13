# Product Requirements Document — Sangam
*(working title, rename freely)*

## Problem
Group/event photo sharing today has two broken paths:
1. **In-the-moment sharing (WhatsApp):** N people → N×N manual select-and-send actions. Painful, repetitive, someone always forgets. "Bhai meri photo khinch ke bhej de" cycle repeats endlessly.
2. **Post-event sharing (Google Drive):** Photos collected days/weeks later, dumped in a folder, browsed one photo at a time like a document viewer, near-impossible to find "my" photos among hundreds.

## Target users
- College students at fests, farewells, small-to-mid campus events
- Secondary: any small-group event organizer (birthdays, trips, sangeet-scale functions)

## Goals
- Zero manual-upload photo collection during an event
- Instant, browsable, attributed gallery — live, not weeks later
- No internet dependency — works on local Wi-Fi/hotspot only
- Free / near-zero cost to run (no cloud storage bill)

## Non-goals (v1)
- No custom in-app camera — always pulls from device Gallery, never captures directly
- No cross-venue / cross-network sync — host and guests must share one local network
- No facial recognition or auto-tagging
- No permanent cloud backup — local-only, explicitly a "this event only" tool, not a lifetime photo cloud
- No AI content moderation — user-driven private/hide toggle only (competitors like Fotify have AI moderation; deliberately deferred, disclose this in the pitch)

## User stories
- As a guest, I open the app, scan the room QR, and my new photos start appearing without me doing anything else beyond a single confirm tap.
- As a guest, I can mark a photo private before it syncs, so an awkward or personal shot never reaches the room.
- As any room member, I can browse everyone's photos in one grid and download only the ones I want.
- As the host, I can see who's contributing, close the room when the event ends, and export everything as an archive.
- As anyone, I can see who took a great shot — attribution is visible, so good photographers get noticed instead of buried in a shared folder.

## Success metrics (hackathon demo / pilot)
- Time from "photo clicked" to "visible in room": under 10 seconds on local Wi-Fi
- Zero *selection* steps for a normal upload (bulk-confirm tap counts as one action, not zero — that's intentional, see Security doc)
- Demo target: 5+ devices in one room, 50+ photos, no crash, live wall updates in real time

## Competitive framing — say this explicitly in the pitch
Tools like GuestPix, Fotify, EventPics, GuestSnap already solve "guest uploads via QR, no login" — but all of them require **manual browser upload** and **internet connectivity to their cloud**, and they charge $30–50 per event. Sangam's actual wedge is the mechanism, not the category:
- Automatic gallery-sync — no manual upload step, ever
- Fully local/offline operation — no venue internet required
- Free, built for the Indian college-fest budget (₹0), not the Western wedding-SaaS budget

Don't pitch "nobody's done event photo sharing" — that's false and a judge will catch it in ten seconds. Pitch "everyone else makes you upload; this one doesn't, and it doesn't need internet."
