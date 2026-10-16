@echo off
setlocal
cd /d "%~dp0.."
where javac >nul 2>&1
if errorlevel 1 (
  echo [ERROR] JDK 21 is required. Install JDK 21 and add javac to PATH.
  pause
  exit /b 1
)
if not exist ".run\classes" mkdir ".run\classes"
dir /s /b "server\src\main\java\*.java" > ".run\sources.txt"
echo Building Java backend with JDK only...
javac --release 21 --add-modules jdk.httpserver -encoding UTF-8 -d ".run\classes" @".run\sources.txt"
if errorlevel 1 (
  echo [ERROR] Java compilation failed.
  pause
  exit /b 1
)
echo.
echo AURORA is starting. Open http://127.0.0.1:8080 in your browser.
echo Press Ctrl+C to stop the server.
java --add-modules jdk.httpserver -cp ".run\classes;server\src\main\resources" vn.uet.auction.Main
endlocal
