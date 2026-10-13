@echo off
title LifeOS AI Web Dashboard
cd /d "%~dp0"
echo Starting LifeOS AI Web Dashboard...
py server.py
if errorlevel 1 (
    python server.py
)
pause
