@echo off
title ChatUPB v2
java --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar ChatUPB_V2-1.0-SNAPSHOT.jar
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: No se pudo iniciar ChatUPB.
    echo Asegurate de tener Java 21 o superior instalado.
    echo Descarga: https://www.oracle.com/java/technologies/downloads/
    pause
)
