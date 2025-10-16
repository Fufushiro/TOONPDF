#!/usr/bin/env bash
set -euo pipefail

# Generate Android adaptive icon foreground from a source image.
# - Applies recommended safe-area crop (18dp inset: content within 72/108 = 66.7% of canvas)
# - Exports density-specific PNGs into res/drawable-*dpi as ic_app_icon_foreground.png
#
# Usage:
#   scripts/generate_app_icon.sh path/to/source_image.(png|jpg|webp)
#
# Requirements: ImageMagick (convert/identify)

SRC=${1:-}
if [[ -z "${SRC}" ]]; then
  echo "Usage: $0 path/to/source_image" >&2
  exit 1
fi

if ! command -v convert >/dev/null 2>&1; then
  echo "Error: ImageMagick 'convert' not found. Install it (e.g., sudo apt-get install imagemagick)." >&2
  exit 2
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RES_DIR="$ROOT_DIR/app/src/main/res"

# Densities and scale factors
# dp canvas for adaptive icon is 108dp. Safe area is 72dp (18dp inset each side).
# So we create canvas_px = 108 * density and content_px = 72 * density

declare -A FACTORS=( [mdpi]=1 [hdpi]=1.5 [xhdpi]=2 [xxhdpi]=3 [xxxhdpi]=4 )

# Prepare a square, trimmed, transparent-padded version of the source first.
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

# Normalize colorspace and alpha
BASE="$WORKDIR/base.png"
convert "${SRC}" -auto-orient -strip -colorspace sRGB -alpha on "$BASE"

# Trim surrounding uniform background and pad to square (max dimension preserved)
TRIMMED="$WORKDIR/trimmed.png"
convert "$BASE" -trim +repage "$TRIMMED"

read WIDTH HEIGHT < <(identify -format "%w %h" "$TRIMMED")
LONGER=$(( WIDTH>HEIGHT ? WIDTH : HEIGHT ))

SQUARE="$WORKDIR/square.png"
convert "$TRIMMED" -gravity center -background none -extent ${LONGER}x${LONGER} "$SQUARE"

for D in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  SCALE=${FACTORS[$D]}
  # Canvas and safe content sizes in px
  CANVAS=$(python3 - <<PY
s=$SCALE
print(int(round(108*s)))
PY
  )
  CONTENT=$(python3 - <<PY
s=$SCALE
print(int(round(72*s)))
PY
  )

  OUTDIR="$RES_DIR/drawable-$D"
  mkdir -p "$OUTDIR"
  OUT="$OUTDIR/ic_app_icon_foreground.png"

  # Resize artwork to content size and composite onto transparent 108dp canvas
  convert "$SQUARE" -resize ${CONTENT}x${CONTENT} \
    -gravity center -background none -extent ${CANVAS}x${CANVAS} \
    PNG32:"$OUT"
  echo "Wrote $OUT"

done

echo "Done. Review the icons and build the app."

