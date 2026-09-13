---
name: FrameRoom
colors:
  surface: '#111318'
  surface-dim: '#111318'
  surface-bright: '#37393e'
  surface-container-lowest: '#0c0e12'
  surface-container-low: '#1a1c20'
  surface-container: '#1e2024'
  surface-container-high: '#282a2e'
  surface-container-highest: '#333539'
  on-surface: '#e2e2e8'
  on-surface-variant: '#d8c3ad'
  inverse-surface: '#e2e2e8'
  inverse-on-surface: '#2f3035'
  outline: '#a08e7a'
  outline-variant: '#534434'
  surface-tint: '#ffb95f'
  primary: '#ffc174'
  on-primary: '#472a00'
  primary-container: '#f59e0b'
  on-primary-container: '#613b00'
  inverse-primary: '#855300'
  secondary: '#c0c1ff'
  on-secondary: '#1000a9'
  secondary-container: '#3131c0'
  on-secondary-container: '#b0b2ff'
  tertiary: '#8fd5ff'
  on-tertiary: '#00344a'
  tertiary-container: '#1abdff'
  on-tertiary-container: '#004966'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffddb8'
  primary-fixed-dim: '#ffb95f'
  on-primary-fixed: '#2a1700'
  on-primary-fixed-variant: '#653e00'
  secondary-fixed: '#e1e0ff'
  secondary-fixed-dim: '#c0c1ff'
  on-secondary-fixed: '#07006c'
  on-secondary-fixed-variant: '#2f2ebe'
  tertiary-fixed: '#c5e7ff'
  tertiary-fixed-dim: '#7fd0ff'
  on-tertiary-fixed: '#001e2d'
  on-tertiary-fixed-variant: '#004c6a'
  background: '#111318'
  on-background: '#e2e2e8'
  surface-variant: '#333539'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 38px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.04em
  metadata-mono:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-tablet: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an intimate, cinematic canvas built specifically for live collective photography and collaborative event memories. Marrying the exacting precision of Apple Photos with the spontaneous social intimacy of BeReal and the restrained prestige of editorial lookbooks, the aesthetic places visual imagery at the absolute forefront while treating interface chrome as translucent, atmospheric glass overlays.

The visual language blends modern dark-mode minimalism with refined glassmorphic depth:
- **Atmospheric Immersion:** The interface acts as a darkened exhibition hall or camera viewfinder. Surfaces stay unobtrusive, recessive, and dark, letting vibrant photography burst forward without chromic clutter.
- **Sensory Tactility:** Floating glass lenses, pills, and tactile haptic affordances evoke precision camera hardware and intuitive mobile-native interaction.
- **Live Event Energy:** Electric warm amber/gold bursts celebrate spontaneous shutter clicks, flash captures, and live synchronized participant activity.

## Colors

The palette is engineered for pure contrast against high-dynamic-range imagery, operating exclusively in dark mode to preserve battery and maintain visual focus during nocturnal and indoor gatherings.

### Palette Roles
- **Primary (`#F59E0B` Amber Gold):** Reserved for live shutter moments, active party sessions, primary action buttons, capture notifications, and glowing state indicators. It carries the warmth of a vintage tungsten flash and warm event lighting.
- **Secondary (`#6366F1` Indigo Violet):** Used for participant tags, collaborator presence badges, secondary links, room invites, and ambient aura highlights.
- **Canvas Base (`#0A0C10` Deep Onyx):** The deepest foundation layer; provides an infinite black photographic backdrop.
- **Surface Elevation (`#12151C` Obsidian Base & `#181C26` Translucent Glass):** Stacked containers and cards featuring micro-diffused light and ambient contrast.
- **Borders & Separators (`rgba(255, 255, 255, 0.08)`):** Hairline translucent stroke boundaries that create crisp glass edges without adding optical weight.
- **Typography & Glyphs:** Primary text in `#FFFFFF` (100% white) for ultra-clear legibility over dark surfaces; secondary metadata in `#94A3B8` (slate neutral) for timestamps, camera EXIF details, and collaborator tallies.

## Typography

Typography uses `Inter` throughout all tiers, configured with tight tracking on headlines to deliver an authoritative, editorial punch akin to modern gallerist magazines and Apple interface design. 

### Guidelines
- **Editorial Impact:** Large display and headline titles apply tighter negative letter-spacing (`-0.02em` to `-0.03em`) to mimic physical title print and premium typography.
- **Tabular & Camera Metadata:** Use font-variant numeric settings (`tnum`, `zero`) for timestamps, contributor counts, shutter speeds, and live counters (`metadata-mono`) to prevent layout shifts during live uploads.
- **Uppercase Labels:** Subheadings, category indicators, and status tags apply uppercase styling with increased letter spacing (`0.04em`) to establish visual hierarchy across dense image feeds.

## Layout & Spacing

