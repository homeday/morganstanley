#!/bin/bash

# Configuration
SERVICE_NAME="jenkins-agent"      # Replace with your actual service name
WORKSPACE_DIR="/var/lib/jenkins/workspace"  # Replace with the correct path
LOG_FILE="/var/log/jenkins_workspace_cleanup.log"

# Ensure log file exists
touch "$LOG_FILE"
chmod 644 "$LOG_FILE"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') : $1" | tee -a "$LOG_FILE"
}

# Step 1: Sleep for a random duration (60 to 600 seconds)
SLEEP_DURATION=$(( RANDOM % 541 + 60 ))
log "Sleeping for $SLEEP_DURATION seconds..."
sleep $SLEEP_DURATION

# Step 2: Stop the Jenkins agent service
log "Stopping $SERVICE_NAME service..."
sudo systemctl stop "$SERVICE_NAME"
if [ $? -ne 0 ]; then
    log "ERROR: Failed to stop $SERVICE_NAME. Aborting."
    exit 1
fi

# Step 3: Remove the workspace directory
if [ -d "$WORKSPACE_DIR" ]; then
    log "Removing workspace directory: $WORKSPACE_DIR"
    sudo rm -rf "$WORKSPACE_DIR"
    log "Workspace directory removed."
else
    log "Workspace directory does not exist: $WORKSPACE_DIR"
fi

# Step 4: Start the Jenkins agent service
log "Starting $SERVICE_NAME service..."
sudo systemctl start "$SERVICE_NAME"
if [ $? -ne 0 ]; then
    log "ERROR: Failed to start $SERVICE_NAME. Please check manually."
    exit 1
fi

log "Workspace cleanup completed successfully."
