@echo off
set JAVA_PATH="C:\Users\User\.jdks\temurin-21.0.11\bin\java.exe"
set JAR_PATH=target\GameDrawAndGuess-1.0-SNAPSHOT.jar

echo Launching with module bypass...
%JAVA_PATH% --module-path %JAR_PATH% --add-modules javafx.controls,javafx.fxml -jar %JAR_PATH%

echo Запуск Draw and Guess через Java 21...
%JAVA_PATH% -jar %JAR_PATH%

if %errorlevel% neq 0 (
    echo Ошибка запуска! Проверь, что путь к Java 21 указан верно.
    pause
)