# Overview
In Jenkins Pipelines, the stash and unstash steps allow you to save and retrieve files between different stages or nodes. This is particularly useful when:

Sharing build artifacts between stages.

Handling node failures by preserving necessary files.

Managing workspaces across different environments.


# stash: Saving Files for Later Use
The stash step archives a set of files from the workspace for later retrieval.

Syntax:

stash name: 'stashName', includes: '**/*', excludes: '', allowEmpty: false


Parameters:

name (String): Unique identifier for the stash.

includes (String, optional): Ant-style pattern to specify files to include. Defaults to all files ('**/*').

excludes (String, optional): Ant-style pattern to specify files to exclude.

allowEmpty (boolean, optional): If set to true, allows stashing even if no files match the pattern. Defaults to false.

Example：
stash name: 'compiledCode', includes: 'build/**/*.class'
This command stashes all .class files under the build directory.


unstash: Retrieving Previously Stashed Files
The unstash step restores files that were previously stashed.

Syntax:

groovy
Copy
Edit
unstash 'stashName'
Example:

groovy
Copy
Edit
unstash 'compiledCode'
This command retrieves the files stashed under the name compiledCode into the current workspace.

Practical Example: Multi-Stage Pipeline
Here's how you might use stash and unstash in a multi-stage pipeline:

groovy
Copy
Edit
pipeline {
    agent none
    stages {
        stage('Build') {
            agent { label 'linux' }
            steps {
                sh 'make build'
                stash name: 'buildArtifacts', includes: 'build/**'
            }
        }
        stage('Test') {
            agent { label 'linux' }
            steps {
                unstash 'buildArtifacts'
                sh 'make test'
            }
        }
        stage('Deploy') {
            agent { label 'linux' }
            steps {
                unstash 'buildArtifacts'
                sh 'make deploy'
            }
        }
    }
}
In this pipeline:

The Build stage compiles the code and stashes the artifacts.

The Test stage retrieves the artifacts for testing.

The Deploy stage retrieves the same artifacts for deployment.

📁 Unstashing into a Specific Directory
By default, unstash restores files into the current working directory. To restore files into a specific subdirectory, use the dir step:

groovy
Copy
Edit
dir('output') {
    unstash 'buildArtifacts'
}
This command restores the stashed files into the output directory within the workspace.

⚠️ Best Practices and Considerations
Relative Paths: Ensure that the includes and excludes patterns are relative to the current working directory.

Unique Stash Names: Use unique names for each stash to avoid conflicts.

Stash Size: Be mindful of the size of the stashed files, as large stashes can impact performance.

Retention: Stashes are typically discarded at the end of a pipeline run. To preserve stashes for longer, consider using the preserveStashes() option in Declarative Pipelines.


Jenkins Pipeline: Using archiveArtifacts
Overview
The archiveArtifacts step in Jenkins Pipelines allows you to archive build artifacts, such as compiled binaries, logs, or other files, making them accessible through the Jenkins web interface. This is particularly useful for:

Preserving build outputs for future reference.

Facilitating artifact downloads for stakeholders.

Enabling integration with external tools or processes.

🛠️ Syntax
Declarative Pipeline:

groovy
Copy
Edit
archiveArtifacts artifacts: 'path/to/artifacts/**/*'
Scripted Pipeline:

groovy
Copy
Edit
archiveArtifacts artifacts: 'path/to/artifacts/**/*'
⚙️ Parameters
artifacts (String): Specifies the files to archive using Ant-style patterns. For example, 'build/libs/**/*.jar'.

allowEmptyArchive (boolean, optional): If set to true, allows the build to continue even if no artifacts are found. Defaults to false.

onlyIfSuccessful (boolean, optional): If set to true, archives artifacts only if the build is successful. Defaults to false.

fingerprint (boolean, optional): If set to true, generates fingerprints for the archived artifacts, enabling tracking across builds. Defaults to false.

caseSensitive (boolean, optional): If set to false, makes the pattern matching case-insensitive. Defaults to true.

defaultExcludes (boolean, optional): If set to false, disables default Ant exclusions. Defaults to true.