The layout is built around mobile-first fluid media grids that transition from single-column vertical social feeds into multi-column editorial mosaics.

### Grid & Responsiveness
- **Mobile Handset (<640px):** 2-to-3 column dynamic masonry or 1-column hero spotlight with `margin` of `1rem` and compact `0.75rem` gutters. Floating action controls anchor to the safe area bottom.
- **Tablet (641px - 1024px):** 4-column feed layout with fluid column expansion and `1.5rem` margins, enabling split view between the live synchronized event roll and individual high-resolution photo inspection.
- **Desktop / Large Display (>1024px):** Fixed max-width container (`1280px`) centered on the onyx base, using 6 to 12 structural columns with `2.5rem` outer margins.

### Spacing Rhythm
- **Internal Padding:** `space-sm` (8px) and `space-md` (16px) govern card paddings and interactive glass pills.
- **Media Separation:** Never allow photo tiles to blend into each other without the designated structural `gutter` or hairline separation.

## Elevation & Depth

Depth is established via frosted glass surfaces, controlled luminous backdrops, and glowing amber ambient shadows rather than stark drop shadows.

### Elevation Hierarchy
- **Base Canvas (Level 0):** `#0A0C10` solid backdrop, non-reflective and neutral.
- **Surface Tiles & Cards (Level 1):** `#181C26` with `80%` opacity, paired with `backdrop-filter: blur(16px)` and a `1px` translucent border of `rgba(255, 255, 255, 0.08)`.
- **Floating Controls & Modals (Level 2):** `#12151C` with `75%` opacity, `backdrop-filter: blur(24px)`, and a diffused ambient shadow: `0 8px 32px rgba(0, 0, 0, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.12)`.
- **Active & Flash State (Level 3 Glow):** Key interactive triggers (such as the live Shutter Button and pulsing Live badges) leverage colored ambient glows: `0 0 24px rgba(245, 158, 11, 0.35)` or `0 0 20px rgba(99, 102, 241, 0.3)`.

## Shapes

The shape system draws direct inspiration from iOS continuity and modern camera apertures, standardizing on smooth corner smoothing.

- **Standard Cards & Image Frames:** `1rem` (16px) for standard event tiles; `1.5rem` (24px) for hero spotlight cards and modal sheets.
- **Pill Architecture (`9999px`):** Floating toolbars, participant counter chips, camera switcher toggles, reaction bubbles, and action triggers are strictly rendered as full capsules (pills).
- **Nested Corner Principle:** Inner elements maintain proportional concentric curves (outer radius minus padding equals inner radius) to ensure visual harmony within glass containers.

## Components

### Buttons
- **Primary Shutter / Action Button:** Pill-shaped or concentric circle (`9999px`). Background: `#F59E0B` solid or linear gradient to `#D97706`. Label: `#0A0C10` bold text. Subtle amber aura glow on tap/hover.
- **Secondary Glass Button:** Translucent glass surface (`rgba(24, 28, 38, 0.8)`), `1px` border of `rgba(255, 255, 255, 0.12)`, text in `#FFFFFF`. Hover shifts opacity to `100%` and lightens border to `rgba(255, 255, 255, 0.2)`.
- **Icon Action Pucks:** `44x44px` circular glass buttons with centered white micro-icons (flash, flip camera, bookmark, share).

### Chips & Badges
- **Live Pulsing Badge:** Ultra-compact pill containing a 6px circular dot pulsing with an amber ping animation (`#F59E0B`), followed by uppercase `label-sm` text "LIVE".
- **Collaborator / Room Filter Chip:** Height `32px`, pill shape, frosted background (`rgba(255, 255, 255, 0.06)`). Selected state highlights with secondary indigo border (`#6366F1`) and tinted background (`rgba(99, 102, 241, 0.15)`).

### Photo Cards & Media Mosaic
- **Image Frame:** Clamped to `rounded-lg` (16px) or `rounded-xl` (24px). Photos fill container with `object-fit: cover`.
- **Glass Metadata Overlay:** Floating at bottom edge of image card; blurred scrim with micro-avatar cluster of contributors, upload timestamp, and reaction counter.

### Inputs & Search Fields
- **Glass Input Bar:** Height `48px`, `rounded-lg` (16px), background `#12151C`, border `1px solid rgba(255, 255, 255, 0.08)`. Placeholder text in `#94A3B8`. Focus state illuminates border to `#F59E0B` with an outer focus ring blur (`0 0 0 2px rgba(245, 158, 11, 0.2)`).

### Reaction Docks & Floating Bottom Controls
- **Floating Shutter Bar:** Docked at screen bottom with a safe-area margin. Suspended frosted capsule (`backdrop-filter: blur(20px)`, background `rgba(18, 21, 28, 0.85)`) holding camera triggers, collaborative photo roll switchers, and quick emoji burst reactions.