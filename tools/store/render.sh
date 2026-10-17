#!/usr/bin/env bash
# Renders the Play Store phone screenshots and feature graphic with headless Chrome.
#
#   tools/store/render.sh            # from anywhere; CHROME=/path/to/chrome to pick the browser
#
# Input: the emulator captures in tools/store/raw/ (1080×2400), taken by screenshots.sh.
# Font: Google Sans Flex, downloaded into tools/store/fonts/ on the first run (not committed).
# Output: app/src/main/play/listings/en-US/graphics/phone-screenshots/1..6.png and feature-graphic/feature.png.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
tools="$root/tools/store"
play="$root/app/src/main/play/listings"

chrome="${CHROME:-}"
for c in "$(command -v chromium 2>/dev/null || true)" "$(command -v google-chrome 2>/dev/null || true)" \
         "$HOME"/.cache/ms-playwright/chromium_headless_shell-*/chrome-headless-shell-linux64/chrome-headless-shell; do
  [ -z "$chrome" ] && [ -n "$c" ] && [ -x "$c" ] && chrome="$c"
done
[ -n "$chrome" ] || { echo "Chrome not found; set CHROME=/path/to/chrome" >&2; exit 1; }
# Without the font Chrome falls back to another one and the captions lay out differently, so fetch it (SIL Open
# Font License) from Google Fonts' repository, and stop rather than overwrite the committed images if that fails.
font="$tools/fonts/google_sans_flex.ttf"
if [ ! -f "$font" ]; then
  mkdir -p "$(dirname "$font")"
  curl -fsSL -o "$font" "https://raw.githubusercontent.com/google/fonts/main/ofl/googlesansflex/GoogleSansFlex%5BROND,opsz,slnt,wdth,wght%5D.ttf" ||
    { rm -f "$font"; echo "Couldn't download Google Sans Flex into $font" >&2; exit 1; }
fi

# shoot <page and query> <width> <height> <output.png>
shoot() {
  mkdir -p "$(dirname "$4")"
  "$chrome" --headless --disable-gpu --hide-scrollbars --allow-file-access-from-files --force-device-scale-factor=1 \
    --virtual-time-budget=4000 --window-size="$2,$3" --screenshot="$4" "file://$tools/$1" >/dev/null 2>&1
}

shots=(home ai gallery detail form alert editor)
raws=(home form gallery detail form alert editor)
dir="$play/en-US/graphics"
rm -f "$dir"/phone-screenshots/*.png  # no stale shots when the list gets shorter
for i in "${!shots[@]}"; do
  shoot "shot.html?shot=${shots[$i]}&src=raw/${raws[$i]}.png" 1080 1920 "$dir/phone-screenshots/$((i + 1)).png"
done
shoot feature.html 1024 500 "$dir/feature-graphic/feature.png"
echo "rendered $dir"
