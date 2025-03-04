===============================
Saving Credentials in a Jenkins Folder
===============================

1. Navigate to Your Project Folder
-----------------------------------
After creating a Jenkins job, a corresponding folder will be available at:

   ``https://jenkins/job/folder/``

2. Access the Credentials Section
-----------------------------------
To store credentials in Jenkins:

#. Open Jenkins and go to your project folder.
#. In the left sidebar, click **"Credentials"**.
#. Click on the **folder button** to open the credentials for this specific folder.
#. Click **"Global credentials (unrestricted)"**.
#. Click **"(+) Add Credentials"** to store a new credential.

3. Adding a New Credential
---------------------------
When adding a credential, fill in the following details:

- **Kind**: Choose from:
  
  - *Username and password* (for authentication)
  - *Secret text* (for API keys or tokens)
  - *Secret file* (for configuration files, certificates, etc.)

- **Scope**: Select **Global (Jenkins, nodes, items, all child items)** so it is available within the folder.
- **ID**: *(Optional)* Provide a unique identifier.
- **Description**: Add a meaningful note.
- Click **"OK"** or **"Save"** to store the credential.

4. Using Stored Credentials in a Jenkins Pipeline
-------------------------------------------------
Once stored, credentials can be accessed securely in your pipeline.

4.1 Using Username and Password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
To use a **Username and Password** in a pipeline:

.. code-block:: groovy

    pipeline {
        agent any
        stages {
            stage('Use Credentials') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'my-credentials-id', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh 'curl -u $USER:$PASS https://secure.example.com'
                    }
                }
            }
        }
    }

4.2 Using Secret Text
^^^^^^^^^^^^^^^^^^^^^
To use a **Secret Text** (such as an API key):

.. code-block:: groovy

    pipeline {
        agent any
        environment {
            API_KEY = credentials('api-key-id')
        }
        stages {
            stage('Use API Key') {
                steps {
                    script {
                        echo "Using API Key safely"
                        sh 'curl -H "Authorization: Bearer $API_KEY" https://api.example.com'
                    }
                }
            }
        }
    }

4.3 Using a Secret File
^^^^^^^^^^^^^^^^^^^^^^^
To use a **Secret File** (such as a configuration file or certificate):

.. code-block:: groovy

    pipeline {
        agent any
        stages {
            stage('Use Secret File') {
                steps {
                    withCredentials([file(credentialsId: 'secret-file-id', variable: 'SECRET_FILE')]) {
                        sh 'cat $SECRET_FILE'
                        sh 'cp $SECRET_FILE /tmp/config.json'
                    }
                }
            }
        }
    }

