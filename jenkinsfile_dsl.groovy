pipeline {
    agent {
        kubernetes {
            cloud 'my-kubernetes-cloud'
            containerTemplate {
                // ... your container template configuration
            }
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'echo "Building the project"'
                // ... other build steps
            }
        }
    }
    post {
        always {
            // View the container logs
            containerLog 'my-custom-agent'
        }
    }
}



pipeline {
    agent {
        kubernetes {
            cloud 'my-kubernetes-cloud'
            containerTemplate {
                name 'my-custom-agent'
                image 'my-custom-image:latest'
                command 'bash'
                args '-c', 'while true; do echo "Agent is running"; sleep 60; done'
            }
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'echo "Building the project"'
            }
        }
    }
}


pipeline {
    agent {
        kubernetes {
            cloud 'my-kubernetes-cloud'
            containerTemplate {
                name 'my-custom-agent'
                image 'my-custom-image:latest'
                command 'bash'
                args '-c', 'mvn clean package'
                envVars [
                    'MY_VAR': 'my_value'
                ]
                resourceRequests [
                    cpu: '1',
                    memory: '2Gi'
                ]
                resourceLimits [
                    cpu: '2',
                    memory: '4Gi'
                ]
                volumeMounts [
                    name: 'my-volume',
                    mountPath: '/mnt/my-volume'
                ]
                workingDir '/app'
            }
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
    }
}
