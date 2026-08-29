# LQD logo — vector rebuild

Rebuilt as true geometry (circles, 45° cuts, one stroke weight) from the
low-resolution raster. Not an auto-trace: every curve is a real arc, every
terminal sits on the grid below. Scales to any size with no soft edges.

## Files
| File | Use |
|---|---|
| `lqd-logo.svg` | Primary. Two-tone, transparent, tight bounding box |
| `lqd-logo-padded.svg` | Same mark with 10% clear space baked in |
| `lqd-logo-reversed.svg` | White + grey, for dark backgrounds |
| `lqd-logo-mono-black.svg` / `-mono-white.svg` | Single-colour |
| `lqd-logo-currentcolor.svg` | Inline SVG that inherits CSS `color` |
| `png/lqd-logo-{512,1024,2048,4096}.png` | Transparent raster exports |
| `png/lqd-icon-1024.png` | Square, padded — app icon / avatar |

## Colours
- Ink `#18181A`  (L, D)
- Grey `#8C9198`  (Q)

## Construction (viewBox `0 0 1187 536`)
- Stroke weight: **86** throughout. Cap height 532 (y 2 → 534).
- **Q** ring: r 268 / r 182 about (542, 268); 2px optical overshoot top and bottom.
- **D** bowl: r 266 / r 180 about (921, 268).
- Every terminal — L foot, both D arms, the Q ring break — is cut on a **45°
  diagonal parallel to the Q tail**. That single rule is the whole system.
- Negative gap between the Q tail and its neighbours: 47 (perpendicular),
  identical on both sides.

## Clear space
Keep a margin of one stroke weight (86 units ≈ 16% of the mark height) clear on
all sides. Minimum legible width: 120px (the ring break closes up below that).
