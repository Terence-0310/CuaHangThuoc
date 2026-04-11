@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title Setup nhanh moi truong Java Maven Docker

REM ============================================================
REM setup_env.cmd
REM - An toan: chi kiem tra/cai dat cong cu can thiet, khong xoa du lieu
REM - Chay duoc bang CMD thuong hoac Run as Administrator
REM - Uu tien winget; Maven co fallback tai zip chinh thuc
REM ============================================================

set "SCRIPT_DIR=%~dp0"
set "MAVEN_BASE=C:\apache-maven"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_FOLDER=apache-maven-%MAVEN_VERSION%"
set "MAVEN_URL=https://downloads.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/%MAVEN_FOLDER%-bin.zip"
set "MAVEN_ZIP=%TEMP%\%MAVEN_FOLDER%-bin.zip"
set "MAVEN_EXTRACT_TMP=%TEMP%\maven_extract_%RANDOM%"
set "DOCKER_INSTALLED_NOW=0"

call :log_info "Bat dau setup moi truong Java Maven Docker..."
echo.

REM ============================================================
REM 1) Kiem tra winget co ton tai hay khong
REM ============================================================
where winget >nul 2>&1
if errorlevel 1 (
    call :log_error "Khong tim thay winget."
    call :log_info "Huong dan cai winget:"
    call :log_info "  1) Mo Microsoft Store"
    call :log_info "  2) Cai hoac cap nhat 'App Installer' (Microsoft Corporation)"
    call :log_info "  3) Dong CMD hien tai, mo lai CMD, chay lai setup_env.cmd"
    goto :finish
) else (
    call :log_ok "Da tim thay winget."
)

echo.
REM ============================================================
REM 2) Kiem tra java -version (CHI BAO CAO TRANG THAI, KHONG CAI JAVA)
REM ============================================================
call :log_info "Kiem tra Java (chi in trang thai, khong cai Java)..."
java -version >nul 2>&1
if errorlevel 1 (
    call :log_error "Java hien khong chay duoc tu CMD (khong cai dat trong script theo yeu cau)."
    call :log_info "Vui long tu kiem tra JAVA_HOME/PATH neu can."
) else (
    call :log_ok "Java san sang. Thong tin hien tai:"
    java -version
)

echo.
REM ============================================================
REM 3) Kiem tra mvn -v, neu chua co thi cai Maven
REM ============================================================
call :log_info "Kiem tra Maven..."
call :check_maven
if "!MAVEN_OK!"=="1" (
    call :log_ok "Maven da san sang."
) else (
    call :log_info "Chua co Maven. Thu cai bang winget..."
    call :install_maven_winget
    call :check_maven

    if "!MAVEN_OK!"=="1" (
        call :log_ok "Da cai Maven bang winget thanh cong."
    ) else (
        call :log_error "Cai Maven bang winget that bai. Thu fallback zip chinh thuc..."
        call :install_maven_fallback_zip
        call :check_maven
        if "!MAVEN_OK!"=="1" (
            call :log_ok "Da cai Maven bang fallback zip thanh cong."
        ) else (
            call :log_error "Khong the cai Maven tu ca winget va fallback zip."
        )
    )
)

echo.
REM ============================================================
REM 4) Kiem tra docker -v, neu chua co thi cai Docker Desktop
REM ============================================================
call :log_info "Kiem tra Docker..."
docker -v >nul 2>&1
if errorlevel 1 (
    call :log_info "Chua co Docker. Thu cai Docker Desktop bang winget..."
    call :install_docker_winget
    docker -v >nul 2>&1
    if errorlevel 1 (
        call :log_error "Docker van chua san sang trong CMD hien tai."
    ) else (
        set "DOCKER_INSTALLED_NOW=1"
        call :log_ok "Da cai Docker Desktop thanh cong."
    )
) else (
    call :log_ok "Docker da san sang."
)

echo.
REM ============================================================
REM 5) Refresh environment neu co the, sau do check lai
REM ============================================================
call :log_info "Dang refresh environment cho phien CMD hien tai..."
call :refresh_env
call :log_ok "Da refresh environment (muc co the)."

echo.
call :log_info "Kiem tra lai sau cai dat:"
echo -------------------- java -version --------------------
java -version
if errorlevel 1 call :log_error "java -version loi."
echo ----------------------- mvn -v ------------------------
mvn -v
if errorlevel 1 call :log_error "mvn -v loi."
echo --------------------- docker -v -----------------------
docker -v
if errorlevel 1 call :log_error "docker -v loi."

if "%DOCKER_INSTALLED_NOW%"=="1" (
    echo.
    call :log_info "Docker Desktop vua duoc cai dat."
    call :log_info "Can mo Docker Desktop va cho den khi Docker running."
    call :log_info "Neu Docker chua chay duoc, co the can restart may."
)