excludes (String, optional): Specifies files to exclude from archiving using Ant-style patterns.

followSymlinks (boolean, optional): If set to false, symbolic links are not followed. Defaults to true.
Jenkins
+3
Jenkins
+3
Stack Overflow
+3
Stack Overflow
+1
Jenkins
+1
Jenkins

📂 Examples
Archiving JAR Files:

groovy
Copy
Edit
archiveArtifacts artifacts: 'build/libs/**/*.jar'
Archiving Multiple File Types:

groovy
Copy
Edit
archiveArtifacts artifacts: 'build/libs/**/*.jar, build/libs/**/*.war'
Archiving with Fingerprinting:

groovy
Copy
Edit
archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
Archiving Only on Successful Builds:

groovy
Copy
Edit
archiveArtifacts artifacts: 'build/libs/**/*.jar', onlyIfSuccessful: true
Allowing Empty Archives:

groovy
Copy
Edit
archiveArtifacts artifacts: 'build/libs/**/*.jar', allowEmptyArchive: true
🧪 Practical Example: Declarative Pipeline
groovy
Copy
Edit
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
        }
    }
}
In this example, after the build stage, all JAR files in the build/libs directory are archived, and fingerprints are generated for tracking.

📁 Notes
Workspace Scope: Only files within the workspace can be archived. Ensure that the artifacts you intend to archive are located within the workspace directory.

Retention Policy: Archived artifacts are retained as long as the build record exists. If the build is deleted, its artifacts are also removed.

Storage Location: By default, archived artifacts are stored on the Jenkins controller. For large artifacts or distributed setups, consider using external storage solutions or plugins like Artifact Manager on S3.

Not a Replacement for Artifact Repositories: While archiveArtifacts is useful for basic artifact storage, for more robust artifact management, consider integrating with artifact repositories like Artifactory or Nexus.


 Prerequisites
Install the Copy Artifact Plugin: Ensure that the Copy Artifact Plugin is installed on your Jenkins instance.

Archive Artifacts in the Upstream Job: In the upstream job (e.g., Job-A), use the archiveArtifacts step to archive the desired files:

groovy
Copy
Edit
archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
📥 Copying Artifacts in the Downstream Job
In the downstream job (e.g., Job-B), use the copyArtifacts step to retrieve the archived artifacts:

groovy
Copy
Edit
copyArtifacts(
    projectName: 'Job-A',
    filter: 'build/libs/**/*.jar',
    selector: lastSuccessful()
)
Parameters Explained:

projectName: Name of the upstream job (Job-A).

filter: Ant-style pattern to specify which artifacts to copy.

selector: Specifies which build's artifacts to copy. Common selectors include:

lastSuccessful(): Last successful build.

specific('42'): Specific build number.

upstream(): Build that triggered the current job.
CloudBees
+5
Jenkins Plugins
+5
Jenkins
+5
Stack Overflow
+1
Jenkins
+1

For more selector options and details, refer to the Copy Artifact Plugin documentation.
Jenkins
+4
Jenkins Plugins
+4
okeybukks.hashnode.dev
+4

🧪 Example: Declarative Pipeline
groovy
Copy
Edit
pipeline {
    agent any
    stages {
        stage('Retrieve Artifacts') {
            steps {
                copyArtifacts(
                    projectName: 'Job-A',
                    filter: 'build/libs/**/*.jar',
                    selector: lastSuccessful()
                )
            }
        }
    }
}
📁 Notes
Permissions: Ensure that the downstream job has permission to access the upstream job's artifacts. You may need to configure permissions accordingly.

Fingerprinting: If you enable fingerprinting (fingerprint: true) in the archiveArtifacts step, Jenkins can track artifact usage across jobs.

Alternative Methods: If you prefer not to use the Copy Artifact plugin, you can manually download artifacts using curl or wget by accessing the artifact's URL, provided you have the necessary permissions.

copyArtifacts(
    projectName: 'Job-A',
    selector: specific('42'),
    filter: 'build/libs/**/*.jar'
)



