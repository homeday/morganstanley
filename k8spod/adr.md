ADR Title: Use OpenShift (CKS) to Host Jenkins Agents
Status: Proposed
Date: 2025-07-11
Decision Makers: [List of stakeholders]
Context:
Our CI/CD infrastructure currently relies on static or VM-based Jenkins agents, which:

Require manual lifecycle management

Suffer from inconsistent environments

Do not scale dynamically with build workloads

To improve scalability, manageability, and integration with our Kubernetes-native workflows, we propose to run Jenkins agents as ephemeral pods within OpenShift, particularly using our CKS (Certified Kubernetes Service) environment.

Decision:
We will use OpenShift (CKS) to run Jenkins agents as Kubernetes pods by:

Leveraging Jenkins Kubernetes plugin to dynamically launch agents in response to build demand

Defining agent pod templates in Jenkins to map to specific toolchains or workloads (e.g., Java, Python, Docker builds)

Using OpenShift service accounts and RBAC to securely manage agent permissions

Optionally using OpenShift SCCs (Security Context Constraints) to limit privileges of agent pods

Applying resource limits and auto-scaling policies to manage build efficiency and cost

Considered Alternatives:
Continue using static Jenkins agents on VMs

❌ Not scalable, harder to maintain, non-containerized

Use other Kubernetes platforms (EKS, GKE)

❌ Not aligned with our current enterprise platform choice (OpenShift)

Consequences:
✅ Pros:

Auto-scalable, ephemeral build agents improve resource utilization

Unified security and RBAC management using OpenShift constructs

Easier to standardize agent environments using container images

Aligns with GitOps and container-first strategies

⚠️ Cons:

Requires Jenkins pipeline jobs to adapt to containerized agents

Some pipelines/tools may need extra permissions or tuning to work in restricted SCCs

Slight learning curve for team in debugging Kubernetes-based agents

Implementation Plan:
Configure Jenkins with the Kubernetes plugin

Define and test custom agent pod templates in OpenShift

Integrate with OpenShift RBAC and SCC for secure operation

Update pipeline definitions where necessary

Gradually migrate teams to use the new setup

--------------------------
Decision:
We will use OpenShift to provision Jenkins agents dynamically using the Kubernetes plugin. Agents will be defined as pod templates, either via JCasC or inline pipeline definitions. This enables elastic scalability, better isolation, and native integration with our OpenShift environment.

Additionally, we support workloads that require **Kerberos authentication** by leveraging an `initContainer` within the pod template. This initContainer performs a `kinit` using a keytab file provided securely from Jenkins credentials.

The detailed flow is:

1. **Keytab Injection**:
   - A Base64-encoded keytab is stored securely in Jenkins credentials (as a Secret Text or File credential).
   - When a pod is provisioned, the credential is loaded and passed as an environment variable or mounted into a shared volume.

2. **Kerberos InitContainer**:
   - The initContainer uses a specialized image capable of Kerberos (`kinit`) authentication.
   - It decodes the keytab and runs `kinit` to obtain a Kerberos Ticket Granting Ticket (TGT).
   - The resulting Kerberos cache (e.g., `/tmp/krb5cc_...`) is written to a **shared volume** (e.g., `emptyDir`).

3. **Application Container**:
   - The main application container (running Jenkins job steps) mounts the same shared volume.
   - It inherits the Kerberos ticket and is able to make authenticated calls without re-running `kinit`.

This setup allows secure, automated Kerberos AuthN within OpenShift-based Jenkins agents without manual ticket handling, while respecting credential isolation and lifecycle boundaries.



Security Considerations:

Keytabs are not written to disk directly

Environment variable with Base64 is cleared after use

Cache is isolated to agent pod and deleted after job finishes

Technical Notes:

initContainer and application container must use the same UID/GID for cache access

Shared volume is typically emptyDir

Kerberos cache path must be consistent (KRB5CCNAME=/shared/krb5cc)
