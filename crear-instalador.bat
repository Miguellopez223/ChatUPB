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

REM --- Paso 2.5: Crear carpeta limpia solo con el fat JAR ---
echo Preparando archivos para empaquetado...
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"
mkdir "target\jpackage-input"
copy "%JAR_FILE%" "target\jpackage-input\ChatUPB_V2-1.0-SNAPSHOT.jar" >nul
echo      Listo.
echo.

REM --- Paso 3: Crear el instalador con jpackage ---
echo [2/3] Creando instalador .exe con jpackage...
echo      (Esto puede tardar 1-2 minutos)
echo.

REM Limpiar output anterior si existe
if exist "target\installer" rmdir /s /q "target\installer"

REM Verificar si existe icono
set ICON_OPT=
if exist "src\main\resources\images\logo.ico" set ICON_OPT=--icon src\main\resources\images\logo.ico

jpackage --type exe ^
      --dest target\installer ^
      --input target\jpackage-input ^
      --name "ChatUPB" ^
      --main-class edu.upb.chatupb_v2.Launcher ^
      --main-jar ChatUPB_V2-1.0-SNAPSHOT.jar ^
      --runtime-image "%JAVA_HOME%" ^
      --win-dir-chooser ^
      --win-shortcut ^
      --win-console ^
      --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
      --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Fallo jpackage con --type exe.
    echo   Posible causa: Necesitas WiX Toolset 3.x
    echo   Descarga: https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm
    echo.
    echo Intentando crear version portable sin instalador...

    jpackage --type app-image --name "ChatUPB" --app-version 2.0.0 --vendor "UPB" --input target\jpackage-input --main-jar ChatUPB_V2-1.0-SNAPSHOT.jar --main-class edu.upb.chatupb_v2.Launcher --dest target\installer %ICON_OPT% --java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" --java-options "-Dfile.encoding=UTF-8"

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

REM Limpiar carpeta temporal
rmdir /s /q "target\jpackage-input" 2>nul

echo.
pause
