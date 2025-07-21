# ADR: Use OpenShift (CKS) to Host Jenkins Agents

**Status**: Proposed  
**Date**: 2025-07-11  
**Decision Makers**: [List of stakeholders]

## Context

Our CI/CD infrastructure currently relies on VM-based Jenkins agents, which introduces several inefficiencies:

- Users must manually configure their Jenkins agent environments in the host configuration and wait for machine provisioning.
- After a machine is provisioned, additional steps are often required — such as applying custom configurations — to make the node usable as a Jenkins agent.
- Agent VMs may remain idle when not in use, wasting resources.
- The system does not scale dynamically with build workloads.

To improve scalability, manageability, and resource efficiency, we propose running Jenkins agents as **ephemeral pods** within the **OpenShift (CKS)** environment.

## Decision

We will use OpenShift (CKS) to host Jenkins agents as Kubernetes pods by:

- Leveraging the Jenkins Kubernetes plugin to dynamically launch agents in response to build demand.
- Defining standard agent pod images tailored to specific environments (e.g., Java, Python).
- Applying resource limits and auto-scaling policies to optimize resource usage and cost.

Additionally, for workloads requiring **Kerberos authentication**, we will incorporate an `initContainer` in the agent pod template. This container will handle the authentication process by performing a `kinit` using a keytab file securely loaded from Jenkins credentials.

### Kerberos Authentication Flow

1. **Keytab Injection**  
   - A Base64-encoded keytab is stored as a Jenkins "Secret Text" credential.  
   - When a pod is provisioned, the credential is loaded and injected into the initContainer via environment variable.

2. **Kerberos InitContainer**  
   - Uses a specialized image capable of Kerberos (`kinit`) authentication.  
   - Decodes the keytab and runs `kinit` to obtain a Ticket Granting Ticket (TGT).  
   - Writes the Kerberos cache (e.g., `/tmp/krb5cc_...`) into a shared `emptyDir` volume.

3. **Application Container**  
   - The main application container mounts the same shared volume.  
   - It accesses the Kerberos TGT and uses it to perform authenticated operations during job execution.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant Jenkins as Jenkins Master
    participant K8s as Kubernetes Cluster

    Jenkins->>K8s: Request new agent pod from Pod Template
    K8s-->>Jenkins: Acknowledge pod creation

    create participant Pod as Jenkins Agent Pod
    K8s->>Pod: Start agent pod with containers
    activate Pod

    create participant Init as Init Container (Kerberos AuthN)
    Pod->>Init: Start Init Container

    create participant Volume as Shared Volume (TGT)
    Pod->>Volume: Create shared volume for TGT

    Init->>Init: Read keytab from Jenkins secret
    Init->>Volume: Write TGT to shared volume

    destroy Init
    Pod->>Init: Terminate Init Container

    create participant JNLP as JNLP Container
    Pod->>JNLP: Start Jnlp Container

    create participant App as App Container
    Pod->>App: Start App Container

    JNLP<<->>Jenkins: Long-term connection to Jenkins master

    Jenkins->>App: Send job execution instructions
    App->>App: Execute job steps
    App->>Volume: Access TGT for authentication
    App-->>Jenkins: Report job result

    Jenkins->>K8s: Terminate agent pod
    K8s-->>Jenkins: Acknowledge pod termination
    K8s->>Pod: Delete agent pod

    destroy App
    Pod-->>App: Delete App container

    destroy JNLP
    Pod-->>JNLP: Delete JNLP container

    destroy Volume
    Pod-->>Volume: Delete volume

    destroy Pod
    K8s-->>Pod: Delete pod
```
```mermaid
graph

    Jenkins[Jenkins Controller]
    subgraph K8s Cluster
    
    K8s[Kubernetes API Server]

    
    subgraph Pod["Agent Pod (created by Kubernetes)"]
        Init["Init Container<br/>(Kerberos AuthN)"]
        Volume["Shared Volume<br/>(emptyDir for TGT)"]
        JNLP["JNLP Container"]
        App["App Container"]
    end
    end
    Jenkins -->|Create/Delete agent pod| K8s
    
    K8s -->|Creates/Deletes pod & volume| Pod


    Init -->|kinit → writes TGT| Volume
    App -->|Reads TGT| Volume

    JNLP <-->|Long connection| Jenkins
    App <-->|Gets job steps<br/>Reports results| Jenkins

    %% Optional styling for clarity
    style Pod fill:#f9f9f9,stroke:#bbb,stroke-width:1.5px
    style Jenkins fill:#e0f7fa,stroke:#00796b,stroke-width:1px
    style K8s fill:#e8f5e9,stroke:#388e3c,stroke-width:1px
    style Init fill:#fff3e0,stroke:#fb8c00,stroke-width:1px
    style JNLP fill:#ede7f6,stroke:#673ab7,stroke-width:1px
    style App fill:#fce4ec,stroke:#d81b60,stroke-width:1px
    style Volume fill:#eeeeee,stroke:#757575,stroke-width:1px


```




##Consequences
Pros
✅ Elastic Scalability: Jenkins agents can be created and destroyed dynamically, based on actual workload.

✅ Resource Efficiency: Eliminates idle VM costs by using ephemeral containers that terminate after use.

✅ Improved Security and Isolation: Each job runs in a fresh pod with strict resource and security boundaries.

Cons
⚠️ Requires changes to job definitions or shared libraries to adopt pod templates.

⚠️ Complexity in managing secrets (e.g., Kerberos keytab handling) within a containerized environment.

⚠️ Potential initial learning curve for users unfamiliar with Kubernetes-native agent behavior.