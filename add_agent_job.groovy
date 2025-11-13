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


import com.cloudbees.hudson.plugins.folder.*
import jenkins.model.*

def folder = Jenkins.instance.getItemByFullName("folderA")
def prop = folder.getProperties().get(Folder.ComputedFolderRunExclusiveProperty)

println prop

//=========================

import com.cloudbees.hudson.plugins.folder.*
import com.cloudbees.hudson.plugins.folder.properties.FolderJobProperty
import jenkins.model.*

def folder = Jenkins.instance.getItemByFullName("folderA")
def prop = folder.getProperties().get(FolderJobProperty)
if (prop != null && prop.getRestrictLabel() != null) {
    println "Folder restricts jobs to label: ${prop.getRestrictLabel()}"
} else {
    println "No restriction set."
}


//===========================

import com.cloudbees.hudson.plugins.folder.*
import com.cloudbees.hudson.plugins.folder.properties.FolderJobProperty
import jenkins.model.*

def folder = Jenkins.instance.getItemByFullName("folderA")
def prop = new FolderJobProperty(true, "k8s-a")  // (restrict=true, label='k8s-a')
folder.addProperty(prop)
folder.save()
println "Restriction set for folderA to label 'k8s-a'"


//===========================
import com.cloudbees.hudson.plugins.folder.*
import com.cloudbees.hudson.plugins.folder.properties.FolderJobProperty
import jenkins.model.*

// Change these variables
def folderName = "folderA"
def newLabel = "k8s-b"

def folder = Jenkins.instance.getItemByFullName(folderName)
if (folder == null) {
    println "Folder '${folderName}' not found!"
    return
}

// Check if the property already exists
def prop = folder.getProperties().get(FolderJobProperty)

if (prop != null) {
    println "Existing restriction found: ${prop.getRestrictLabel()}"
    prop.setRestrictLabel(newLabel)
    prop.setRestrictRunWhere(true)
    println "Updated restriction to '${newLabel}'"
} else {
    println "No restriction found, creating a new one..."
    def newProp = new FolderJobProperty(true, newLabel)
    folder.addProperty(newProp)
}

folder.save()
println "Folder restriction is now set to '${newLabel}'"

