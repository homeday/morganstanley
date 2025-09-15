class CKSAgentConfHelper implements Serializable {
    private static final def TGT_SECRET = 'tgt-secret'
    private static final def TGT_VOLUME = 'tgt-volume'
    private static final def KERBEROS_PATH = '/tmp/kerberos_jenkins'
    private static final def JNLP_AGENT_TYPE = 'jnlp'
    private static final def TGT_REFRESHER_AGENT_TYPE = 'tgtrefresher'
    private static final def CUSTOM_AGENT_TYPE = 'custom'

    private final def script
    private final CommonPropertiesMgr comPropMgr

    CKSAgentConfHelper(def script) {
        this.script = script
        this.comPropMgr = new CommonPropertiesMgr(script)
    }

    def getAgentYaml(def agents, Map options = [:]) {
        assert agents != null : "Agents list cannot be null"

        def keytabCredentialId = options.get("McCredit", "")
        def kerberosPrincipal = options.get("kerberosPrincipal", "default_user@REALM")
        def keytabContent = ""

        // Load the keytab base64 string from Jenkins Secret Text Credential
        if (keytabCredentialId) {
            script.withCredentials([script.string(credentialsId: keytabCredentialId, variable: 'KEYTAB_B64')]) {
                keytabContent = script.KEYTAB_B64
            }
        }

        def poYaml = readBasePodTemplate()

        agents.eachWithIndex { agent, index ->
            def container = buildContainerConfig(agent, index, keytabCredentialId)
            poYaml.spec.containers += container
        }

        addKerberosConfig(poYaml, keytabCredentialId, keytabContent, kerberosPrincipal)
        return script.writeYaml(data: poYaml, returnText: true)
    }

    private def readBasePodTemplate() {
        script.readYaml(text: getAgentConfig(JNLP_AGENT_TYPE))
    }

    private def buildContainerConfig(def agent, int index, def keytabCredentialId) {
        def containerYaml
        if (agent instanceof Map) {
            containerYaml = buildCustomContainer(agent)
            containerYaml.spec.containers[0].name = agent?.containerName ?: "container${index + 1}"
        } else {
            containerYaml = buildPredefinedContainer(agent)
        }

        containerYaml.spec.containers[0].resources = comPropMgr.jobResMapping.get(
            script.env.JOB_NAME,
            comPropMgr.defaultResourceSsetting
        )

        addKerberosVolumeMounts(containerYaml.spec.containers[0], keytabCredentialId)
        return containerYaml.spec.containers
    }

    private def buildCustomContainer(Map agent) {
        validateImageSource(agent.image)
        return script.readYaml(text: getAgentConfig(CUSTOM_AGENT_TYPE))
    }

    private def buildPredefinedContainer(def agentType) {
        script.readYaml(text: getAgentConfig(agentType))
    }

    private void validateImageSource(def image) {
        def validSources = [comPropMgr.afAddress, comPropMgr.edgeAddress]*.toLowerCase()
        if (!validSources.any { image?.toLowerCase()?.startsWith(it) }) {
            script.error("Image must be from Artifactory: ${validSources.join(' or ')}")
        }
    }

    private def addKerberosVolumeMounts(def container, def keytabCredentialId) {
        def volumeName = keytabCredentialId ? TGT_VOLUME : TGT_SECRET
        container.volumeMounts = (container.volumeMounts ?: []) << [
            name     : volumeName,
            mountPath: KERBEROS_PATH,
            readOnly : true
        ]

        container.env << [name: 'KRBSCCNAME',
                          value: "${KERBEROS_PATH}/krb5cc_${keytabCredentialId ?: 'msde_agent_qa'}"]

        if (keytabCredentialId) {
            container.env << [name: comPropMgr.tgt_keytab_cred_id, value: keytabCredentialId]
        }
    }

    private void addKerberosConfig(Object podYaml, def keytabCredentialId, def keytabContent, def principal) {
        if (keytabCredentialId) {
            podYaml.spec.volumes = (podYaml.spec.volumes ?: []) << [
                name    : TGT_VOLUME,
                emptyDir: [:]
            ]
            addTgtRefresherInitContainer(podYaml, keytabContent, principal)
        } else {
            podYaml.spec.volumes = (podYaml.spec.volumes ?: []) << [
                name  : TGT_SECRET,
                secret: [secretName: 'msde-agent-qa-tgt']
            ]
        }
    }

    private void addTgtRefresherInitContainer(Object podYaml, def keytabB64, def principal) {
        def tgtYaml = script.readYaml(text: getAgentConfig(TGT_REFRESHER_AGENT_TYPE))
        def container = tgtYaml.spec.initContainers[0]

        container.command = ["/bin/sh", "-c"]
        container.args = ["""
            echo "$KEYTAB_B64" | base64 -d > ${KERBEROS_PATH}/user.keytab && \
            kinit -kt ${KERBEROS_PATH}/user.keytab ${principal}
        """.stripIndent().trim()]

//         def initScript = """
//     |#!/bin/sh
//     |echo "Decoding keytab"
//     |echo "$KEYTAB_B64" | base64 -d > /tmp/keytab && \
//     |kinit -kt /tmp/keytab user@REALM
// """.stripMargin()


        container.env = (container.env ?: []) + [
            [name: "KEYTAB_B64", value: keytabB64],
            [name: "KRBSCCNAME", value: "${KERBEROS_PATH}/krb5cc_${principal.replaceAll('@', '_')}"]
        ]

        container.volumeMounts = (container.volumeMounts ?: []) + [
            [name: TGT_VOLUME, mountPath: KERBEROS_PATH]
        ]

        podYaml.spec.initContainers = (podYaml.spec.initContainers ?: []) + [container]
    }


    private def getAgentConfig(def agentType) {
        script.libraryResource("com/cn/ms/msde/jenkins/podtemplates/${agentType}.yaml")
    }
}


import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud

def getK8sNamespace(String cloudName) {
    def jenkins = jenkins.model.Jenkins.get()
    def cloud = jenkins.clouds.getByName(cloudName) as KubernetesCloud
    return cloud?.namespace ?: "default"
}