goto :finish

:check_maven
set "MAVEN_OK=0"
mvn -v >nul 2>&1
if not errorlevel 1 set "MAVEN_OK=1"
exit /b 0

:install_maven_winget
REM Thu cac package ID pho bien cua Maven
winget install -e --id Apache.Maven --source winget --accept-package-agreements --accept-source-agreements --silent
if not errorlevel 1 exit /b 0

winget install -e --id Apache.Maven.3 --source winget --accept-package-agreements --accept-source-agreements --silent
if not errorlevel 1 exit /b 0

exit /b 1

:install_docker_winget
winget install -e --id Docker.DockerDesktop --source winget --accept-package-agreements --accept-source-agreements --silent
if not errorlevel 1 (
    set "DOCKER_INSTALLED_NOW=1"
    exit /b 0
)
exit /b 1

:install_maven_fallback_zip
call :log_info "Tai Maven zip chinh thuc: %MAVEN_URL%"

where curl >nul 2>&1
if errorlevel 1 (
    call :log_error "Khong tim thay curl de tai Maven zip."
    exit /b 1
)

if exist "%MAVEN_ZIP%" del /f /q "%MAVEN_ZIP%" >nul 2>&1
curl -L "%MAVEN_URL%" -o "%MAVEN_ZIP%"
if errorlevel 1 (
    call :log_error "Tai Maven zip that bai."
    exit /b 1
)

if not exist "%MAVEN_BASE%" (
    mkdir "%MAVEN_BASE%" >nul 2>&1
    if errorlevel 1 (
        call :log_error "Khong tao duoc thu muc %MAVEN_BASE%."
        exit /b 1
    )
)

if exist "%MAVEN_EXTRACT_TMP%" rmdir /s /q "%MAVEN_EXTRACT_TMP%" >nul 2>&1
mkdir "%MAVEN_EXTRACT_TMP%" >nul 2>&1

REM Thu giai nen bang tar truoc
tar -xf "%MAVEN_ZIP%" -C "%MAVEN_EXTRACT_TMP%" >nul 2>&1
if errorlevel 1 (
    REM fallback PowerShell Expand-Archive (chi dung khi can)
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_EXTRACT_TMP%' -Force" >nul 2>&1
    if errorlevel 1 (
        call :log_error "Khong giai nen duoc Maven zip."
        exit /b 1
    )
)

if not exist "%MAVEN_EXTRACT_TMP%\%MAVEN_FOLDER%" (
    call :log_error "Khong tim thay thu muc Maven sau khi giai nen."
    exit /b 1
)

if exist "%MAVEN_BASE%\%MAVEN_FOLDER%" rmdir /s /q "%MAVEN_BASE%\%MAVEN_FOLDER%" >nul 2>&1
xcopy "%MAVEN_EXTRACT_TMP%\%MAVEN_FOLDER%" "%MAVEN_BASE%\%MAVEN_FOLDER%\" /E /I /H /Y >nul
if errorlevel 1 (
    call :log_error "Khong copy duoc Maven vao %MAVEN_BASE%."
    exit /b 1
)

set "MAVEN_HOME=%MAVEN_BASE%\%MAVEN_FOLDER%"
setx MAVEN_HOME "%MAVEN_HOME%" >nul

REM Cap nhat PATH nguoi dung, tranh lap lai
echo %PATH% | find /I "%MAVEN_HOME%\bin" >nul
if errorlevel 1 (
    setx PATH "%PATH%;%MAVEN_HOME%\bin" >nul
)

call :log_ok "Da cai dat fallback Maven vao %MAVEN_HOME%"
exit /b 0

:refresh_env
REM Nap lai PATH tu Registry cho CMD hien tai (User + Machine)
set "PATH_MACHINE="
set "PATH_USER="

for /f "tokens=2,*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path 2^>nul ^| find /I "Path"') do set "PATH_MACHINE=%%B"
for /f "tokens=2,*" %%A in ('reg query "HKCU\Environment" /v Path 2^>nul ^| find /I "Path"') do set "PATH_USER=%%B"

if defined PATH_MACHINE (
    if defined PATH_USER (
        set "PATH=%PATH_MACHINE%;%PATH_USER%"
    ) else (
        set "PATH=%PATH_MACHINE%"
    )
)
exit /b 0

:log_ok
echo [OK] %~1
exit /b 0

:log_info
echo [INFO] %~1
exit /b 0

:log_error
echo [ERROR] %~1
exit /b 0

:finish
echo.
call :log_info "Hoan tat script setup_env.cmd"
pause
endlocal
