ADR - 3: Jenkins Permission Control
Status
​[Specify the current status of this decision, e.g., Proposed, Accepted, or Deprecated.]​

Context
After logging in, developers should only be able to operate their own jobs and should not have permission to operate jobs in other projects, such as triggering or stopping builds. This ADR addresses how to achieve this requirement.​

Architecture and High-Level Design
Components
The Role-based Authorization Strategy plugin provides a mechanism to limit control for different users. The plugin offers three types of roles:​
Jenkins Plugins
+3
CloudBees Documentation
+3
wiki.jenkins-ci.org
+3

Global Roles: Assign permissions on a global basis, such as admin, job creator, anonymous, etc., allowing settings for Overall, Agent, Job, Run, View, and SCM permissions.​

Item Roles: Assign item-specific permissions (e.g., Job, Run, or Credentials) on Jobs, Pipelines, and Folders.​

Agent Roles: Assign agent-specific permissions.​
GitHub
+2
Jenkins
+2
javadoc.jenkins.io
+2

Decision
The current Jenkins solution incorporates two of these roles: Global and Item roles.​

Item Role
Jenkins assigns several permissions to item roles:​

Trigger Job: Launching a job by clicking the Build Now button from the UI.​

Cancel Job: Stopping any running jobs.​

View the Workspace of Job: Viewing relevant files in the workspace link on the job page after completion.​

Users need to create a TAC group named tac-<SystemId>-dev-train-rw, which can be associated with a new item role when creating any type of Jenkins job via the Train application. This association grants the permissions listed above. The permission is applied at the folder level, meaning only the jobs under that folder can be operated.​

​Note: The item role also grants users the ability to add credentials, which is covered in another ADR.​

Global Role
The current Jenkins solution defines two global roles:​

Admin Role: Assigned to Jenkins administrators. An ACL group controls membership, eliminating the need to access the UI to add or remove members. This means one ACL group is assigned to the Admin Role.​

Reader Role: Assigned to all authenticated users, with permissions defined accordingly.


Inbound Agent Workflow

sequenceDiagram
    participant Agent as Jenkins Inbound Agent
    participant Controller as Jenkins Controller
    participant Executor as Build Executor
    participant Workspace as Workspace

    Note over Agent, Controller: Agent initiates connection to Controller using agent.jar
    Agent->>Controller: Download agent.jar from Controller
    Agent->>Controller: java -jar agent.jar -jnlpUrl <jnlpUrl> -secret <secret> -workDir <workDir>
    Controller->>Agent: Authentication and Configuration
    Controller->>Agent: Assign Build Task
    Agent->>Executor: Execute Build Task
    Executor->>Workspace: Access Code and Resources
    Executor->>Agent: Report Build Results
    Agent->>Controller: Send Build Results
    Controller->>Agent: Acknowledge Completion
    Note over Agent, Controller: Agent awaits next task or disconnects


 SSH Agent Connection Mechanism 

sequenceDiagram
    participant Controller as Jenkins Controller
    participant Agent as Jenkins SSH Agent
    participant Build as Build Executor
    participant Workspace as Workspace

    Note over Controller, Agent: Controller initiates SSH connection to Agent
    Controller->>Agent: SSH Connection Request
    Agent->>Controller: SSH Authentication
    Controller->>Agent: Transfer remoting.jar
    Controller->>Agent: Launch remoting.jar
    Controller->>Agent: Assign Build Task
    Agent->>Build: Execute Build Task
    Build->>Workspace: Access Code and Resources
    Build->>Agent: Report Build Results
    Agent->>Controller: Send Build Results
    Controller->>Agent: Acknowledge Completion
    Note over Controller, Agent: Agent awaits next task or disconnects


​Involving JSM and AI Self-Service to oversee the addition of Jenkins agents offers several advantages:​

Two-Level Approvals: Implementing a two-level approval process enables ITSO to ensure that all requirements are met and restricts the ability to add agent nodes to Jenkins.​

VM Type Validation: Since Jenkins is currently used for building and deployment to QA environments, it's essential to validate that only non-production VMs are added.​

Audit Trail: This approach provides a clear audit trail, detailing which VMs are added and by whom in the JSM ticket.​

Permission Management: Limiting permissions ensures that users cannot add agents directly from the Jenkins portal, enhancing security.



--------------------------
"​Incorporating AI Self-Service and the Train console application to provide launch arguments and the JAR package enhances security and usability:​

Protection of Sensitive Configuration Data: Jenkins stores a secret text for each agent. When launching the agent, the command must include this secret to authenticate the connection. With AI Self-Service, users do not need to view or store this secret text, thereby reducing the risk of exposure.​

Simplified Launch Commands: By utilizing Train, users can avoid memorizing complex launch commands. Instead, they can execute a straightforward command like train agent launch, streamlining the process.


