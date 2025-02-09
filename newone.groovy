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




pipeline {
    agent any
    stages {
        stage('Example') {
            steps {
                script {
                    // Define a password variable
                    def pass = 'secretPassword'
                    
                    // Use the Mask Passwords Plugin to mask the password
                    maskPasswords(varPasswordPairs: [[var: 'MY_PASSWORD', password: pass]]) {
                        sh 'echo Password is: $MY_PASSWORD'
                    }
                }
            }
        }
    }
}


// Requires https://plugins.jenkins.io/mask-passwords to run

/**
 * Runs code with secret environment variables and hides the values.
 *
 * @param varAndPasswordList - A list of Maps with a 'var' and 'password' key.  Example: `[[var: 'TOKEN', password: 'sekret']]`
 * @param Closure - The code to run in
 * @return {void}
 */
def withSecretEnv(List<Map> varAndPasswordList, Closure closure) {
  wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: varAndPasswordList]) {
    withEnv(varAndPasswordList.collect { "${it.var}=${it.password}" }) {
      closure()
    }
  }
}

// Example code:
node {
  withSecretEnv([[var: 'VAULT_TOKEN', password: 'toosekret']]) {
    sh '''#!/bin/bash -eu
    echo "with env use:      ${VAULT_TOKEN}"
    sleep 1
    echo "without env use:   toosekret"
    sleep 1
    echo "just the var name: VAULT_TOKEN"
    '''
    sleep 1
    echo "Outside SH: VAULT_TOKEN=${VAULT_TOKEN}"
  }
}


apiVersion: v1
kind: Secret
metadata:
  name: my-certs
type: Opaque
data:
  cert1.pem: <base64-encoded-cert1>
  cert2.pem: <base64-encoded-cert2>
  cert3.pem: <base64-encoded-cert3>



apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: my-container
    image: my-image
    volumeMounts:
    - name: certs
      mountPath: "/etc/certs"
  volumes:
  - name: certs
    secret:
      secretName: my-certs


apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: my-container1
    image: my-image1
    volumeMounts:
    - name: certs
      mountPath: "/etc/certs"
  - name: my-container2
    image: my-image2
    volumeMounts:
    - name: certs
      mountPath: "/etc/certs"
  volumes:
  - name: certs
    secret:
      secretName: my-certs

apiVersion: v1
kind: ConfigMap
metadata:
  name: my-config
data:
  config1.properties: |
    key1=value1
    key2=value2
  config2.properties: |
    keyA=valueA
    keyB=valueB

apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: my-container1
    image: my-image1
    volumeMounts:
    - name: config
      mountPath: "/etc/config"
  - name: my-container2
    image: my-image2
    volumeMounts:
    - name: config
      mountPath: "/etc/config"
  volumes:
  - name: config
    configMap:
      name: my-config


apiVersion: v1
kind: Secret
metadata:
  name: secret1
type: Opaque
data:
  cert1.pem: <base64-encoded-cert1>
---
apiVersion: v1
kind: Secret
metadata:
  name: secret2
type: Opaque
data:
  cert2.pem: <base64-encoded-cert2>


apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: my-container1
    image: my-image1
    volumeMounts:
    - name: secret1
      mountPath: "/etc/secret1"
    - name: secret2
      mountPath: "/etc/secret2"
  - name: my-container2
    image: my-image2
    volumeMounts:
    - name: secret1
      mountPath: "/etc/secret1"
    - name: secret2
      mountPath: "/etc/secret2"
  volumes:
  - name: secret1
    secret:
      secretName: secret1
  - name: secret2
    secret:
      secretName: secret2


apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: my-container1
    image: my-image1
    volumeMounts:
    - name: shared-volume
      mountPath: "/etc/shared"
  - name: my-container2
    image: my-image2
    volumeMounts:
    - name: shared-volume
      mountPath: "/etc/shared"
  volumes:
  - name: shared-volume
    secret:
      secretName: my-secret


#!/bin/bash

# Default values
default_arg="default_value"
default_path="/default/path"

# Initialize variables
arg=$default_arg
path=$default_path

# Parse named arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --arg) arg="$2"; shift ;;
        --path) path="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

echo "Argument is: $arg"
echo "Path is: $path"



#!/bin/bash

# Assign argument or use default path
default_path="/default/path"
path_arg=${1:-$default_path}

echo "Path is: $path_arg"



pipeline {
    agent any

    stages {
        stage('Capture HTTP Response') {
            steps {
                script {
                    def responseContent = powershell(script: '''
                        $url = "https://example.com/api"
                        $response = Invoke-WebRequest -Uri $url -UseBasicParsing

                        if ($response.StatusCode -ne 200) {
                            Throw "HTTP request failed with status: $($response.StatusCode)"
                        }

                        # Only return the response content (Jenkins captures the output)
                        Write-Output $response.Content
                    ''', returnOutput: true)

                    echo "Captured Response Content: ${responseContent}"
                }
            }
        }
    }
}

