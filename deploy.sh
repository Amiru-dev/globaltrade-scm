#!/bin/bash
# GlobalTrade SCM Deployment Script
# This script handles deployment to Payara Server

set -e  # Exit on error

# Configuration
PAYARA_HOME=${PAYARA_HOME:-/opt/payara}
APP_NAME="globaltrade-scm-ear"
CONTEXT_ROOT="/globaltrade-scm"
ADMIN_PORT=${ADMIN_PORT:-4848}
SERVER_HOST=${SERVER_HOST:-localhost}

echo "========================================="
echo "GlobalTrade SCM Deployment Script"
echo "========================================="
echo "Payara Home: $PAYARA_HOME"
echo "Application Name: $APP_NAME"
echo "Context Root: $CONTEXT_ROOT"
echo "Admin Port: $ADMIN_PORT"
echo "Server Host: $SERVER_HOST"
echo "========================================="

# Check if Payara is running
echo "Checking Payara Server status..."
if ! $PAYARA_HOME/bin/asadmin --host $SERVER_HOST --port $ADMIN_PORT list-applications > /dev/null 2>&1; then
    echo "ERROR: Payara Server is not running or not accessible"
    echo "Start Payara Server: $PAYARA_HOME/bin/asadmin start-domain"
    exit 1
fi

echo "Payara Server is running"

# Build the application
echo "Building application..."
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed"
    exit 1
fi

echo "Build successful"

# Stop existing deployment if exists
echo "Checking for existing deployment..."
EXISTING_APP=$($PAYARA_HOME/bin/asadmin --host $SERVER_HOST --port $ADMIN_PORT list-applications | grep "$APP_NAME" || true)

if [ ! -z "$EXISTING_APP" ]; then
    echo "Undeploying existing application: $APP_NAME"
    $PAYARA_HOME/bin/asadmin --host $SERVER_HOST --port $ADMIN_PORT undeploy $APP_NAME
    if [ $? -ne 0 ]; then
        echo "WARNING: Undeploy failed, continuing with deployment"
    fi
else
    echo "No existing deployment found"
fi

# Deploy new version
echo "Deploying new version..."
$PAYARA_HOME/bin/asadmin --host $SERVER_HOST --port $ADMIN_PORT deploy \
    --contextroot $CONTEXT_ROOT \
    --force=true \
    globaltrade-scm-ear/target/$APP_NAME.ear

if [ $? -ne 0 ]; then
    echo "ERROR: Deployment failed"
    exit 1
fi

echo "Deployment successful"

# Verify deployment
echo "Verifying deployment..."
$PAYARA_HOME/bin/asadmin --host $SERVER_HOST --port $ADMIN_PORT list-applications

# Test datasource connection
echo "Testing datasource connection..."
$PAYARA_HOME/bin/asadmin --host $SERVER_HOST --port $ADMIN_PORT ping-connection-pool GlobalTradePool

if [ $? -ne 0 ]; then
    echo "WARNING: Datasource connection test failed"
    echo "Verify datasource configuration in Payara admin console"
else
    echo "Datasource connection successful"
fi

echo "========================================="
echo "Deployment completed successfully!"
echo "Application URL: http://$SERVER_HOST:8080$CONTEXT_ROOT"
echo "========================================="