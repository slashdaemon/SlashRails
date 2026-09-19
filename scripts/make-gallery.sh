#!/usr/bin/env bash
# Turns the recorded demo takes (versions/1.21.1-fabric/run-demo/demo/*.mp4) into store-gallery
# assets in build/gallery/: animated WebP + GIF per clip, and a labelled side-by-side ride comparison.
#   scripts/make-gallery.sh [path/to/ffmpeg.exe]
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FF="${1:-/c/Users/slash/tools/ffmpeg/bin/ffmpeg.exe}"
IN="$ROOT/versions/1.21.1-fabric/run-demo/demo"
OUT="$ROOT/build/gallery"
mkdir -p "$OUT"
FONT="C\:/Windows/Fonts/segoeuib.ttf"
LIMIT=$((5 * 1024 * 1024 - 65536))  # a little under Modrinth's 5 MiB gallery limit

anim() { # input.mp4 name
  "$FF" -y -loglevel error -i "$1" -vf "fps=24,scale=960:-2:flags=lanczos" \
    -c:v libwebp_anim -quality 75 -compression_level 6 -loop 0 -an "$OUT/$2.webp"
  # Modrinth caps gallery images at 5 MiB; step the GIF down until it fits.
  for fw in 15:800 12:720 10:640 10:560 8:480; do
    "$FF" -y -loglevel error -i "$1" -vf "fps=${fw%%:*},scale=${fw##*:}:-2:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=sierra2_4a" \
      -loop 0 "$OUT/$2.gif"
    if [ "$(stat -c %s "$OUT/$2.gif")" -le "$LIMIT" ]; then echo "  $2.gif: ${fw%%:*} fps, ${fw##*:} px"; break; fi
  done
  [ "$1" -ef "$OUT/$2.mp4" ] || cp "$1" "$OUT/$2.mp4"
}

[ -f "$IN/click.mp4" ] && anim "$IN/click.mp4" click
[ -f "$IN/preview.mp4" ] && anim "$IN/preview.mp4" preview

if [ -f "$IN/ride-vanilla.mp4" ] && [ -f "$IN/ride-smooth.mp4" ]; then
  # Two 960x540 rides side by side on a 1920x1080 canvas, labelled above.
  label="fontfile='$FONT':fontsize=46:fontcolor=white:y=150"
  "$FF" -y -loglevel error -i "$IN/ride-vanilla.mp4" -i "$IN/ride-smooth.mp4" -filter_complex \
    "[0:v]scale=960:540,setpts=PTS-STARTPTS[l];[1:v]scale=960:540,setpts=PTS-STARTPTS[r];\
[l][r]hstack=shortest=1,pad=1920:1080:0:270:color=0x1b1f24,\
drawtext=text='Vanilla':x=480-text_w/2:$label,\
drawtext=text='SlashRails':x=1440-text_w/2:$label" \
    -c:v libx264 -crf 16 -pix_fmt yuv420p -movflags +faststart "$OUT/ride-compare.mp4"
  anim "$OUT/ride-compare.mp4" ride-compare
fi
cp "$IN"/../screenshots/demo-*.png "$OUT/" 2>/dev/null || true
ls -la "$OUT"
for f in "$OUT"/*.gif "$OUT"/*.webp; do
  [ "$(stat -c %s "$f")" -gt "$LIMIT" ] && echo "OVER GALLERY LIMIT: $f"
done
exit 0
