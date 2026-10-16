#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .run/classes
find server/src/main/java -name '*.java' > .run/sources.txt
javac --release 21 --add-modules jdk.httpserver -encoding UTF-8 -d .run/classes @.run/sources.txt
echo 'Open http://127.0.0.1:8080 in your browser. Ctrl+C to stop.'
java --add-modules jdk.httpserver -cp '.run/classes:server/src/main/resources' vn.uet.auction.Main
