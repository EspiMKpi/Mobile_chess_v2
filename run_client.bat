@echo off
chcp 65001 >nul
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
"%JAVA_HOME%\bin\java" -Dfile.encoding=UTF-8 -cp "bin;lib/*" client.ClientRun
