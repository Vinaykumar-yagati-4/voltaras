# VOLTARAS Frontend — Asset Sources & Licenses

Every bitmap shipped in `frontend/src/assets/` is documented here with its source,
license, and any optimization performed.

---

## `voltaras-transmission-grid-hero.webp`

| Field | Value |
|---|---|
| Purpose | Full-width landing-page hero background (electricity transmission infrastructure) |
| File | `src/assets/voltaras-transmission-grid-hero.webp` |
| Source | Wikimedia Commons — [File:Electric transmission power tower.jpg](https://commons.wikimedia.org/wiki/File:Electric_transmission_power_tower.jpg) |
| Original URL | `https://upload.wikimedia.org/wikipedia/commons/b/b3/Electric_transmission_power_tower.jpg` |
| Author | Foto3821 (Wikimedia Commons user) |
| License | [CC0 1.0 Universal Public Domain Dedication](https://creativecommons.org/publicdomain/zero/1.0/) — no attribution required |
| Subject | Four high-voltage electricity transmission towers |
| Original dimensions | 4272 × 2848 px (aspect 3:2), 4.76 MB JPEG |
| Downloaded | 13 August 2026 (HTTP 200, byte-verified 4,992,621 bytes) |
| Optimization | Resized to 1920 × 1280 px (max width 1920, Lanczos), re-encoded to WebP at quality 82 (method 6); EXIF/ICC/XMP metadata stripped |
| Optimized size | 1920 × 1280 px, ~331 KB |

**Usage note:** the image is bundled at build time via `import` (no remote URL at
runtime). The landing hero applies a solid navy translucent overlay
(`bg-navy-950/70`) over it for text readability while keeping the towers visible.

No other external bitmaps are used by the frontend.
