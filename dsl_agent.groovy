@Library('your-library-name') _

pipeline {
    agent any
    stages {
        stage('Python Stage') {
            agent {
                pythonAgent {
                    // Agent specific configuration or overrides if needed
                    retries 2
                }
            }
            steps {
                sh 'python --version'
            }
        }
        stage('Node.js Stage') {
            agent {
                nodejsAgent {
                    // Agent specific configuration or overrides if needed
                    retries 2
                }
            }
            steps {
                sh 'node --version'
            }
        }
    }
}




// vars/pythonAgent.groovy
def call(Closure body) {
    def pythonYaml = libraryResource 'podtemplates/python.yaml'
    kubernetes {
        yaml pythonYaml
        body()
    }
}

// vars/nodejsAgent.groovy
def call(Closure body) {
    def nodejsYaml = libraryResource 'podtemplates/nodejs.yaml'
    kubernetes {
        yaml nodejsYaml
        body()
    }
}



(root)
+-- src
|   +-- com
|       +-- yourdomain
|           +-- YourLibrary.groovy
+-- vars
|   +-- yourLibrary.groovy
+-- resources
    +-- podtemplates
        +-- python.yaml
        +-- nodejs.yaml

# python.yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    type: jenkins-agent
spec:
  containers:
  - name: python
    image: python:3.9
    command: ["sleep", "9999"]
  restartPolicy: Never


# nodejs.yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    type: jenkins-agent
spec:
  containers:
  - name: nodejs
    image: node:14
    command: ["sleep", "9999"]
  restartPolicy: Never

@Library('your-library-name') _

pipeline {
    agent any
    stages {
        stage('Python Stage') {
            agent {
                podAgent(nodetype: 'python', retries: 2) {
                    // Additional agent configuration if needed
                }
            }
            steps {
                sh 'python --version'
            }
        }
        stage('Node.js Stage') {
            agent {
                podAgent(nodetype: 'nodejs', retries: 2) {
                    // Additional agent configuration if needed
                }
            }
            steps {
                sh 'node --version'
            }
        }
    }
}


// vars/podAgent.groovy
def call(Map config = [:], Closure body) {
    def yamlFile
    switch (config.nodetype) {
        case 'python':
            yamlFile = libraryResource 'podtemplates/python.yaml'
            break
        case 'nodejs':
            yamlFile = libraryResource 'podtemplates/nodejs.yaml'
            break
        default:
            error "Unknown nodetype: ${config.nodetype}"
    }
    kubernetes {
        yaml yamlFile
        if (config.containsKey('retries')) {
            retries config.retries
        }
        body()
    }
}
