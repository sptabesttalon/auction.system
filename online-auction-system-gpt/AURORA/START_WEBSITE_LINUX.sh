#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo 'Open http://127.0.0.1:8080 in your browser.'
exec java --add-modules jdk.httpserver -jar dist/AURORA-server.jar
