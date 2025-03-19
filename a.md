# Jenkins Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [System Components](#system-components)
   - [Jenkins Master](#jenkins-master)
   - [Agents](#agents)
   - [External Services](#external-services)
   - [User Interaction](#user-interaction)
3. [Component Interactions](#component-interactions)
4. [Visual Representation](#visual-representation)
5. [Scalability Considerations](#scalability-considerations)
6. [Security Measures](#security-measures)
7. [Conclusion](#conclusion)

## Overview

This document provides a detailed overview of our Jenkins-based Continuous Integration/Continuous Deployment (CI/CD) pipeline. It outlines the system's components, their interactions, and considerations for scalability and security.

## System Components

### Jenkins Master

The Jenkins Master is the central orchestrator of the CI/CD pipeline. Its responsibilities include:

- **Job Scheduling:** Managing and triggering build jobs based on predefined criteria.
- **Plugin Management:** Extending Jenkins functionalities through various plugins.
- **Resource Allocation:** Assigning tasks to appropriate agents based on availability and capability.

In our setup, the Jenkins Master comprises:

- **Active Node:** The primary instance handling all operations.
- **Standby Node:** A failover instance ensuring high availability.

### Agents

Agents are responsible for executing tasks assigned by the Jenkins Master. We have categorized them as:

- **BYOA Node Inbound Agent:** Handles inbound connections for specific tasks.
- **MSDE Node SSH Agent:** Executes tasks over SSH, suitable for secure operations.
- **CKS Node Pod Agent:** Runs tasks within Kubernetes pods, facilitating containerized builds.

### External Services

Our Jenkins environment interacts with several external services:

- **BitBucket:** Source code repository triggering builds via webhooks.
- **NAS (Network Attached Storage):** Stores artifacts and backups accessible over NFS.
- **Artifactory:** Manages binary artifacts, serving as a repository for build outputs.
- **Jenkins QA:** Quality assurance instance for testing plugins and updates.
- **Update Center:** Provides updates and plugins for Jenkins.

### User Interaction

Users interact with the system through:

- **AI Self Services:** Automated services for adding agents and managing jobs.
- **JSM (Jira Service Management):** Platform for ticketing and issue tracking.
- **Train Application:** Application for training purposes, accessible via console commands.

## Component Interactions

The interactions between components are as follows:

- **User to Train Application:** Users issue console commands to the Train Application.
- **Train Application to AI Self Services:** Communicates via HTTPS RESTful APIs.
- **Agents to Artifactory:** Agents download and publish files to Artifactory.
- **BitBucket to Jenkins:** Webhooks notify Jenkins of events, triggering builds.
- **Jenkins to BitBucket:** Jenkins creates webhooks in BitBucket for integration.
- **Jenkins to MSDE Agent:** Connects over SSH for task execution.
- **BYOA and CKS Agents to Jenkins:** Communicate via Jenkins Remoting over TCP/IP port 7788.
- **Agents to BitBucket:** Access source code over HTTPS on port 443.
- **Jenkins to NAS:** Utilizes NFS for storage operations.
- **User to Jenkins:** Accesses Jenkins Portal for job management.
- **User to AI Self Services:** Interacts with the Train Application.
- **AI Self Services to Jenkins:** Manages agents and jobs.
- **User to JSM:** Submits tickets via JSM forms.
- **JSM to AI Self Services:** Notifies via webhooks.
- **Update Center to Jenkins QA:** Downloads plugins and updates.
- **Jenkins QA to Artifactory:** Publishes plugins.
- **Artifactory to NAS:** NAS downloads plugins from Artifactory.

## Visual Representation

Below is a visual representation of the Jenkins architecture:

```mermaid
flowchart TB
    subgraph Jenkins
        direction TB
        ActiveNode[Active Node]:::activeNode 
        StandbyNode[Standby Node]:::standbyNode
    end

    subgraph Agents
        BYOAAgent[[BYOA Node Inbound Agent]]:::agentNode
        MSDEAgent[[MSDE Node SSH Agent]]:::agentNode
        CKSAgent[[CKS Node Pod Agent]]:::cksAgent
    end

    subgraph "External Services"
        BitBucket([BitBucket]):::externalNode
        NAS[NAS]:::externalNode
        Artifactory([Artifactory]):::externalNode
    end

    subgraph "Internet Services"
        JenkinsQA([Jenkins QA]):::externalNode
        UpdateCenter[Update Center]:::externalNode
    end

    User((User)):::userNode
    AIServices[AI Self Services]:::externalNode
    JSM[JSM]:::externalNode
    TrainApp[Train Application]:::externalNode

    User --> |Console Command Line| TrainApp
    TrainApp --> |HTTPS RESTful APIs| AIServices
    Agents --> |Download/Publish files| Artifactory

    BitBucket --> |Webhook Event Notification| Jenkins
    Jenkins --> |Webhook Creation| BitBucket
    Jenkins --> |SSH| MSDEAgent
    BYOAAgent --> |Jenkins Remoting TCP/IP 7788| Jenkins
    CKSAgent --> |Jenkins Remoting TCP/IP 7788| Jenkins
    Agents --> |Source Code HTTPS 443| BitBucket
    Jenkins --> |NFS| NAS
    User --> |Jenkins Portal| Jenkins
    User --> |Train Application| AIServices
    AIServices --> |Add BYOA Agent| Jenkins
    AIServices --> |Add/Modify/Delete Jobs| Jenkins
    User --> |Open Ticket via JSM Form| JSM
    JSM --> |Web Hook| AIServices
    UpdateCenter --> |HTTPS Download/Update Plugins| JenkinsQA
    JenkinsQA --> |Publish Plugins| Artifactory
    Artifactory --> |Download Plugins| NAS

    classDef activeNode fill:#f9f,stroke:#333,stroke-width:2px,color:#000;
    classDef standbyNode fill:#bbf,stroke:#333,stroke-width:2px,stroke-dasharray: 5 5,color:#000;
    classDef agentNode fill:#ff9,stroke:#333,stroke-width:2px,color:#000;
    classDef cksAgent fill:#ff9,stroke:#333,stroke-width:2px,stroke-dasharray: 5 5,color:#000;
    classDef externalNode fill:#9f9,stroke:#333,stroke-width:2px,color:#000;
    classDef userNode fill:#f99,stroke:#333,stroke-width:2px,color:#000;
    classDef lineStyle stroke:#333,stroke-width:2px,stroke-dasharray: 5 5;

    class StandbyNode standbyNode;
    class BYOAAgent,MSDEAgent agentNode;
    class CKSAgent cksAgent;
    class BitBucket,NAS,AIServices,JSM,JenkinsQA,UpdateCenter,Artifactory,TrainApp externalNode;


 
