@echo off
echo ================================================
echo    KHOI DONG SERVER PHAN CONG GIAM THI
echo ================================================
echo.

cd /d "%~dp0"

if not exist "lib" (
    echo [LOI] Thu muc lib/ khong ton tai!
    echo Hay chay download_libs.bat truoc.
    pause
    exit /b 1
)

setlocal enabledelayedexpansion
set CLASSPATH=src
for %%f in (lib\*.jar) do (
    set CLASSPATH=!CLASSPATH!;%%f
)

echo [1/2] Bien dich ma nguon...
javac -encoding UTF-8 -cp "%CLASSPATH%" src\Protocol.java src\DatabaseManager.java src\ExcelReader.java src\AssignmentEngine.java src\ExcelWriter.java src\Server.java
if errorlevel 1 (
    echo [LOI] Bien dich that bai!
    pause
    exit /b 1
)
echo      Bien dich thanh cong!
echo.

echo [2/2] Khoi dong Server...
echo ================================================
java -Dfile.encoding=UTF-8 -cp "%CLASSPATH%" Server
echo.
echo [SERVER] Da dung.
pause
