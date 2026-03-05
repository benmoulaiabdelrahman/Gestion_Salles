#!/bin/bash
set -euo pipefail

SECRET_PATH="${HOME}/.gestion-salles/app.secret"
if [[ -z "${GESTION_SALLES_SECRET:-}" && -f "${SECRET_PATH}" ]]; then
  export GESTION_SALLES_SECRET
  GESTION_SALLES_SECRET="$(head -n 1 "${SECRET_PATH}" | tr -d '\r\n')"
fi

JAVA_OPTS=(
  "-Dsun.awt.X11.appName=Gestion Salles"
  "-Dsun.awt.X11.XWMClass=Gestion Salles"
)

# Force XToolkit only for X11 sessions.
if [[ -n "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
  JAVA_OPTS+=("-Dawt.toolkit=sun.awt.X11.XToolkit")
fi

exec java "${JAVA_OPTS[@]}" -jar GestionSalles-1.0-SNAPSHOT-shaded.jar
