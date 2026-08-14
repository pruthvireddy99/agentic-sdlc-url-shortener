@echo off
setlocal
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven is not installed. Please install Maven 3.9.11+ or run from a shell with curl and tar available.
exit /b 1
