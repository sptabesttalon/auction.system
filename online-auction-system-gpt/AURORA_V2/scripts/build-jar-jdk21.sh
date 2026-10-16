#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .build/classes dist
find server/src/main/java -name '*.java' > .build/java-sources.txt
javac --release 21 --add-modules jdk.httpserver -encoding UTF-8 -d .build/classes @.build/java-sources.txt
cp -r server/src/main/resources/* .build/classes/
printf 'Main-Class: vn.uet.auction.Main\n' > .build/MANIFEST.MF
jar cfm dist/AURORA-server.jar .build/MANIFEST.MF -C .build/classes .
echo 'Created dist/AURORA-server.jar'
