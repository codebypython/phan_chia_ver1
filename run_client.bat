@echo off
echo ================================================
echo    KHOI DONG CLIENT PHAN CONG GIAM THI
echo ================================================
echo.

cd /d "%~dp0"

setlocal enabledelayedexpansion
set CLASSPATH=src
for %%f in (lib\*.jar) do (
    set CLASSPATH=!CLASSPATH!;%%f
)

echo [1/2] Bien dich Client...
javac -encoding UTF-8 -cp "%CLASSPATH%" src\Protocol.java src\Client.java
if errorlevel 1 (
    echo [LOI] Bien dich that bai!
    pause
    exit /b 1
)
echo      Bien dich thanh cong!
echo.

echo [2/2] Khoi dong Client (GUI)...
start /B javaw -Dfile.encoding=UTF-8 -cp "%CLASSPATH%" Client
echo [CLIENT] Da mo giao dien Client.
timeout /t 2 >nul
