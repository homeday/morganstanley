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



pipeline {
    agent any

    environment {
        VAULT_ADDR = 'https://vault.example.com'
        KRB5_CONFIG = '/etc/krb5.conf' // Path to your Kerberos configuration file
        KRB5_KEYTAB = '/path/to/your.keytab' // Path to your Kerberos keytab file
        KRB5_PRINCIPAL = 'your_principal@YOUR.REALM' // Your Kerberos principal
    }

    stages {
        stage('Authenticate with Kerberos and Get Vault Token') {
            steps {
                script {
                    // Authenticate with Kerberos
                    sh 'kinit -kt $KRB5_KEYTAB $KRB5_PRINCIPAL'

                    // Get Vault token using Kerberos authentication
                    def vaultToken = sh(script: 'vault login -method=kerberos -format=json | jq -r .auth.client_token', returnStdout: true).trim()

                    // Set the Vault token as an environment variable
                    env.VAULT_TOKEN = vaultToken
                }
            }
        }
        stage('Retrieve Secrets') {
            steps {
                script {
                    def secrets = [
                        [path: 'secret/data/myapp', engineVersion: 2, secretValues: [
                            [envVar: 'MY_SECRET', vaultKey: 'my_secret_key']
                        ]]
                    ]
                    def configuration = [vaultUrl: env.VAULT_ADDR, vaultToken: env.VAULT_TOKEN]
                    vault(configuration: configuration, vaultSecrets: secrets)
                }
                echo "Retrieved secret: ${env.MY_SECRET}"
            }
        }
        // Other stages...
    }
}


pipeline {
    agent any
    environment {
        VAULT_ADDR = 'https://vault.example.com'
    }
    stages {
        stage('Retrieve Secret with Kerberos Token') {
            steps {
                script {
                    // Retrieve the Vault token dynamically from a Jenkins credential
                    withCredentials([string(credentialsId: 'vault-token-id', variable: 'VAULT_TOKEN')]) {
                        withVault(
                            configuration: [
                                vaultUrl: VAULT_ADDR,
                                vaultCredentialId: '' // Leave this empty
                            ],
                            extraEnvironment: [
                                "VAULT_TOKEN=${env.VAULT_TOKEN}"
                            ],
                            vaultSecrets: [vaultSecret(path: 'secret/my-secret', secretValues: [
                                [envVar: 'MY_SECRET', vaultKey: 'password']
                            ])]
                        ) {
                            sh 'echo "My secret is $MY_SECRET"'
                        }
                    }
                }
            }
        }
    }
}


