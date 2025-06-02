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
        
        def poYaml = readBasePodTemplate()
        def keytabCredentiallD = options.get("McCredit", "")

        agents.eachWithIndex { agent, index ->
            def container = buildContainerConfig(agent, index, keytabCredentiallD)
            poYaml.spec.containers += container
        }

        addKerberosConfig(poYaml, keytabCredentiallD)
        return script.writeYaml(data: poYaml, returnText: true)
    }

    private def readBasePodTemplate() {
        script.readYaml(text: getAgentConfig(JNLP_AGENT_TYPE))
    }

    private def buildContainerConfig(def agent, int index, def keytabCredentiallD) {
        if (agent instanceof Map) {
            containerYaml = buildCustomContainer(agent)
            containerYaml.spec.containers[0].name = agent?.containerName ?: "container${index + 1}"
        } else { 
            containerYaml = buildPredefinedContainer(agent) 
        }
        containerYaml.spec.containers[0].resources = comPropMgr.jobResMapping.get(script.env.JOB_NAME, 
                                                                                  comPropMgr.defaultResourceSsetting)
        addKerberosVolumeMounts(containerYaml.spec.containers[0], keytabCredentiallD)
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

    private def addKerberosVolumeMounts(def container, def keytabCredentiallD) {
        def volumeName = keytabCredentiallD ? TGT_VOLUME : TGT_SECRET 
        container.volumeMounts = (container.volumeMounts ?: []) << [ 
            name: volumeName, 
            mountPath: KERBEROS_PATH, 
            readOnly: true
        ]

        container.env << [name: 'KRBSCCNAME',
                          value: "${KERBEROS_PATH}/krb5cci{keytabCredentialID ?: 'msde_agent_qa'}"]
        if (keytabCredentiallD) {
            container.env << [name: comPropMgr.tgt_keytab_cred_id, value: keytabCredentiallD]
        }
    }

    private void addKerberosConfig(Object podYaml, def keytabCredentiallD) {
        if (keytabCredentiallD) {
            podYaml.spec.volumes = (podYaml.spec.volumes ?: []) << [
                name: TGT_VOLUME,
                emptyDir: [medium: '']
            ]
            addTgtRefresherContainer(podYaml, keytabCredentiallD)
        } else {
            podYaml.spec.volumes = (podYaml.spec.volumes ?: []) << [
                name: TGT_SECRET,
                secret: [secretName: 'msde-agent-qa-tgt']
            ]
        }
    }

    private void addTgtRefresherContainer(Object podYaml, def keytabCredentiallD) {
        def tgtYaml = script.readYaml(text: getAgentConfig(TGT_REFRESHER_AGENT_TYPE))
        tgtYaml.spec.containers[0].env << [
            name: "KRBSCCNAME", 
            value: "${KERBEROS_PATH}/krb5cc_${keytabCredentiallD}"
        ]
        podYaml.spec.containers += tgtYaml.spec.containers
    }

    private def getAgentConfig(def agentType) {
        script.libraryResource("com/cn/ms/msde/jenkins/podtemplates/${agentType}.yaml")
    }
}