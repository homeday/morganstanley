# Jenkins Architecture Documentation

## Context:

In the current software development landscape, delivering high-quality applications rapidly and efficiently is paramount. CI/CD practices have become essential to achieve this goal, enabling teams to automate the building, testing, and deployment of code changes.​
## Decision:

To implement effective CI/CD pipelines, we have chosen Jenkins as our automation server. Jenkins is an open-source tool renowned for its extensibility and robust plugin ecosystem, offering over 1,000 plugins that significantly extend its core functionality. By leveraging Jenkins, we aim to streamline our development workflows, reduce manual errors, and accelerate our release cycle.​
digital.ai

## System Components

### Jenkins Controller

The Jenkins Controller is the central orchestrator of the CI/CD pipeline. Its responsibilities include:

- **Job Scheduling:** Managing and triggering build jobs based on predefined criteria.
- **Plugin Management:** Extending Jenkins functionalities through various plugins.
- **Resource Allocation:** Assigning tasks to appropriate agents based on availability and capability.

In our setup, the Jenkins Controller comprises:

- **Active Node:** The primary instance handling all operations.
- **Standby Node:** A failover instance ensuring high availability.

### Agents

Agents are responsible for executing real tasks assigned by the Jenkins Master. We have categorized them as:

- **BYOA Agent:** Users can register their own VMs to be as Jenkins agents.
- **MSDE Agent:** The Agents which are provided by MSDE.
- **CKS Pod Agent:** Runs tasks within CKS pods

### External Services

Our Jenkins environment interacts with several external services:

- **BitBucket:** Source code repository triggering builds via webhooks.
- **NAS:** Stores Jenkins controller configure data and logs.
- **Artifactory:** Manages binary artifacts, serving as a repository for build outputs.
- **Jenkins QA:** QA instance for testing plugins and updates.
- **Update Center:** Provides updates and plugins for Jenkins (internet).

### User Interaction

Users interact with the system through:

- **JSM:** Users can open a ticket to add their own VMs as Jenkins Agents (BYOA).
- **Train Application:** Application for creating/modifying/deleting Jenkins jobs.
- **UI Portal:** Users can view the results of their Jenkins jobs and add credentials via it.
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


 
