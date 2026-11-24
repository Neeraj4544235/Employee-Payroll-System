@echo off
REM Windows: compiles and runs the Employee Payroll System
cd /d "%~dp0"
set JAR=lib\sqlite-jdbc-3.36.0.3.jar
if not exist lib mkdir lib
if not exist out mkdir out

if not exist %JAR% (
  echo Downloading SQLite JDBC driver...
  curl -L -o %JAR% https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar
  if errorlevel 1 (
    echo Download failed. Get sqlite-jdbc-3.36.0.3.jar manually and put it in the lib folder.
    pause
    exit /b 1
  )
)

echo Compiling...
javac -cp %JAR% -d out src\payroll\*.java
if errorlevel 1 (
  pause
  exit /b 1
)

echo Starting app...
java -cp "out;%JAR%" payroll.Main
pause
