#!/usr/bin/env bash
set -euo pipefail

JAR_PATH="${1:-GestionSalles-1.0-SNAPSHOT-shaded.jar}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Health-check jar not found: ${JAR_PATH}" >&2
  exit 1
fi

exec java -cp "${JAR_PATH}" com.gestion.salles.utils.HealthCheck
