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
