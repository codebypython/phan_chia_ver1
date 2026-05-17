@echo off
echo ================================================
echo    TAI THU VIEN (Apache POI + PostgreSQL)
echo ================================================
echo.

cd /d "%~dp0"

if not exist "lib" mkdir lib

echo Dang tai cac thu vien can thiet...
echo.

echo [1/9] postgresql-42.7.5.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar' -OutFile 'lib\postgresql-42.7.5.jar'" 2>nul
if exist "lib\postgresql-42.7.5.jar" (echo      OK) else (echo      THAT BAI)

echo [2/9] poi-5.2.5.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/poi/poi/5.2.5/poi-5.2.5.jar' -OutFile 'lib\poi-5.2.5.jar'" 2>nul
if exist "lib\poi-5.2.5.jar" (echo      OK) else (echo      THAT BAI)

echo [3/9] poi-ooxml-5.2.5.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml/5.2.5/poi-ooxml-5.2.5.jar' -OutFile 'lib\poi-ooxml-5.2.5.jar'" 2>nul
if exist "lib\poi-ooxml-5.2.5.jar" (echo      OK) else (echo      THAT BAI)

echo [4/9] poi-ooxml-lite-5.2.5.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml-lite/5.2.5/poi-ooxml-lite-5.2.5.jar' -OutFile 'lib\poi-ooxml-lite-5.2.5.jar'" 2>nul
if exist "lib\poi-ooxml-lite-5.2.5.jar" (echo      OK) else (echo      THAT BAI)

echo [5/9] xmlbeans-5.2.0.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/xmlbeans/xmlbeans/5.2.0/xmlbeans-5.2.0.jar' -OutFile 'lib\xmlbeans-5.2.0.jar'" 2>nul
if exist "lib\xmlbeans-5.2.0.jar" (echo      OK) else (echo      THAT BAI)

echo [6/9] commons-compress-1.26.1.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.26.1/commons-compress-1.26.1.jar' -OutFile 'lib\commons-compress-1.26.1.jar'" 2>nul
if exist "lib\commons-compress-1.26.1.jar" (echo      OK) else (echo      THAT BAI)

echo [7/9] commons-io-2.16.1.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/commons-io/commons-io/2.16.1/commons-io-2.16.1.jar' -OutFile 'lib\commons-io-2.16.1.jar'" 2>nul
if exist "lib\commons-io-2.16.1.jar" (echo      OK) else (echo      THAT BAI)

echo [8/9] commons-collections4-4.4.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar' -OutFile 'lib\commons-collections4-4.4.jar'" 2>nul
if exist "lib\commons-collections4-4.4.jar" (echo      OK) else (echo      THAT BAI)

echo [9/9] log4j-api-2.23.1.jar
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.23.1/log4j-api-2.23.1.jar' -OutFile 'lib\log4j-api-2.23.1.jar'" 2>nul
if exist "lib\log4j-api-2.23.1.jar" (echo      OK) else (echo      THAT BAI)

echo.
echo ================================================
echo Kiem tra thu vien da tai:
echo ================================================
dir /b lib\*.jar
echo.
echo Hoan tat! Ban co the chay run_server.bat va run_client.bat.
pause
