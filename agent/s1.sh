#!/bin/bash
# retrieve_secret.sh

# Vault mTLS authentication and secret retrieval logic
# Replace with actual commands to authenticate and fetch the secret
SECRET=$(vault read -field=secret secret/jenkins/agent)

# Send the secret to the Unix domain socket
echo -n "$SECRET" | socat - UNIX-CONNECT:/tmp/jenkins_socket/agent.sock


Jenkins credentials can be securely managed within Jenkins itself, allowing them to be utilized when jobs run on any type of agent. These credentials are stored in an encrypted form on the Jenkins controller, ensuring secure access during job execution.​

Alternatively, Vault can be employed by users, which is particularly advantageous when jobs run on Bring Your Own Agent (BYOA) setups. In such scenarios, users often have the flexibility to configure access to Vault using methods like Kerberos or mutual TLS (mTLS).
