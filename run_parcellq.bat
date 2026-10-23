@echo off
title ParcellQ - University Parcel Shelter Server
cd /d "%~dp0"
echo ========================================================
echo        Starting ParcellQ Campus Logistics Server...
echo ========================================================
echo.
echo Launching server on http://localhost:8080
echo Press Ctrl+C in this window when you want to stop the server.
echo.

start http://localhost:8080
call mvn exec:java
pause