Overview
The SSH Pipeline Steps Plugin facilitates SSH operations in Jenkins Pipelines, allowing you to:
Jenkins Plugins

Execute remote commands (sshCommand)

Transfer files to (sshPut) and from (sshGet) remote machines

Execute scripts on remote hosts (sshScript)

Remove files or directories on remote hosts (sshRemove)
Stack Overflow
+2
Jenkins Plugins
+2
jenkinsci.github.io
+2
Reddit
+4
Jenkins
+4
Jenkins Plugins
+4

This plugin is particularly useful for deployment tasks, remote server management, and integrating with external systems.

🛠️ Plugin Installation
Navigate to Manage Jenkins > Manage Plugins.

Under the Available tab, search for SSH Pipeline Steps.

Select the plugin and click Install without restart.
Jenkins Plugins
Jenkins
+3
CloudBees Docs
+3
Jenkins Plugins
+3

⚙️ Remote Configuration
Define a remote map with the necessary connection details:
Jenkins Plugins
+1
jenkinsci.github.io
+1

groovy
Copy
Edit
def remote = [:]
remote.name = 'remote-server'
remote.host = '192.168.1.100'
remote.user = 'jenkins'
remote.password = 'your_password' // Alternatively, use credentials for better security
remote.allowAnyHosts = true
For enhanced security, it's recommended to use SSH keys stored in Jenkins credentials.

🔐 Using Credentials
To utilize SSH credentials stored in Jenkins:

Add your SSH private key to Jenkins Credentials with a unique ID (e.g., ssh-key-id).

Use the withCredentials block to access the credentials:
jenkinsci.github.io

groovy
Copy
Edit
withCredentials([sshUserPrivateKey(credentialsId: 'ssh-key-id', keyFileVariable: 'identity', usernameVariable: 'user')]) {
    remote.user = user
    remote.identityFile = identity
    // Proceed with SSH operations
}
🧪 Pipeline Examples
1. Execute Remote Command

groovy
Copy
Edit
sshCommand remote: remote, command: 'uptime'
2. Transfer File to Remote Host

groovy
Copy
Edit
sshPut remote: remote, from: 'local/file.txt', into: '/remote/path/'
3. Retrieve File from Remote Host

groovy
Copy
Edit
sshGet remote: remote, from: '/remote/path/file.txt', into: 'local/file.txt', override: true
4. Execute Script on Remote Host

groovy
Copy
Edit
writeFile file: 'script.sh', text: 'echo Hello, World!'
sshScript remote: remote, script: 'script.sh'
5. Remove File on Remote Host

groovy
Copy
Edit
sshRemove remote: remote, path: '/remote/path/file.txt'
📄 Full Declarative Pipeline Example
groovy
Copy
Edit
pipeline {
    agent any
    environment {
        REMOTE_HOST = '192.168.1.100'
    }
    stages {
        stage('SSH Operations') {
            steps {
                script {
                    def remote = [:]
                    remote.name = 'remote-server'
                    remote.host = env.REMOTE_HOST
                    remote.user = 'jenkins'
                    remote.password = 'your_password'
                    remote.allowAnyHosts = true

                    // Execute command
                    sshCommand remote: remote, command: 'uptime'

                    // Transfer file to remote
                    sshPut remote: remote, from: 'local/file.txt', into: '/remote/path/'

                    // Retrieve file from remote
                    sshGet remote: remote, from: '/remote/path/file.txt', into: 'local/file.txt', override: true

                    // Execute script on remote
                    writeFile file: 'script.sh', text: 'echo Hello from script'
                    sshScript remote: remote, script: 'script.sh'

                    // Remove file on remote
                    sshRemove remote: remote, path: '/remote/path/file.txt'
                }
            }
        }
    }
}

 Jenkins Pipeline File Management: stash, unstash, archiveArtifacts, and copyArtifacts Explained
📦 Managing Files in Jenkins Pipelines: A Guide to stash, unstash, archiveArtifacts, and copyArtifacts
🔄 Jenkins Pipeline File Handling: Understanding stash, unstash, archiveArtifacts, and copyArtifacts
