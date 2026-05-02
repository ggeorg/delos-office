#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

mvn -pl delos-calc-app -am clean install
mvn -f delos-calc-app/pom.xml javafx:run
