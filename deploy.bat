@echo off
REM GlobalTrade SCM Deployment Script for Windows
REM This script handles deployment to Payara Server

setlocal enabledelayedexpansion

REM Configuration
set PAYARA_HOME=%PAYARA_HOME:="C:\payara"%
set APP_NAME=globaltrade-scm-ear
set CONTEXT_ROOT=/globaltrade-scm
set ADMIN_PORT=%ADMIN_PORT:="4848"%
set SERVER_HOST=%SERVER_HOST:="localhost"%

echo =========================================
echo GlobalTrade SCM Deployment Script
echo =========================================
echo Payara Home: %PAYARA_HOME%
echo Application Name: %APP_NAME%
echo Context Root: %CONTEXT_ROOT%
echo Admin Port: %ADMIN_PORT%
echo Server Host: %SERVER_HOST%
echo =========================================

REM Check if Payara is running
echo Checking Payara Server status...
"%PAYARA_HOME%\bin\asadmin.bat" --host %SERVER_HOST% --port %ADMIN_PORT% list-applications >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Payara Server is not running or not accessible
    echo Start Payara Server: "%PAYARA_HOME%\bin\asadmin.bat" start-domain
    exit /b 1
)

echo Payara Server is running

REM Build the application
echo Building application...
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Maven build failed
    exit /b 1
)

echo Build successful

REM Stop existing deployment if exists
echo Checking for existing deployment...
"%PAYARA_HOME%\bin\asadmin.bat" --host %SERVER_HOST% --port %ADMIN_PORT% list-applications | findstr "%APP_NAME%" >nul
if %errorlevel% equ 0 (
    echo Undeploying existing application: %APP_NAME%
    "%PAYARA_HOME%\bin\asadmin.bat" --host %SERVER_HOST% --port %ADMIN_PORT% undeploy %APP_NAME%
    if %errorlevel% neq 0 (
        echo WARNING: Undeploy failed, continuing with deployment
    )
) else (
    echo No existing deployment found
)

REM Deploy new version
echo Deploying new version...
"%PAYARA_HOME%\bin\asadmin.bat" --host %SERVER_HOST% --port %ADMIN_PORT% deploy --contextroot %CONTEXT_ROOT% --force=true globaltrade-scm-ear\target\%APP_NAME%.ear

if %errorlevel% neq 0 (
    echo ERROR: Deployment failed
    exit /b 1
)

echo Deployment successful

REM Verify deployment
echo Verifying deployment...
"%PAYARA_HOME%\bin\asadmin.bat" --host %SERVER_HOST% --port %ADMIN_PORT% list-applications

REM Test datasource connection
echo Testing datasource connection...
"%PAYARA_HOME%\bin\asadmin.bat" --host %SERVER_HOST% --port %ADMIN_PORT% ping-connection-pool GlobalTradePool

if %errorlevel% neq 0 (
    echo WARNING: Datasource connection test failed
    echo Verify datasource configuration in Payara admin console
) else (
    echo Datasource connection successful
)

echo =========================================
echo Deployment completed successfully!
echo Application URL: http://%SERVER_HOST%:8080%CONTEXT_ROOT%
echo =========================================

endlocal