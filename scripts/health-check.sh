#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR_PATH="${1:-${ROOT_DIR}/target/GestionSalles-1.0-SNAPSHOT-shaded.jar}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Jar not found at ${JAR_PATH}. Build first with: mvn -DskipTests package" >&2
  exit 1
fi

exec java -cp "${JAR_PATH}" com.gestion.salles.utils.HealthCheck
