apiVersion: v1
kind: Pod
metadata:
  name: my-jenkins-agent
spec:
  containers:
  - name: jenkins-agent
    image: jenkins/inbound-agent:latest
    command: ["java"]
    args: ["-jar", "/usr/local/jenkins-slave.jar", "-jnlpUrl", "${JENKINS_AGENT_JNLPS_URL}", "-secretFile", "/secrets/secret.file"]
    volumeMounts:
    - name: jenkins-secrets
      mountPath: /secrets
  volumes:
  - name: jenkins-secrets
    secret:
      secretName: my-jenkins-agent-secret


@Library('my-shared-library') _

pipeline {
  agent {
    kubernetes {
      cloudName 'my-kubernetes-cloud'
      yaml loadKubernetesPodTemplate()
    }
  }
  stages {
    stage('Build') {
      steps {
        sh 'make build'
      }
    }
  }
}


def yamlContent = libraryResource 'pod-template.yaml'
def image = 'my-image'
def tag = 'latest'
yamlContent = yamlContent.replaceAll(/\${IMAGE_NAME}/, image)
                  .replaceAll(/\${IMAGE_TAG}/, tag)

@Library('my-shared-library') _

pipeline {
  agent {
    kubernetes {
      cloudName 'my-kubernetes-cloud'
      yaml {
        def yamlContent = libraryResource 'pod-template.yaml'
        def image = 'my-image'
        def tag = 'latest'
        yamlContent = yamlContent.replaceAll(/\${IMAGE_NAME}/, image)
                                 .replaceAll(/\${IMAGE_TAG}/, tag)
        return yamlContent
      }
    }
  }
  // ... rest of your pipeline
}


def myKubernetesAgent(Closure body) {
    kubernetes {
        cloudName 'my-kubernetes-cloud'
        yaml {
            def yamlContent = libraryResource 'pod-template.yaml'
            def image = 'my-image'
            def tag = 'latest'
            yamlContent = yamlContent.replaceAll(/\${IMAGE_NAME}/, image)
                                     .replaceAll(/\${IMAGE_TAG}/, tag)
            return yamlContent
        }
    }
    body()
}
