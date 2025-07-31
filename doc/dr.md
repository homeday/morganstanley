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



❓ FNQ: Why are all related Pull Request jobs triggered when the target branch (e.g., main) is updated?
Context:
When using the "Merging the pull request with the current target branch revision" strategy in the Bitbucket Branch Source Plugin for Jenkins multibranch pipelines, a merge into the target branch (like main) can cause Jenkins to re-evaluate and trigger all open pull requests targeting that branch.

This happens because Jenkins attempts to re-calculate the merged PR state (PR + main) to reflect the latest base branch changes, ensuring PR builds are up to date with the base.

💡 Recommendation:
If this behavior is not desired (i.e., you do not want PR builds to be triggered after each main update), you can:

❌ Disable the Bitbucket webhook for push events to the target branch, and

❌ Disable "Scan Multibranch Pipeline Triggers" (or "Periodically scan multibranch pipeline" if enabled).

However, this also means:

🚫 You will no longer receive automatic detection of new branches or PRs.

🚫 Jenkins won't update the build status for new PRs or detect branch deletions unless manually triggered or scanned.

✅ Alternative (if strict control is needed):
Use the "The current pull request revision" strategy instead of merge simulation.

Or use conditional logic in the pipeline to skip unnecessary builds based on cause (e.g., check if triggered by a base branch update).

❓ FNQ: Why is the console output of some tools (e.g., pip, gradle) unreadable or missing colors/special formatting in Jenkins?
Context:
Some build tools (such as pip, gradle, npm, etc.) produce ANSI-colored or rich-formatted output in the terminal to improve readability.
However, Jenkins' default console does not render these ANSI escape sequences correctly, resulting in cluttered or difficult-to-read logs.

✅ Recommendation:
To properly display colored or styled output in the Jenkins log:

Install the AnsiColor Plugin.

Wrap the sh or bat steps inside an ansiColor block in your Jenkins pipeline.

groovy
Copy
Edit
pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ansiColor('xterm') {
          sh 'gradle build'
        }
      }
    }
  }
}
📝 Notes:
The color map (e.g., 'xterm', 'vga', etc.) can be chosen based on the terminal color codes your tool emits.

This only affects console display—there’s no impact on build results or artifacts.

Make sure your terminal-based tool does not auto-disable ANSI output when not detecting a TTY (some tools do this).