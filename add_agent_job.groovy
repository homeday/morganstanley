import jenkins.model.*
import hudson.slaves.*
import hudson.slaves.JNLPLauncher

node {
    stage('Add Node and Generate Command') {
        script {
            def jenkins = Jenkins.instance
            def nodeName = "new-node"
            def remoteFS = "/home/jenkins"
            def numExecutors = 2
            def label = "linux"
            def launcher = new JNLPLauncher()
            def retentionStrategy = new RetentionStrategy.Always()
            def node = new DumbSlave(nodeName, remoteFS, launcher)
            node.setNumExecutors(numExecutors)
            node.setLabelString(label)
            node.setRetentionStrategy(retentionStrategy)
            jenkins.addNode(node)
            
            // Generate the connection command
            def jenkinsUrl = jenkins.getRootUrl()
            def secret = node.getComputer().getJnlpMac()
            def workDir = remoteFS
            def connectionCommand = "java -jar slave.jar -url ${jenkinsUrl} -secret ${secret} -name ${nodeName} -workDir ${workDir}"
            
            echo "Connection Command: ${connectionCommand}"
        }
    }
}
