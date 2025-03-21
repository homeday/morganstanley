kubectl get pods --all-namespaces -o custom-columns='POD:.metadata.name,CONTAINER:.spec.containers[*].name,CPU_REQUEST:.spec.containers[*].resources.requests.cpu,CPU_LIMIT:.spec.containers[*].resources.limits.cpu,MEMORY_REQUEST:.spec.containers[*].resources.requests.memory,MEMORY_LIMIT:.spec.containers[*].resources.limits.memory'

apiVersion: v1
kind: Pod
metadata:
  name: pod-resources-demo
spec:
  resources:
    requests:
      cpu: "1"      # 1 CPU core for the entire Pod
      memory: "200Mi"  # 200 MiB for the entire Pod
    limits:
      cpu: "2"      # 2 CPU cores for the entire Pod
      memory: "400Mi"  # 400 MiB for the entire Pod
  containers:
  - name: container-1
    image: nginx
  - name: container-2
    image: busybox



$ oc adm top pods -n your-namespace
oc adm top pod <pod-name> -n <namespace>
oc adm top pods -n <namespace>
kubectl top pods -n <namespace>
kubectl top pods



#!/bin/bash

# Define the output CSV file
output_file="pod_usage_metrics.csv"

# Write the CSV header if the file doesn't exist
if [ ! -f "$output_file" ]; then
    echo "Timestamp,Namespace,Pod,CPU (cores),Memory (MiB)" > "$output_file"
fi

# Function to convert memory usage to MiB
convert_memory_to_mib() {
    local memory=$1
    if [[ $memory == *Ki ]]; then
        echo "scale=2; ${memory%Ki}/1024" | bc
    elif [[ $memory == *Mi ]]; then
        echo "${memory%Mi}"
    elif [[ $memory == *Gi ]]; then
        echo "scale=2; ${memory%Gi}*1024" | bc
    else
        echo "0"
    fi
}

# Infinite loop to collect data every 3 seconds
while true; do
    # Get the current timestamp
    timestamp=$(date +"%Y-%m-%d %H:%M:%S")

    # Execute the oc adm top pods command and process the output
    oc adm top pods --all-namespaces 2>&1 | while read -r line; do
        # Check if the line contains the error message
        if [[ "$line" == *"metrics not available yet"* ]]; then
            echo "[$timestamp] Warning: Metrics not available yet. Skipping this iteration."
            continue
        fi

        # Skip the header line
        if [[ "$line" == *"NAMESPACE"* ]]; then
            continue
        fi

        # Extract metrics data
        namespace=$(echo "$line" | awk '{print $1}')
        pod=$(echo "$line" | awk '{print $2}')
        cpu=$(echo "$line" | awk '{print $3}')
        memory=$(echo "$line" | awk '{print $4}')
        memory_mib=$(convert_memory_to_mib "$memory")

        # Append the data to the CSV file
        echo "$timestamp,$namespace,$pod,$cpu,$memory_mib" >> "$output_file"
    done

    # Wait for 3 seconds before the next iteration
    sleep 3
done

