@echo off
chcp 65001 >nul
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > sources.txt
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
set ERR=%ERRORLEVEL%
del sources.txt
if %ERR% neq 0 (
    echo BUILD FAILED
    exit /b %ERR%
)
echo BUILD OK
