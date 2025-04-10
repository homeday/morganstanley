# Jenkins Credential Management

## Status

## Context
Build jobs running on Jenkins may require credentials to access resources such as Artifactory for downloading files. These credentials should not be hardcoded in scripts or stored locally on machines. This ADR covers the Jenkins solution for handling credentials securely.

## Components

### Jenkins Credential Storage
Jenkins provides a robust system to securely store sensitive information. Internally, there are two scopes for credentials:
- **System/Global-Level Credentials:** Accessible across all jobs.
- **Job/Folder-Level Credentials:** Restricted to the specific project folder or job.

### Vault
Credentials can also be stored in an external Vault. MSDE provides a shared library to access Vault and retrieve credentials as needed.

## Storage Location

- **System-Level Credentials:**  
    Stored in the main Jenkins home directory, typically within the credentials.xml file located at $JENKINS_HOME/credentials.xml. This file contains the encrypted secrets used by Jenkins for system-wide operations. These credentials are managed by the MSDE team; users do not have permission to access them in their Jenkinsfiles.

- **Job/Folder-Level Credentials:**  
    Credentials specific to jobs or folders are embedded within their respective config.xml files, ensuring that only the jobs or folders that require access can utilize them. Jenkins employs the Role-Based Authorization Strategy (RBAC) plugin to manage folder permissions. This allows users to create, modify, or delete credentials within a folder if they are the member of the relevant TAC group. ​

- **Secrets Directory:**  
  Additional sensitive data and master keys are stored in the `$JENKINS_HOME/secrets/` directory. This includes critical files such as `master.key`, which is used for encryption and decryption. The Jenkins home directory is located on a NAS, and ACL groups are used to restrict directory permissions. Currently, only one system ID has access.

## Encryption Mechanism

- Jenkins uses the Advanced Encryption Standard (AES) to encrypt and protect secrets, credentials, and their respective encryption keys. These keys are stored in the `$JENKINS_HOME/secrets/` directory along with the master key that safeguards them.

- **Algorithm:**  
  The encryption process is implemented using AES, a symmetric encryption method. This approach ensures that even if the configuration files are exposed, the sensitive data remains protected because decryption requires the corresponding master key.

- **Key Storage:**  
  The actual encryption key is stored in the `master.key` file within the `$JENKINS_HOME/secrets/` directory. This master key is essential for decrypting credentials when they are needed by the system or plugins.

- **Encryption at Rest:**  
  Credentials are stored in an encrypted form in XML configuration files, ensuring that secret data is not stored in plaintext on disk, which enhances overall security.



## Consequence
This credential management approach enables secure, centralized handling of sensitive information across Jenkins jobs and infrastructure. By storing credentials within Jenkins or integrating with Vault, teams can access secrets safely without embedding them in scripts or exposing them in source control.

The use of job- and folder-level scopes allows fine-grained access control, ensuring that only authorized jobs or users can access specific credentials. Leveraging RBAC for permission management further strengthens this control by aligning credential visibility with TAC group memberships.

Encryption at rest, backed by AES and a securely stored master key, provides a strong safeguard against unauthorized access to stored secrets. The use of a NAS with ACL restrictions ensures that physical storage of credentials and encryption keys is tightly controlled.

This model improves both security and compliance posture, while also simplifying secret distribution and rotation processes. It reduces the risk of credential leakage and supports secure automation across environments, empowering teams to build and deploy with confidence.