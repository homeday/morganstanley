#!/bin/bash
# start_agent.sh

# Read the secret from the Unix domain socket
SECRET=$(socat - UNIX-CONNECT:/tmp/jenkins_socket/agent.sock)

# Start the Jenkins agent with the retrieved secret
# Replace with actual command to start the Jenkins agent
java -jar agent.jar -secret "$SECRET"