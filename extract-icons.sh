#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ICON_ICO="${ROOT_DIR}/src/main/resources/icons/app_icon.ico"
FALLBACK_PNG="${ROOT_DIR}/src/main/resources/icons/University_of_Laghouat_logo_64x64.png"
OUT_DIR="${ROOT_DIR}/target/icons"
OUT_PNG="${OUT_DIR}/app_icon.png"

mkdir -p "${OUT_DIR}"

if command -v convert >/dev/null 2>&1; then
  # Extract PNGs from the ICO, then pick the largest one.
  convert "${ICON_ICO}" "${OUT_DIR}/app_icon-%d.png" || true
  largest="$(ls -1S "${OUT_DIR}"/app_icon-*.png 2>/dev/null | head -n 1 || true)"
  if [[ -n "${largest}" ]]; then
    cp -f "${largest}" "${OUT_PNG}"
    exit 0
  fi
  # Some ImageMagick builds may output a single file without %d expansion.
  if [[ -f "${OUT_PNG}" ]]; then
    exit 0
  fi
fi

if command -v icotool >/dev/null 2>&1; then
  # Extract the largest PNG from the ICO
  icotool -x -o "${OUT_DIR}" "${ICON_ICO}"
  # Pick the largest extracted PNG
  largest="$(ls -1 "${OUT_DIR}"/*.png | sort -V | tail -n 1 || true)"
  if [[ -n "${largest}" ]]; then
    mv -f "${largest}" "${OUT_PNG}"
    exit 0
  fi
fi

# Fallback: use an existing PNG to avoid build failure
if [[ -f "${FALLBACK_PNG}" ]]; then
  cp -f "${FALLBACK_PNG}" "${OUT_PNG}"
  exit 0
fi

echo "Failed to extract app_icon.png (missing tools and fallback)." >&2
exit 1
