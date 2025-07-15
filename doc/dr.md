dr flow:
access the dr machine.
get the script.
run the script file.
start the Jenkins service with command:
systemctl --user start jenkins
check the Jenkin service status with
systemctl --user status jenkins
go to the infoblox with Cyberark
set the CNAME to the lb of DR 

open the Jenkins portal
bring the dr agents back one by one


New-Item -Path 'c:\works\' -Name 'minikube' -ItemType Directory -Force
$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -OutFile 'c:\works\minikube\minikube.exe' -Uri 'https://github.com/kubernetes/minikube/releases/latest/download/minikube-windows-amd64.exe' -UseBasicParsing

$oldPath = [Environment]::GetEnvironmentVariable('Path', [EnvironmentVariableTarget]::Machine)
if ($oldPath.Split(';') -inotcontains 'C:\works\minikube'){
  [Environment]::SetEnvironmentVariable('Path', $('{0};C:\works\minikube' -f $oldPath), [EnvironmentVariableTarget]::Machine)
}


export http_proxy="http://172.27.208.1:7890"
export https_proxy="http://172.27.208.1:7890"
export no_proxy="localhost,172.27.208.1,10.96.0.0/12,192.168.59.0/24,192.168.49.0/24,192.168.39.0/24,172.27.212.108/24"

set HTTP_PROXY="http://172.27.208.1:7890"
set HTTPS_PROXY="http://172.27.208.1:7890"
set NO_PROXY=localhost,127.0.0.1,10.96.0.0/12,192.168.59.0/24,192.168.49.0/24,192.168.39.0/24



$Env:HTTP_PROXY="http://127.0.0.1:7890"
$Env:HTTPS_PROXY="http://127.0.0.1:7890"
$Env:NO_PROXY="localhost,127.0.0.1,10.96.0.0/12,192.168.59.0/24,192.168.49.0/24,192.168.39.0/24"



