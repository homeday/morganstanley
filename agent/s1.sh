#!/bin/bash
# retrieve_secret.sh

# Vault mTLS authentication and secret retrieval logic
# Replace with actual commands to authenticate and fetch the secret
SECRET=$(vault read -field=secret secret/jenkins/agent)

# Send the secret to the Unix domain socket
echo -n "$SECRET" | socat - UNIX-CONNECT:/tmp/jenkins_socket/agent.sock
