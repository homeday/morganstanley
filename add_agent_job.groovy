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


//===========================================================================================
package org.company

import java.util.logging.Logger
import java.util.logging.Level

/**
 * Production-grade Jenkins Shared Library logger.
 * - Uses JUL Logger to write to Jenkins master logs
 * - Correct caller info (using @NonCPS)
 * - Safe for CPS (static methods, no serialization)
 * - Includes custom formatting: timestamp, thread, job, build
 */
class LoggerUtil implements Serializable {

    // Use your own namespace so Jenkins Log Recorder can filter it
    private static final Logger LOGGER =
        Logger.getLogger("org.company.sharedlib")

    /**
     * Build custom formatted log prefix.
     *
     * Must be @NonCPS because it uses Date and system calls
     * that should not be CPS-transformed.
     */
    @NonCPS
    private static String formatMessage(String msg) {
        String ts = new Date().format("yyyy-MM-dd HH:mm:ss.SSS")
        String thread = Thread.currentThread().name

        // Works inside Jenkins Pipeline only
        String job   = System.getenv("JOB_NAME") ?: "unknown-job"
        String build = System.getenv("BUILD_NUMBER") ?: "0"

        return "[${ts}] [${thread}] [${job}#${build}] ${msg}"
    }

    @NonCPS
    static void info(String msg) {
        LOGGER.log(Level.INFO, formatMessage(msg))
    }

    @NonCPS
    static void warn(String msg) {
        LOGGER.log(Level.WARNING, formatMessage(msg))
    }

    @NonCPS
    static void error(String msg) {
        LOGGER.log(Level.SEVERE, formatMessage(msg))
    }

    @NonCPS
    static void debug(String msg) {
        LOGGER.log(Level.FINE, formatMessage(msg))
    }
}

import org.company.LoggerUtil

def info(String msg) {
    LoggerUtil.info(msg)
}

def warn(String msg) {
    LoggerUtil.warn(msg)
}

def error(String msg) {
    LoggerUtil.error(msg)
}

def debug(String msg) {
    LoggerUtil.debug(msg)
}

@Library('my-shared-lib') _

pipeline {
    agent any
    stages {
        stage('Test Logging') {
            steps {
                script {
                    logger.info("Pipeline started")
                    logger.warn("This is a warning")
                    logger.error("Something went wrong")
                    logger.debug("Debug message (visible only if log level=FINE)")
                }
            }
        }
    }
}


(
  kube_pod_status_phase{phase="Running", namespace=~"$namespace"}
  OR
  ((time() - kube_pod_created{namespace=~"$namespace"}) < 12*3600)
)


{
  "title": "Jenkins K8s Agents (Clean View)",
  "timezone": "browser",
  "schemaVersion": 39,
  "version": 1,
  "refresh": "30s",
  "templating": {
    "list": [
      {
        "name": "namespace",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(kube_pod_info, namespace)",
        "includeAll": true,
        "multi": true
      },
      {
        "name": "pod_age_hours",
        "type": "textbox",
        "label": "Show terminated pods younger than (hours)",
        "query": "",
        "current": {"text": "12", "value": "12"}
      },
      {
        "name": "jenkins_only",
        "type": "custom",
        "query": "true,false",
        "current": {"text": "true", "value": "true"},
        "label": "Only Jenkins Agent Pods"
      }
    ]
  },
  "panels": [
    {
      "type": "table",
      "title": "Pods (Filtered)",
      "datasource": "Prometheus",
      "targets": [
        {
          "expr": "kube_pod_info{namespace=~\"$namespace\"} "
                  + "and on(namespace,pod) ("
                  + "kube_pod_status_phase{phase=\"Running\", namespace=~\"$namespace\"}"
                  + " OR "
                  + "(time() - kube_pod_created < $pod_age_hours * 3600) "
                  + ") "
                  + "and on(pod) ("
                  + "$jenkins_only == \"true\" ? kube_pod_info{pod=~\"jenkins-agent-.*\"} : kube_pod_info"
                  + ")",
          "format": "table"
        }
      ],
      "options": {
        "showHeader": true
      }
    },
    {
      "type": "stat",
      "title": "Running Jenkins Agents",
      "targets": [
        {
          "expr": "count(kube_pod_status_phase{phase=\"Running\", pod=~\"jenkins-agent-.*\", namespace=~\"$namespace\"})"
        }
      ]
    },
    {
      "type": "stat",
      "title": "Recently Terminated Jenkins Agents (< $pod_age_hours h)",
      "targets": [
        {
          "expr": "count((time() - kube_pod_created < $pod_age_hours * 3600) and kube_pod_info{pod=~\"jenkins-agent-.*\", namespace=~\"$namespace\"})"
        }
      ]
    }
  ]
}


