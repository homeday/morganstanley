# ADR: Implementing Jenkins High Availability with F5 Load Balancer

## 1. Title

Implementing Jenkins High Availability with F5 Load Balancer

## 2. Status

Accepted

## 3. Context

Ensuring continuous availability of Jenkins is critical for our development and deployment pipelines. To mitigate risks associated with single points of failure, we require a High Availability (HA) solution that provides failover capabilities and minimizes downtime. However, due to Jenkins' architecture, certain constraints must be considered when designing an HA solution.

## 4.Decision:

We will implement an Active/Standby High Availability (HA) configuration for Jenkins with the following components:​

- Active Node:

Hosts the primary Jenkins instance, with the JENKINS_HOME directory located on its local disk.​

Utilizes the ThinBackup plugin to perform regular backups of critical configurations and job data to a Network Attached Storage (NAS).​

- Standby Node:

Hosts a secondary Jenkins installation with the JENKINS_HOME directory on its local disk.​

The Jenkins service remains inactive under normal operations.​

In the event of a failure detected through external monitoring tools or manual observation, an operator will:​

Manually copy the latest backup from the NAS to the Standby Node's local JENKINS_HOME.

Start the Jenkins service on the Standby Node.

- F5 Load Balancer:

Manages incoming user traffic, directing requests to the Active Node during normal operations.​

Upon manual intervention indicating an Active Node failure, traffic can be redirected to the Standby Node.​

- Network Attached Storage (NAS):

Serves as the centralized repository for Jenkins backups.​

Ensures that the Standby Node has access to the most recent configurations and job data for restoration in case of Active Node failure.

## 5. Rationale

This architecture offers several advantages:

- **High Availability:** Automatic failover to the Standby node ensures minimal service disruption.

- **Data Consistency:** Utilizing shared storage maintains uniform data across nodes.

- **Manageability:** The F5 Load Balancer provides centralized control over traffic distribution and health monitoring.

## 6. Constraints and Considerations

- **Active/Active Mode Limitations:** Implementing an Active/Active High Availability (HA) configuration for Jenkins is not feasible due to inherent limitations in its architecture. Jenkins relies heavily on file-based storage for its configurations and logs. In an Active/Active setup, where multiple Jenkins instances operate concurrently, simultaneous read and write operations to these files can lead to conflicts. For example, one node might write changes to a configuration file that another node cannot immediately detect, potentially resulting in data corruption or inconsistent states across the instances. Therefore, an Active/Passive (Active/Standby) configuration is recommended to ensure data integrity and system stability.

- **Startup Time:** Initiating the Jenkins service on the Standby node requires additional time, which may result in a brief service interruption during failover. The duration of this startup process depends on the size and complexity of the Jenkins instance.

## 7. Implementation Plan

- **Configure F5 Load Balancer:** Set up virtual servers and pools to manage traffic and perform health checks on Jenkins nodes.

- **Set Up Shared Storage:** Ensure both nodes have access to the NAS for `JENKINS_HOME`.

- **Deploy Jenkins Nodes:** Install Jenkins on both nodes, configuring one as Active and the other as Standby.

- **Implement Monitoring Scripts:** Develop scripts on the Standby node to monitor the Active node's status and initiate failover procedures when necessary.

- **Testing:** Conduct failover tests to validate the HA setup and ensure seamless transitions between nodes.

## 8. Diagram

To visualize the Jenkins HA setup with the F5 Load Balancer, refer to the following diagram:

```plantuml
@startuml
title Jenkins HA with F5 Load Balancer

actor User
node BYOA

rectangle "F5 Load Balancer" as F5 #MintCream;line:Orange;text:MidnightBlue {
    node "Virtual Server" as VS #MintCream;line:Orange;text:MidnightBlue
    note right of VS
    - Two nodes are configured as a pool with Active/Standby mode
    - Use a configure file in the NAS to member two nodes' role.
    - Active node is the default target for all requests.
    - Standby node is used if the active node is down.
    end note
}

node "Jenkins Active Node" as Active #palegreen;line:green;text:green

node "Jenkins Standby Node" as Standby #aliceblue;line:blue;line.dotted;text:blue {
    rectangle service #aliceblue;line:blue;text:blue [
        By default, the Jenkins service is stopped.
    ]
    rectangle Script #aliceblue;line:blue;text:blue [
        Run a script via crontab.
        - Checks active node status.
        - If down, starts Jenkins on standby (unless in maintenance).
        - Switches standby/active in a configure file in the NAS.
        - Logs status for review.
    ]
}

folder "NAS (JENKINS_HOME)" as NAS #linen;line:navy;text:navy

User --> VS : Access with https/443
BYOA --> VS : Connect with Tcp/7788

VS --> Active #green;text:green : Https/443 and Tcp/7788\nrequest by default
F5 --> Active #Orange;text:Orange : Health Check
VS --> Standby #blue;line.dotted;text:blue : Https/443 and Tcp/7788 requests\nif active node is down
F5 --> Standby #Orange;text:Orange : Health Check

Script --> Active #blue;text:blue : Monitor Jenkins service status
note right of Active
The monitor script in both two machines
end note
Active -- NAS #navy;text:navy : Read/Write
Standby -- NAS #navy;text:navy : Read/Write

@enduml


Implementing an Active/Standby High Availability (HA) configuration for Jenkins using an F5 Load Balancer and a script on the Standby Node to monitor the Active Node's Jenkins instance.​

In the event of a failover, the system would switch to the Active/Standby HA configuration and initiate the Jenkins service on the Standby Node. Additionally, configuration and log data would be shared via Network Attached Storage (NAS) to ensure consistency between the two nodes.