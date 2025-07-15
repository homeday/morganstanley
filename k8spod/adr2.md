## Decision

We will use OpenShift to dynamically provision Jenkins agent pods using the Kubernetes plugin, allowing scalability, isolation, and native integration with OpenShift. Agent configurations will be defined via JCasC and/or pipeline-level pod templates.

Additionally, to support workloads that require Kerberos authentication, the following design pattern will be used within the agent pod:

### Kerberos Authentication Flow via Init Container

Some jobs require Kerberos authentication. To support this securely within Jenkins agent pods, we adopt the following approach:

- A **Base64-encoded keytab file** is stored as a Jenkins credential (Secret Text or File).
- The pod template includes an **`initContainer`** which:
  - Reads the keytab from an environment variable or secret mount.
  - Runs `kinit` using a specialized Kerberos-enabled image.
  - Stores the resulting **TGT cache** in a **shared `emptyDir` volume**.
- The **main application container** shares the same volume, so it can access the Kerberos ticket without managing authentication directly.

This pattern ensures the ticket is available for secure operations (e.g., `curl --negotiate` or `hdfs dfs`) in the application container.

---

### Components Involved

- **Jenkins Master**: Orchestrates job execution and provisions agent pods.
- **Kubernetes Cluster (OpenShift)**: Manages pod lifecycle.
- **Jenkins Agent Pod**: A dynamic pod created per job, composed of:
  - `initContainer` for Kerberos setup
  - JNLP container (Jenkins connectivity)
  - App container (job logic)
  - Shared volume (`emptyDir`) for TGT

---

### 🔄 Lifecycle Flow

#### Pod Provisioning
- Jenkins requests a pod using a defined template.
- Kubernetes acknowledges and begins creating the pod.

#### Authentication Setup
- Init container:
  - Decodes keytab from Jenkins credentials
  - Runs `kinit`
  - Writes TGT to shared volume
- Init container then terminates.

#### Main Container Start
- JNLP connects to Jenkins master
- App container starts and uses the TGT from shared volume

#### Job Execution
- Jenkins sends job steps to the app container
- App uses the Kerberos ticket for secure operations

#### Teardown
- Jenkins signals the pod to terminate
- Kubernetes deletes all containers and the pod itself

---

### 📈 Mermaid Sequence Diagram

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
    JNLP<<->>Jenkins: Long term connection to Jenkins master
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
    Pod-->>Volume: Delete Volume
    destroy Pod
    K8s-->>Pod: Delete Pod
