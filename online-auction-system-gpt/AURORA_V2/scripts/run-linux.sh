#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -pl server -am package -DskipTests
mvn -pl server exec:java
