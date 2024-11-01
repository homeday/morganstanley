apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-service-account
imagePullSecrets:
- name: my-registry-secret

podTemplate {
    containers {
        containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave') {
        }
    }
    spec {
        serviceAccountName: 'my-jenkins-agent-sa'
    }
}

podTemplate(name: 'base-template', containers: [
    containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:alpine', args: '${computer.jnlpmac} ${computer.name}'),
    containerTemplate(name: 'utility', image: 'alpine', command: 'cat', ttyEnabled: true)
]) {
    // Base pod configuration
}



pipeline {
    agent {
        kubernetes {
            inheritFrom 'base-template'
            yaml """
            apiVersion: v1
            kind: Pod
            spec:
              containers:
              - name: custom-container
                image: my-custom-image:latest
                command: ['cat']
                tty: true
            """
        }
    }
    stages {
        stage('Build') {
            steps {
                container('custom-container') {
                    script {
                        echo 'Building...'
                        sh 'echo "Run some build commands here"'
                    }
                }
            }
        }
        stage('Test') {
            steps {
                container('custom-container') {
                    script {
                        echo 'Testing...'
                        sh 'echo "Run some test commands here"'
                    }
                }
            }
        }
    }
}



pipeline {
    agent none
    stages {
        stage('Setup Pod Template') {
            steps {
                script {
                    podTemplate(inheritFrom: 'base-template', containers: [
                        containerTemplate(name: 'custom-container', image: 'my-custom-image:latest', command: 'cat', ttyEnabled: true),
                        containerTemplate(name: 'additional-container', image: 'another-custom-image:latest', command: 'sleep', args: 'infinity', ttyEnabled: true)
                    ]) {
                        node(POD_LABEL) {
                            stage('Build') {
                                container('custom-container') {
                                    script {
                                        echo 'Building...'
                                        sh 'echo "Run some build commands here"'
                                    }
                                }
                            }
                            stage('Test') {
                                container('additional-container') {
                                    script {
                                        echo 'Testing...'
                                        sh 'echo "Run some test commands here"'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


pipeline {
    agent none
    stages {
        stage('Setup Pod Template') {
            steps {
                script {
                    podTemplate(inheritFrom: 'base-template', containers: [
                        containerTemplate(name: 'custom-container', image: 'my-custom-image:latest', command: 'cat', ttyEnabled: true, 
                        volumeMounts: [volumeMount(mountPath: '/my-mount-path', name: 'my-volume')])
                    ], 
                    volumes: [persistentVolumeClaim(claimName: 'my-pvc', mountPath: '/my-mount-path')]) {
                        node(POD_LABEL) {
                            stage('Build') {
                                container('custom-container') {
                                    script {
                                        echo 'Building...'
                                        sh 'echo "Run some build commands here"'
                                    }
                                }
                            }
                            stage('Test') {
                                container('custom-container') {
                                    script {
                                        echo 'Testing...'
                                        sh 'echo "Run some test commands here"'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// vars/basePod.groovy
def call() {
    return podTemplate(name: 'base-template', containers: [
        containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:alpine', args: '${computer.jnlpmac} ${computer.name}'),
        containerTemplate(name: 'utility', image: 'alpine', command: 'cat', ttyEnabled: true)
    ])
}


@Library('my-shared-library') _

pipeline {
    agent {
        kubernetes {
            inheritFrom 'base-template'
            yaml """
            apiVersion: v1
            kind: Pod
            spec:
              containers:
              - name: custom-container
                image: my-custom-image:latest
                command: ['cat']
                tty: true
              - name: additional-container
                image: another-custom-image:latest
                command: ['sleep', 'infinity']
                tty: true
            """
        }
    }
    stages {
        stage('Initialize') {
            steps {
                script {
                    basePod()
                }
            }
        }
        stage('Build') {
            steps {
                container('custom-container') {
                    script {
                        echo 'Building...'
                        sh 'echo "Run some build commands here"'
                    }
                }
            }
        }
        stage('Test') {
            steps {
                container('additional-container') {
                    script {
                        echo 'Testing...'
                        sh 'echo "Run some test commands here"'
                    }
                }
            }
        }
    }
}



pipeline {
    agent none
    stages {
        stage('Setup Pod Template') {
            steps {
                script {
                    podTemplate(containers: [
                        containerTemplate(name: 'custom-container', image: 'my-custom-image:latest', command: 'cat', ttyEnabled: true, 
                        volumeMounts: [volumeMount(mountPath: '/etc/config', name: 'config-volume')])
                    ], 
                    volumes: [configMapVolume(configMapName: 'my-config', mountPath: '/etc/config', items: [keyToPath(key: 'my-key', path: 'my-key')])]) {
                        node(POD_LABEL) {
                            stage('Build') {
                                container('custom-container') {
                                    script {
                                        echo 'Building...'
                                        sh 'echo "Run some build commands here"'
                                    }
                                }
                            }
                            stage('Test') {
                                container('custom-container') {
                                    script {
                                        echo 'Testing...'
                                        sh 'cat /etc/config/my-key'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


pipeline {
    agent none
    stages {
        stage('Setup Pod Template') {
            steps {
                script {
                    podTemplate(inheritFrom: 'base-template', containers: [
                        containerTemplate(name: 'custom-container', image: 'my-custom-image:latest', command: 'cat', ttyEnabled: true, 
                        volumeMounts: [volumeMount(mountPath: '/etc/config', name: 'config-volume')])
                    ], 
                    volumes: [configMapVolume(configMapName: 'my-config', mountPath: '/etc/config')]) {
                        node(POD_LABEL) {
                            stage('Build') {
                                container('custom-container') {
                                    script {
                                        echo 'Building...'
                                        sh 'echo "Run some build commands here"'
                                    }
                                }
                            }
                            stage('Test') {
                                container('custom-container') {
                                    script {
                                        echo 'Testing...'
                                        sh 'cat /etc/config/my-key'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Library('my-shared-library') _

import org.jenkinsci.plugins.yourlibrary.BasePodTemplate

pipeline {
    agent none
    stages {
        stage('Setup Pod Template') {
            steps {
                script {
                    def basePodTemplate = new BasePodTemplate()
                    podTemplate(
                        inheritFrom: 'base-template',
                        containers: basePodTemplate.getCustomPodTemplate().containers,
                        volumes: basePodTemplate.getCustomPodTemplate().volumes
                    ) {
                        node(POD_LABEL) {
                            stage('Build') {
                                container('custom-container') {
                                    script {
                                        echo 'Building...'
                                        sh 'echo "Run some build commands here"'
                                    }
                                }
                            }
                            stage('Test') {
                                container('custom-container') {
                                    script {
                                        echo 'Testing...'
                                        sh 'cat /etc/config/my-key'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


package org.jenkinsci.plugins.yourlibrary

import org.csanchez.jenkins.plugins.kubernetes.*

class BasePodTemplate {

    PodTemplate getBasePodTemplate() {
        return new PodTemplate(containers: [
            new ContainerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:alpine', args: '${computer.jnlpmac} ${computer.name}'),
            new ContainerTemplate(name: 'utility', image: 'alpine', command: 'cat', ttyEnabled: true)
        ])
    }

    PodTemplate getCustomPodTemplate() {
        return new PodTemplate(containers: [
            new ContainerTemplate(name: 'custom-container', image: 'my-custom-image:latest', command: 'cat', ttyEnabled: true, 
            volumeMounts: [new VolumeMount('/etc/config', 'config-volume', false)])
        ], 
        volumes: [new ConfigMapVolume('my-config', '/etc/config')])
    }
}


package org.jenkinsci.plugins.yourlibrary

import org.csanchez.jenkins.plugins.kubernetes.*

class BasePodTemplate {
    static PodTemplate getBasePodTemplate() {
        return new PodTemplate(containers: [
            new ContainerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:alpine', args: '${computer.jnlpmac} ${computer.name}'),
            new ContainerTemplate(name: 'utility', image: 'alpine', command: 'cat', ttyEnabled: true)
        ])
    }

    static PodTemplate getCustomPodTemplate() {
        return new PodTemplate(containers: [
            new ContainerTemplate(name: 'custom-container', image: 'my-custom-image:latest', command: 'cat', ttyEnabled: true, 
            volumeMounts: [new VolumeMount('/etc/config', 'config-volume', false)])
        ], 
        volumes: [new ConfigMapVolume('my-config', '/etc/config')])
    }
}

