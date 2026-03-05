#!/usr/bin/env bash
set -euo pipefail

APP_ID="gestionsalles"
APP_NAME="Gestion Salles"
APP_WM_CLASS="Gestion Salles"

resolve_app_exec() {
  if command -v "${APP_ID}" >/dev/null 2>&1; then
    command -v "${APP_ID}"
    return 0
  fi
  if command -v "GestionSalles" >/dev/null 2>&1; then
    command -v "GestionSalles"
    return 0
  fi
  return 1
}

APP_EXEC="$(resolve_app_exec || true)"
if [[ -z "${APP_EXEC}" ]]; then
  echo "post-install: unable to resolve app executable." >&2
  exit 0
fi

APP_REAL="$(readlink -f "${APP_EXEC}")"
APP_ROOT="$(cd "$(dirname "${APP_REAL}")/.." && pwd)"

ICON_PATH=""
for candidate in \
  "${APP_ROOT}/lib/app/app_icon.png" \
  "${APP_ROOT}/lib/app/app_icon_512.png" \
  "${APP_ROOT}/lib/app/app_icon_256.png" \
  "${APP_ROOT}/lib/app/app_icon_128.png" \
  "${APP_ROOT}/lib/app_icon.png" \
  "${APP_ROOT}/lib/app_icon_512.png" \
  "${APP_ROOT}/lib/app_icon_256.png" \
  "${APP_ROOT}/lib/app_icon_128.png"
do
  if [[ -f "${candidate}" ]]; then
    ICON_PATH="${candidate}"
    break
  fi
done

DESKTOP_PATH="/usr/share/applications/${APP_ID}.desktop"
cat > "${DESKTOP_PATH}" <<EOF
[Desktop Entry]
Type=Application
Name=${APP_NAME}
Exec=${APP_EXEC}
Icon=${ICON_PATH:-${APP_ID}}
Terminal=false
Categories=Education;
StartupNotify=true
StartupWMClass=${APP_WM_CLASS}
X-GNOME-WMClass=${APP_WM_CLASS}
EOF

exit 0
