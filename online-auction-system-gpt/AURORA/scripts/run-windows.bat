@echo off
cd /d "%~dp0.."
echo Starting AURORA Auction at http://127.0.0.1:8080
mvn -pl server -am package -DskipTests
if errorlevel 1 exit /b 1
mvn -pl server exec:java
