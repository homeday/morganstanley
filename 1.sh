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