{
  "title": "Kubernetes Pods with Role & Jenkins Agents (Filtered <12h)",
  "timezone": "browser",
  "schemaVersion": 39,
  "version": 1,
  "refresh": "30s",
  "templating": {
    "list": [
      {
        "name": "namespace",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(kube_pod_info, namespace)",
        "includeAll": true,
        "multi": true
      },
      {
        "name": "role",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(kube_pod_info, role)",
        "includeAll": true,
        "multi": true
      },
      {
        "name": "pod_age_hours",
        "type": "textbox",
        "label": "Show terminated pods younger than (hours)",
        "query": "",
        "current": { "text": "12", "value": "12" }
      }
    ]
  },
  "panels": [
    {
      "type": "stat",
      "title": "Running Pods (All)",
      "targets": [
        {
          "expr": "count(kube_pod_status_phase{phase=\"Running\", namespace=~\"$namespace\"})"
        }
      ]
    },
    {
      "type": "stat",
      "title": "Running Pods by Role",
      "targets": [
        {
          "expr": "count(kube_pod_status_phase{phase=\"Running\", namespace=~\"$namespace\", role=~\"$role\"})"
        }
      ]
    },
    {
      "type": "stat",
      "title": "Running Jenkins Agents",
      "targets": [
        {
          "expr": "count(kube_pod_status_phase{phase=\"Running\", pod=~\"jenkins-agent-.*\", namespace=~\"$namespace\"})"
        }
      ]
    },
    {
      "type": "stat",
      "title": "Recently Terminated Pods (<$pod_age_hours h)",
      "targets": [
        {
          "expr": "count((time() - kube_pod_created{namespace=~\"$namespace\"}) < $pod_age_hours * 3600)"
        }
      ]
    },
    {
      "type": "table",
      "title": "All Pods (Running OR < $pod_age_hours h, role = $role)",
      "datasource": "Prometheus",
      "targets": [
        {
          "expr": "(kube_pod_info{namespace=~\"$namespace\", role=~\"$role\"}) and on(namespace, pod) (kube_pod_status_phase{phase=\"Running\", namespace=~\"$namespace\"} OR ((time() - kube_pod_created{namespace=~\"$namespace\"}) < $pod_age_hours * 3600))",
          "format": "table"
        }
      ],
      "options": { "showHeader": true }
    },
    {
      "type": "table",
      "title": "Jenkins Agents (Running OR < $pod_age_hours h)",
      "datasource": "Prometheus",
      "targets": [
        {
          "expr": "kube_pod_info{pod=~\"jenkins-agent-.*\", namespace=~\"$namespace\"} and on(namespace, pod) (kube_pod_status_phase{phase=\"Running\", namespace=~\"$namespace\"} OR ((time() - kube_pod_created{namespace=~\"$namespace\"}) < $pod_age_hours * 3600))",
          "format": "table"
        }
      ],
      "options": { "showHeader": true }
    },
    {
      "type": "table",
      "title": "Other Pods (Not Jenkins, role = $role)",
      "datasource": "Prometheus",
      "targets": [
        {
          "expr": "kube_pod_info{pod!~\"jenkins-agent-.*\", namespace=~\"$namespace\", role=~\"$role\"} and on(namespace, pod) (kube_pod_status_phase{phase=\"Running\", namespace=~\"$namespace\"} OR ((time() - kube_pod_created{namespace=~\"$namespace\"}) < $pod_age_hours * 3600))",
          "format": "table"
        }
      ],
      "options": { "showHeader": true }
    }
  ]
}



import groovy.json.JsonSlurper

// ================================
// 1. Settings
// ================================
def namespace = "default"
def labelSelector = "app=myapp"   // change this

// Token & CA cert of the pod running Jenkins
def token = new File("/var/run/secrets/kubernetes.io/serviceaccount/token").text.trim()
def caCertPath = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"

// Kubernetes API endpoint
def urlStr = "https://kubernetes.default.svc/api/v1/namespaces/${namespace}/pods?labelSelector=${labelSelector}"
println "Calling URL: ${urlStr}"

// ================================
// 2. Open HTTPS connection normally
// ================================
def url = new URL(urlStr)
def conn = url.openConnection()

conn.setRequestProperty("Authorization", "Bearer ${token}")
conn.setRequestProperty("Accept", "application/json")

// Load CA cert (Jenkins must trust Kubernetes CA)
def caCertFile = new File(caCertPath)
if (caCertFile.exists()) {
    System.setProperty("javax.net.ssl.trustStore", caCertPath)
}

// ================================
// 3. Fetch and parse JSON
// ================================
def responseText = conn.inputStream.text
def json = new JsonSlurper().parseText(responseText)

// ================================
// 4. Output result
// ================================
println ""
println "====== PODS FOUND ======"
json.items.each { pod ->
    println "- ${pod.metadata.name}    (phase=${pod.status.phase})"
}
println "========================"
println "Total pod count: ${json.items.size()}"

