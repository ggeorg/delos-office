#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

mvn -pl delos-base-app -am clean install
mvn -f delos-base-app/pom.xml javafx:run
