@echo off
set "JAVA_HOME=C:\Users\tikhi\.jdks\jdk-21.0.13+9"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ========================================================
echo  Starting RetroChat Minecraft 1.21.4 Client...
echo ========================================================
call gradlew.bat runClient
pause
