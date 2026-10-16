@echo off
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Java 21 is required. Install JDK 21 and add java to PATH.
  pause
  exit /b 1
)
echo.
echo Open http://127.0.0.1:8080 in Chrome or Edge after server starts.
echo To stop server: Ctrl+C.
java --add-modules jdk.httpserver -jar dist\AURORA-server.jar
pause
