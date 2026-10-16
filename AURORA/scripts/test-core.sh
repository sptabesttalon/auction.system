#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .local-test
find server/src/main/java -name '*.java' > .local-test/sources.txt
echo scripts/SmokeTest.java >> .local-test/sources.txt
javac --add-modules jdk.httpserver -encoding UTF-8 -d .local-test @.local-test/sources.txt
java --add-modules jdk.httpserver -cp .local-test SmokeTest
