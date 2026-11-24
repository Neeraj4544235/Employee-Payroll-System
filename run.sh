#!/usr/bin/env bash
# Mac / Linux: compiles and runs the Employee Payroll System
cd "$(dirname "$0")"
JAR="lib/sqlite-jdbc-3.36.0.3.jar"
mkdir -p lib out

if [ ! -f "$JAR" ]; then
  echo "Downloading SQLite JDBC driver..."
  curl -L -o "$JAR" https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar \
    || { echo "Download failed. Get sqlite-jdbc-3.36.0.3.jar manually and put it in lib/"; exit 1; }
fi

echo "Compiling..."
javac -cp "$JAR" -d out src/payroll/*.java || exit 1

echo "Starting app..."
java -cp "out:$JAR" payroll.Main
