#!/bin/bash
#
# Script Name: backup.sh
# Description: Performs daily backups of the /home directory.
# Usage: ./backup.sh [destination_directory]
# Author: Jane Doe
# Created: 2025-04-14
# Version: 1.0
# License: MIT
#
# Notes:
# - Requires rsync to be installed.
# - Ensure the destination directory has sufficient space.


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




Check the default ticket cache
bash
Copy
Edit
klist
This will show the currently active Kerberos ticket for your session, including:

The principal (user) name
Ticket expiration time
The Key Distribution Center (KDC) that issued the ticket
2. List tickets from a specific cache
If you suspect that different users have separate ticket caches, you can specify the cache file explicitly:

bash
Copy
Edit
klist -c /tmp/krb5cc_user2
or if using a different type of cache (e.g., in-memory for sssd):

bash
Copy
Edit
klist -c DIR:/run/user/$(id -u)/krb5cc
3. Check all available caches (for modern systems using collection caches)
bash
Copy
Edit
klist -l
This will list all ticket caches available to your user.

4. Verify ticket ownership
If you’re logged in as user1 but want to confirm if user2's ticket is being used, run:

bash
Copy
Edit
klist | grep Principal
This will show which principal (user) has an active ticket.

5. Show ticket details (verbose mode)
To get more details, such as the encryption type:

bash
Copy
Edit
klist -e
6. Check ticket expiration
If you want to know when a ticket will expire:

bash
Copy
Edit
klist -A





How to Manage Multiple Kerberos Ticket Caches
If you need to maintain multiple tickets for different accounts, you can use cache switching:

1️⃣ Use KRB5CCNAME to store multiple tickets
You can assign different ticket caches for different accounts like this:

bash
Copy
Edit
# Obtain a ticket for user1
export KRB5CCNAME=/tmp/krb5cc_user1
kinit user1@REALM

# Obtain a ticket for user2 without overwriting user1's cache
export KRB5CCNAME=/tmp/krb5cc_user2
kinit -k -t /path/to/keytab user2@REALM
Whenever you need to switch between accounts, set the cache name:

bash
Copy
Edit
export KRB5CCNAME=/tmp/krb5cc_user1  # Switch back to user1
klist  # Verify the ticket in use
2️⃣ Using kinit -c to specify the cache per command
Instead of setting an environment variable, you can specify the cache directly:

bash
Copy
Edit
kinit -c /tmp/krb5cc_user1 user1@REALM
kinit -c /tmp/krb5cc_user2 -k -t /path/to/keytab user2@REALM
3️⃣ List and use different ticket caches
Check all stored ticket caches:

bash
Copy
Edit
klist -l
Switch to a specific cache:

bash
Copy
Edit
export KRB5CCNAME=KEYRING:persistent:$(id -u)
4️⃣ Automate switching with scripts
You can create a script to switch accounts:

bash
Copy
Edit
#!/bin/bash
export KRB5CCNAME="/tmp/krb5cc_$1"
klist
Save it as switch_kerberos.sh, then run:

bash
Copy
Edit
source switch_kerberos.sh user1
🚀 Key Takeaways
By default, only one ticket is active per session.
Using KRB5CCNAME or -c allows storing multiple tickets.
Always check active tickets with klist

Scenario 1: Default (Per-Session Ticket Cache)
By default, each SSH session has its own separate Kerberos ticket cache.

If you refresh the ticket in one session (kinit), it does not affect the other session.
Running klist in the second session will still show the old ticket until it expires.
👉 Result: The other session is not affected unless a shared cache is used.

🔍 Scenario 2: Shared Cache (KEYRING or FILE)
If both SSH sessions use the same ticket cache file (e.g., /tmp/krb5cc_user1), then:

Running kinit in one session refreshes the ticket in the shared cache.
The second session will immediately see the updated ticket when running klist.
Check if your system is using a shared cache:

bash
Copy
Edit
klist -l
If both sessions point to the same cache, then they share tickets.

👉 Result: If the cache is shared, all sessions see the updated ticket.

🔍 Scenario 3: Persistent Ticket Cache (KEYRING:persistent or DIR:)
On modern Linux systems, Kerberos may use a persistent ticket cache (e.g., KEYRING:persistent:UID or DIR:/run/user/UID/krb5cc).

If both SSH sessions use this persistent cache, they share the same ticket.
Refreshing (kinit) in one session immediately updates the ticket for all sessions.
Check your current cache type:

bash
Copy
Edit
echo $KRB5CCNAME
If it shows something like KEYRING:persistent:1000 or DIR:/run/user/1000/krb5cc, the cache is shared.

👉 Result: If using a persistent cache, all sessions get the refreshed ticket.

🚀 How to Ensure Shared or Separate Caches?
If you want all SSH sessions to share tickets, explicitly set:
bash
Copy
Edit
export KRB5CCNAME=KEYRING:persistent:$(id -u)
If you want separate tickets per session, use a unique cache per session:
bash
Copy
Edit
export KRB5CCNAME=/tmp/krb5cc_session_$$
kinit user@REALM
Would you like to test which setup your system is using?
