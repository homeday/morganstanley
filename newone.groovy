def call(Map config) {
    return {
        kubernetes {
            label config.label
            defaultContainer 'jnlp'
            yaml """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                some-label: ${config.label}
            spec:
              containers:
              - name: python
                image: python:3.8
                command:
                - cat
                tty: true
            """
        }
    }
}



pipeline {
    agent {
        pythonAgent(label: 'my-defined-label')
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
    }
}
