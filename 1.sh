#!/bin/bash

PEM_FILE=your_concatenated.pem
KEYSTORE=your_keystore.jks
PASSWORD=your_keystore_password

# Count the number of certificates in the PEM file
CERT_COUNT=$(grep -c 'END CERTIFICATE' $PEM_FILE)

# Loop through each certificate and import it
for ((i=0; i<$CERT_COUNT; i++)); do
    ALIAS="cert_$i"
    awk "n==$i { print }; /END CERTIFICATE/ { n++ }" $PEM_FILE | \
    keytool -importcert -alias $ALIAS -keystore $KEYSTORE -storepass $PASSWORD -noprompt
done



#!/bin/bash

# 配置参数
USER="admin"
HOST="jenkins.example.com"
SSH_PORT="22"
JENKINS_PORT="8080"
API_TOKEN="your_api_token"

# 1. 检查SSH连接
echo "测试SSH连接..."
ssh -p $SSH_PORT $USER@$HOST "echo 'SSH连接成功!'" || {
    echo "SSH连接失败，请检查网络或SSH配置"
    exit 1
}

# 2. 检查Jenkins服务状态
echo "检查Jenkins服务状态..."
SERVICE_STATUS=$(ssh -p $SSH_PORT $USER@$HOST "sudo systemctl status jenkins")
if [[ $SERVICE_STATUS == *"active (running)"* ]]; then
    echo "Jenkins服务运行正常"
else
    echo "Jenkins服务未运行:"
    echo "$SERVICE_STATUS"
    exit 2
fi

# 3. 检查HTTP访问
echo "测试Jenkins登录页..."
curl -sSfL http://$HOST:$JENKINS_PORT/login -o /dev/null || {
    echo "无法访问Jenkins页面"
    exit 3
}

# 4. 检查API访问
echo "测试Jenkins API..."
API_RESPONSE=$(curl -u $USER:$API_TOKEN -sS -w "%{http_code}" http://$HOST:$JENKINS_PORT/api/json -o /dev/null)
if [ "$API_RESPONSE" == "200" ]; then
    echo "API访问成功"
else
    echo "API访问失败 (HTTP状态码: $API_RESPONSE)"
    exit 4
fi

echo "所有检查通过！"

#!/bin/bash

# Variables
REMOTE_USER="your_username"
REMOTE_HOST="your_remote_host"
LOCAL_USER="your_local_username"
LOCAL_HOST="your_local_host"
JENKINS_URL="http://your_remote_host:8080"  # Adjust the port if necessary
MAINTENANCE_FLAG="/path/to/maintenance.flag"  # Path to the maintenance flag file on the remote host

# Function to check if the remote host is up
check_host_up() {
  echo "Pinging $REMOTE_HOST to check if it's up..."
  if ping -c 1 -W 1 "$REMOTE_HOST" &> /dev/null; then
    echo "$REMOTE_HOST is reachable."
    return 0
  else
    echo "$REMOTE_HOST is not reachable."
    return 1
  fi
}

# Function to check if the remote host is in maintenance mode
check_maintenance_mode() {
  echo "Checking if $REMOTE_HOST is in maintenance mode..."
  if ssh "${REMOTE_USER}@${REMOTE_HOST}" "[ -f $MAINTENANCE_FLAG ]"; then
    echo "$REMOTE_HOST is in maintenance mode."
    return 0
  else
    echo "$REMOTE_HOST is not in maintenance mode."
    return 1
  fi
}

# Function to check Jenkins service status via SSH and systemctl
check_jenkins_service() {
  echo "Checking Jenkins service status on $REMOTE_HOST..."
  ssh "${REMOTE_USER}@${REMOTE_HOST}" << EOF
    if systemctl is-active --quiet jenkins; then
      echo "Jenkins service is active."
    else
      echo "Jenkins service is not active."
    fi
EOF
}

# Function to perform an HTTP request to check Jenkins responsiveness
check_jenkins_http() {
  echo "Checking Jenkins HTTP response..."
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$JENKINS_URL")

  if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "Jenkins is responsive (HTTP 200 OK)."
  else
    echo "Jenkins is not responsive (HTTP status code: $HTTP_STATUS)."
  fi
}

# Function to start Jenkins service on local node
start_jenkins_on_local() {
  echo "Starting Jenkins service on local node $LOCAL_HOST..."
  sudo systemctl start jenkins
  if systemctl is-active --quiet jenkins; then
    echo "Jenkins service started successfully on local node."
  else
    echo "Failed to start Jenkins service on local node."
  fi
}

# Function to stop Jenkins service on remote node
stop_jenkins_on_remote() {
  echo "Stopping Jenkins service on remote node $REMOTE_HOST..."
  ssh "${REMOTE_USER}@${REMOTE_HOST}" << EOF
    sudo systemctl stop jenkins
    if systemctl is-active --quiet jenkins; then
      echo "Failed to stop Jenkins service on remote node."
    else
      echo "Jenkins service stopped successfully on remote node."
    fi
EOF
}

# Function to check and handle Jenkins service status on both nodes
check_and_handle_jenkins_status() {
  if check_host_up; then
    if check_maintenance_mode; then
      echo "Skipping Jenkins checks since $REMOTE_HOST is in maintenance mode."
    else
      check_jenkins_service
      check_jenkins_http
      if [ "$HTTP_STATUS" -ne 200 ]; then
        stop_jenkins_on_remote
        start_jenkins_on_local
      fi
    fi
  else
    echo "Skipping Jenkins checks since $REMOTE_HOST is down."
    start_jenkins_on_local
  fi
}

# Periodically check the status of the remote host and Jenkins service
while true; do
  check_and_handle_jenkins_status
  sleep 300  # Check every 5 minutes
done