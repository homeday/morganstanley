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


import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder

def call(String agentType, String imageName) {
    // Define the resource path for the YAML file
    String yamlPath = "k8s/${agentType}.yaml"
    def yamlFile = libraryResource(yamlPath)
    
    // Parse the YAML file
    def yamlSlurper = new YamlSlurper()
    def config = yamlSlurper.parseText(yamlFile)

    // Replace the image name
    config.spec.containers[0].image = imageName

    // Convert the updated config back to YAML
    def yamlBuilder = new YamlBuilder()
    yamlBuilder config
    return yamlBuilder.toString()
}

