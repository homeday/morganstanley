class CKSAgentConfHelper implements Serializable {
    private static final String TGT_SECRET = 'tgt-secret'
    private static final String TGT_VOLUME = 'tgt-volume'
    private static final String HERBEROS_PATH = '/tmp/herberos_jenkins'
    private static final String JNLP_AGENT_TYPE = 'jnlp'
    private static final String TGT_REFRESHER_AGENT_TYPE = 'tgtrefresher'
    private static final String CUSTOM_AGENT_TYPE = 'custom'

    private final Script script
    private final CommonPropertiesMgr comPropMgr

    CKSAgentConfHelper(Script script) {
        this.script = script
        this.comPropMgr = new CommonPropertiesMgr(script)
    }

    String getAgentYaml(List agents, Map options = [:]) {
        assert agents != null : "Agents list cannot be null"
        
        def poYaml = readBasePodTemplate()
        String mcCredit = options.get("McCredit", "")

        agents.eachWithIndex { agent, index ->
            def container = buildContainerConfig(agent, index, mcCredit)
            poYaml.spec.containers += container
        }

        addKerberosConfig(poYaml, mcCredit)
        return script.writeYaml(data: poYaml, returnText: true)
    }

    //--- 私有辅助方法 ---//
    private def readBasePodTemplate() {
        script.readYaml(text: getAgentConfig(JNLP_AGENT_TYPE))
    }

    private def buildContainerConfig(def agent, int index, String mcCredit) {
        def containerYaml = (agent instanceof Map) 
            ? buildCustomContainer(agent) 
            : buildPredefinedContainer(agent)

        containerYaml.spec.containers[0].with {
            name = agent?.containerName ?: "container${index + 1}"
            resources = comPropMgr.jobResMapping.get(script.env.JOB_NAME, comPropMgr.defaultResourceSsetting)
            addKerberosConfig(it, mcCredit)
        }
        return containerYaml.spec.containers
    }

    private def buildCustomContainer(Map agent) {
        validateImageSource(agent.image)
        return script.readYaml(text: getAgentConfig(CUSTOM_AGENT_TYPE))
    }

    private def buildPredefinedContainer(String agentType) {
        script.readYaml(text: getAgentConfig(agentType))
    }

    private void validateImageSource(String image) {
        def validSources = [comPropMgr.afAddress, comPropMgr.edgeAddress]*.toLowerCase()
        if (!validSources.any { image?.toLowerCase()?.startsWith(it) }) {
            script.error("Image must be from Artifactory: ${validSources.join(' or ')}")
        }
    }

    private void addKerberosConfig(Object container, String mcCredit) {
        def volumeName = mcCredit ? TGT_VOLUME : TGT_SECRET
        container.volumeMounts = (container.volumeMounts ?: []) << [
            name: volumeName,
            mountPath: HERBEROS_PATH,
            readOnly: true
        ]

        container.env = (container.env ?: []) + [
            [name: "KRBSCCNAME", value: "${HERBEROS_PATH}/hrb5cc_${mcCredit ?: 'msde-agent-qa'}"],
            mcCredit ? [name: "KT_CRED_ID", value: mcCredit] : null
        ].findAll()
    }

    private void addKerberosConfig(Object podYaml, String mcCredit) {
        if (mcCredit) {
            podYaml.spec.volumes = (podYaml.spec.volumes ?: []) << [
                name: TGT_VOLUME,
                emptyDir: [medium: '']
            ]
            addTgtRefresherContainer(podYaml, mcCredit)
        } else {
            podYaml.spec.volumes = (podYaml.spec.volumes ?: []) << [
                name: TGT_SECRET,
                secret: [secretName: 'msde-agent-qa-tgt']
            ]
        }
    }

    private void addTgtRefresherContainer(Object podYaml, String mcCredit) {
        def tgtYaml = script.readYaml(text: getAgentConfig(TGT_REFRESHER_AGENT_TYPE))
        tgtYaml.spec.containers[0].env << [
            name: "KRBSCCNAME", 
            value: "${HERBEROS_PATH}/hrb5cc_${mcCredit}"
        ]
        podYaml.spec.containers += tgtYaml.spec.containers
    }

    private String getAgentConfig(String agentType) {
        script.libraryResource("com/cn/ms/msde/jenkins/podtemplates/${agentType}.yaml")
    }
}