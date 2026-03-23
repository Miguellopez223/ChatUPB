@echo off
echo ============================================
echo   ChatUPB v2 - Creador de Instalador
echo ============================================
echo.

REM --- Paso 1: Compilar y crear el Fat JAR ---
echo [1/3] Compilando proyecto y creando JAR...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Fallo la compilacion. Revisa los errores arriba.
    pause
    exit /b 1
)
echo      JAR creado exitosamente.
echo.

REM --- Paso 2: Verificar que el JAR existe ---
set JAR_FILE=target\ChatUPB_V2-1.0-SNAPSHOT.jar
if not exist "%JAR_FILE%" (
    echo ERROR: No se encontro el archivo %JAR_FILE%
    pause
    exit /b 1
)

REM --- Paso 3: Crear el instalador con jpackage ---
echo [2/3] Creando instalador .exe con jpackage...
echo      (Esto puede tardar 1-2 minutos)
echo.

REM Limpiar output anterior si existe
if exist "target\installer" rmdir /s /q "target\installer"

jpackage ^
    --type exe ^
    --name "ChatUPB" ^
    --app-version 2.0.0 ^
    --vendor "UPB" ^
    --description "Chat P2P - Universidad Privada Boliviana" ^
    --input target ^
    --main-jar ChatUPB_V2-1.0-SNAPSHOT.jar ^
    --main-class edu.upb.chatupb_v2.Launcher ^
    --dest target\installer ^
    --win-dir-chooser ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "ChatUPB" ^
    --icon src\main\resources\images\logo.ico ^
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Fallo jpackage. Posibles causas:
    echo   - Necesitas instalar WiX Toolset 3.x: https://wixtoolset.org/releases/
    echo   - O usa --type app-image en vez de --type exe para generar carpeta portable
    echo.
    echo Intentando crear version portable (sin instalador)...

    jpackage ^
        --type app-image ^
        --name "ChatUPB" ^
        --app-version 2.0.0 ^
        --vendor "UPB" ^
        --input target ^
        --main-jar ChatUPB_V2-1.0-SNAPSHOT.jar ^
        --main-class edu.upb.chatupb_v2.Launcher ^
        --dest target\installer ^
        --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
        --java-options "-Dfile.encoding=UTF-8"

    if %ERRORLEVEL% neq 0 (
        echo ERROR: Tambien fallo app-image. Revisa que tengas JDK 21+ instalado.
        pause
        exit /b 1
    )
    echo.
    echo [3/3] Version PORTABLE creada en: target\installer\ChatUPB\
    echo      Puedes comprimir esa carpeta en un .zip y distribuirla.
    echo      El usuario ejecuta: ChatUPB\ChatUPB.exe
) else (
    echo.
    echo [3/3] INSTALADOR creado exitosamente!
    echo.
    echo ============================================
    echo   Archivo: target\installer\ChatUPB-2.0.0.exe
    echo ============================================
    echo.
    echo   El usuario solo hace doble click en el .exe,
    echo   se instala como cualquier programa de Windows.
    echo   NO necesita tener Java instalado.
)

echo.
pause
