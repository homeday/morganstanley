pipeline {
    agent none
    stages {
        stage('Detect namespace and create secret') {
            steps {
                script {
                    import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud
                    import com.cloudbees.plugins.credentials.common.StandardCredentials
                    import org.jenkinsci.plugins.plaincredentials.StringCredentials
                    import jenkins.model.Jenkins

                    def cloudName = "k8s"
                    def cloud = Jenkins.instance.clouds.getByName(cloudName)
                    if (!(cloud instanceof KubernetesCloud)) {
                        error "Cloud '${cloudName}' is not a KubernetesCloud."
                    }

                    def apiServer = cloud.serverUrl
                    def credId = cloud.credentialsId

                    def creds = com.cloudbees.plugins.credentials.CredentialsProvider
                        .lookupCredentials(StandardCredentials.class, Jenkins.instance, null, null)
                        .find { it.id == credId }
                    if (!(creds instanceof StringCredentials)) {
                        error "Credentials ID '${credId}' is not a Secret Text."
                    }
                    def token = creds.secret.getPlainText()

                    // Helper method to perform GET requests
                    def httpGet = { url ->
                        try {
                            def resp = httpRequest(
                                url: url,
                                httpMode: 'GET',
                                customHeaders: [[name: 'Authorization', value: "Bearer ${token}"]],
                                validResponseCodes: '200',
                                ignoreSslErrors: true
                            )
                            return resp.content
                        } catch (Exception e) {
                            return null
                        }
                    }

                    // Try to list namespaces
                    def nsListJson = httpGet("${apiServer}/api/v1/namespaces")
                    def namespaces = []
                    if (nsListJson) {
                        def jsonSlurper = new groovy.json.JsonSlurper()
                        def nsList = jsonSlurper.parseText(nsListJson)
                        namespaces = nsList.items.collect { it.metadata.name }
                    }

                    if (namespaces.isEmpty()) {
                        // Fallback list of common namespaces
                        namespaces = ["default", "dev", "qa", "prod", "staging"]
                    }

                    def detectedNamespace = null
                    def jsonSlurper = new groovy.json.JsonSlurper()

                    // Try reading the default ServiceAccount in each namespace
                    for (ns in namespaces) {
                        def url = "${apiServer}/api/v1/namespaces/${ns}/serviceaccounts/default"
                        try {
                            def resp = httpRequest(
                                url: url,
                                httpMode: 'GET',
                                customHeaders: [[name: 'Authorization', value: "Bearer ${token}"]],
                                validResponseCodes: '200',
                                ignoreSslErrors: true
                            )
                            // Success means token can access this namespace
                            detectedNamespace = ns
                            break
                        } catch (Exception ignored) {
                            // Ignore forbidden or not found
                        }
                    }

                    if (detectedNamespace == null) {
                        error "No accessible namespace found for this token"
                    }

                    echo "Detected namespace: ${detectedNamespace}"

                    // Prepare secret JSON
                    def secretJson = groovy.json.JsonOutput.toJson([
                        apiVersion: "v1",
                        kind: "Secret",
                        metadata: [name: "my-secret"],
                        type: "Opaque",
                        data: [
                            username: "YWRtaW4=", // base64("admin")
                            password: "cGFzc3dvcmQ=" // base64("password")
                        ]
                    ])

                    // Create the secret
                    def createResp = httpRequest(
                        url: "${apiServer}/api/v1/namespaces/${detectedNamespace}/secrets",
                        httpMode: 'POST',
                        customHeaders: [[name: 'Authorization', value: "Bearer ${token}"]],
                        requestBody: secretJson,
                        contentType: 'APPLICATION_JSON',
                        validResponseCodes: '200:299',
                        ignoreSslErrors: true
                    )

                    echo "Secret creation response status: ${createResp.status}"
                    echo "Response content: ${createResp.content}"
                }
            }
        }
    }
}


def detectNamespace = { apiServer, token ->
    // Step 1: Try listing all namespaces (might fail if not allowed)
    def nsList = sh(
        script: """
            curl -s -k -H "Authorization: Bearer ${token}" \
                 ${apiServer}/api/v1/namespaces | jq -r '.items[].metadata.name' 2>/dev/null || true
        """,
        returnStdout: true
    ).trim().split("\n")

    if (!nsList || nsList.size() == 0) {
        // Step 2: If listing failed, brute-force common namespaces
        nsList = ["default", "dev", "qa", "prod", "staging"]
    }

    // Step 3: Check which one works by reading its default ServiceAccount
    for (ns in nsList) {
        def code = sh(
            script: """
                curl -s -o /dev/null -w "%{http_code}" -k \
                     -H "Authorization: Bearer ${token}" \
                     ${apiServer}/api/v1/namespaces/${ns}/serviceaccounts/default
            """,
            returnStdout: true
        ).trim()

        if (code == "200") {
            return ns
        }
    }

    error "No accessible namespace found for this token"
}


pipeline {
    agent none
    stages {
        stage('Detect namespace and create secret') {
            steps {
                script {
                    import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud
                    import com.cloudbees.plugins.credentials.common.StandardCredentials
                    import org.jenkinsci.plugins.plaincredentials.StringCredentials
                    import jenkins.model.Jenkins

                    def cloudName = "k8s"
                    def cloud = Jenkins.instance.clouds.getByName(cloudName)
                    if (!(cloud instanceof KubernetesCloud)) {
                        error "Cloud '${cloudName}' is not a KubernetesCloud."
                    }

                    def apiServer = cloud.serverUrl
                    def credId = cloud.credentialsId

                    def creds = com.cloudbees.plugins.credentials.CredentialsProvider
                        .lookupCredentials(StandardCredentials.class, Jenkins.instance, null, null)
                        .find { it.id == credId }
                    if (!(creds instanceof StringCredentials)) {
                        error "Credentials ID '${credId}' is not a Secret Text."
                    }
                    def token = creds.secret.getPlainText()

                    // Detect the namespace
                    def namespace = detectNamespace(apiServer, token)
                    echo "Detected namespace: ${namespace}"

                    // Create the secret in detected namespace
                    def secretJson = '''
                    {
                      "apiVersion": "v1",
                      "kind": "Secret",
                      "metadata": { "name": "my-secret" },
                      "type": "Opaque",
                      "data": {
                        "username": "YWRtaW4=",
                        "password": "cGFzc3dvcmQ="
                      }
                    }
                    '''

                    sh """
                        curl -k -X POST \\
                          -H "Authorization: Bearer ${token}" \\
                          -H "Content-Type: application/json" \\
                          ${apiServer}/api/v1/namespaces/${namespace}/secrets \\
                          -d '${secretJson.replace("'", "'\\''")}'
                    """
                }
            }
        }
    }
}
