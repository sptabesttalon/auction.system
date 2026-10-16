@echo off
cd /d "%~dp0"
set "AUCTION_DEMO=1"
set "AUCTION_DATA=data\demo-session.dat"
where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Java 21 is required.
  pause
  exit /b 1
)
echo.
echo Demo website: http://127.0.0.1:8080
 echo Accounts: seller_demo / demo12345; bidder_demo / demo12345
 echo To stop: Ctrl+C.
java --add-modules jdk.httpserver -jar dist\AURORA-server.jar
pause
