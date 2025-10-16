Excellent observation 🔍 — you’re right!
When you click “Add a new cloud” in Jenkins UI and hit Save, your browser sends a form submission (POST) to:

/jenkins/cloud/doCreate


This is technically the endpoint that creates a cloud — but there’s a big catch: it’s not a public or stable REST API, it’s a Stapler form submission endpoint (the internal Jenkins web framework).

That means you can emulate it with requests, but you must:

Reproduce the exact form field names,

Include Jenkins’ CSRF crumb,

Include a valid session or API token authentication.

Let’s go step-by-step 👇

🧩 1. What /cloud/doCreate Actually Does

URL: POST /jenkins/cloud/doCreate

Type: multipart/form-data or application/x-www-form-urlencoded

Purpose: Trigger Jenkins’ form-binding code to create a new KubernetesCloud object via the standard Descriptor.newInstance path.

It’s equivalent to hitting “Save” in the “Configure System” page for Clouds.

⚙️ 2. How to Emulate It with requests

Below is a Python script that mimics the UI request.

import requests

jenkins_url = "http://jenkins.example.com"
user = "admin"
token = "your_api_token"

session = requests.Session()
session.auth = (user, token)

# 1️⃣ Get CSRF crumb (needed if CSRF protection is enabled)
crumb_response = session.get(f"{jenkins_url}/crumbIssuer/api/json")
crumb_data = crumb_response.json()
session.headers.update({crumb_data['crumbRequestField']: crumb_data['crumb']})

# 2️⃣ Prepare POST data — these fields must match Jenkins’ expected form parameters
payload = {
    "stapler-class": "org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud",
    "name": "my-k8s-cloud",
    "serverUrl": "https://your-k8s-api-server",
    "namespace": "jenkins",
    "credentialsId": "k8s-cred-id",
    "skipTlsVerify": "false",
    "jenkinsUrl": "",
    "jenkinsTunnel": "",
    "connectTimeout": "0",
    "readTimeout": "0",
    "containerCapStr": "10",
    "maxRequestsPerHostStr": "32",
    "retentionTimeout": "5",
    "usageRestricted": "false",
    "stapler-class-bag": "true",
    "_.add": ""  # necessary when no templates defined
}

# 3️⃣ Send POST to create the cloud
resp = session.post(f"{jenkins_url}/cloud/doCreate", data=payload, allow_redirects=False)

print("Status:", resp.status_code)
print("Response:", resp.text)

🧠 3. How It Works

Jenkins internally maps /cloud/doCreate → hudson.model.Cloud.doCreate() in core,
which uses the Descriptor.newInstance(StaplerRequest, JSONObject) mechanism.
So as long as your POST data includes:

stapler-class (the full Java class name of the cloud)

The correct form field names (matching the Jelly/Stapler descriptors)

Valid crumb/authentication

…Jenkins will instantiate your KubernetesCloud and persist it.

⚠️ 4. Limitations
⚠️ Issue	Explanation
Not official REST API	Internal endpoint — Jenkins may change form field names anytime.
Needs CSRF crumb	Required if CSRF protection is on (default).
Hard to add Pod Templates	Complex nested objects (PodTemplate) require special json form encoding used by Stapler.
Difficult to debug	Jenkins doesn’t return structured JSON; only redirects or HTML error pages.
✅ 5. When Is It Reasonable to Use

This trick is fine for:

Internal automation or bootstrapping Jenkins.

Scripts that mimic UI operations for setup.

But for production automation, using:

/scriptText (Groovy)
or

Configuration-as-Code (JCasC)

is more stable and supported.

If you want, I can:

✅ Capture and explain the exact form fields that /cloud/doCreate expects for the Kubernetes plugin,

Or show a Python version that includes a Pod Template in the POST payload (which uses nested JSON field encoding Jenkins expects).

Would you like me to show that full payload example (with Pod Template included)?

You said:
for my case, i don't need the pod template. i would like to know if using jcasc, how to use the restful to implement it?
ChatGPT said:

Excellent — you’re thinking in the right direction 👏

If your goal is to manage Jenkins Kubernetes Clouds dynamically via REST, but without using the internal /scriptText or /cloud/doCreate endpoints, then the Configuration-as-Code (JCasC) approach is the clean, supported, GitOps-friendly way to do it.

Let’s go over how to do this — entirely with REST API calls using Python + requests.

⚙️ 1. Prerequisites

Make sure you have:

The Configuration as Code plugin (configuration-as-code) installed.

The Kubernetes plugin installed.

Jenkins is running with admin credentials or API token access.

🧩 2. JCasC Endpoint Overview

JCasC exposes REST endpoints under /configuration-as-code/:

Endpoint	Method	Description
/configuration-as-code/view	GET	View current config as YAML
/configuration-as-code/export	GET	Export full current config
/configuration-as-code/reload	POST	Reload from source (usually file)
/configuration-as-code/apply	POST	Apply YAML config directly (💥 dynamic)

👉 The one you need is POST /configuration-as-code/apply.

This lets you send YAML that defines a new cloud (e.g. KubernetesCloud) and Jenkins applies it instantly.

🧾 3. Example JCasC YAML (Cloud only)

Here’s a minimal YAML to define a Kubernetes cloud without PodTemplates:

jenkins:
  clouds:
    - kubernetes:
        name: "my-k8s-cloud"
        serverUrl: "https://your-k8s-api-server"
        namespace: "jenkins"
        credentialsId: "k8s-cred-id"
        skipTlsVerify: false
        containerCapStr: "10"
        retentionTimeout: 5

🐍 4. Python requests example

Here’s how to POST it to Jenkins programmatically:

import requests
import yaml

jenkins_url = "http://jenkins.example.com"
user = "admin"
token = "your_api_token"

session = requests.Session()
session.auth = (user, token)

# Get CSRF crumb
crumb = session.get(f"{jenkins_url}/crumbIssuer/api/json").json()
session.headers.update({crumb["crumbRequestField"]: crumb["crumb"]})

# The JCasC YAML configuration
jcasc_yaml = """
jenkins:
  clouds:
    - kubernetes:
        name: "my-k8s-cloud"
        serverUrl: "https://your-k8s-api-server"
        namespace: "jenkins"
        credentialsId: "k8s-cred-id"
        skipTlsVerify: false
        containerCapStr: "10"
        retentionTimeout: 5
"""

# Send to JCasC apply endpoint
resp = session.post(
    f"{jenkins_url}/configuration-as-code/apply",
    headers={"Content-Type": "application/x-yaml"},
    data=jcasc_yaml.encode("utf-8"),
)

print("Status:", resp.status_code)
print("Response:", resp.text)

✅ 5. If Successful

Jenkins will respond with:

Configuration applied successfully


and you’ll see the new Kubernetes Cloud under
Manage Jenkins → Clouds immediately.

⚠️ 6. Notes and Tips
Case	Explanation
Idempotency	JCasC will replace the existing jenkins.clouds list, not append by default. So if you have multiple clouds, include all of them in the YAML you send.
Credentials	The credentialsId must already exist in Jenkins.
Reapply safely	You can first call /configuration-as-code/export to get current YAML, append yours, and then POST to /apply.
Security	Only Jenkins admins can use this API.
Persistence	The applied config is written into Jenkins’ runtime config (and optionally to disk if configured). It’s not automatically stored in the original YAML file unless you manage that externally.
🧠 7. Recommended Workflow

If your goal is to manage this programmatically or via automation:

Goal	Recommended Action
GitOps-style (versioned config)	Store the JCasC YAML in Git and reload via /configuration-as-code/reload
Ad-hoc REST API automation	Use /configuration-as-code/apply like above
Declarative management	Use Flux / Argo CD to deploy Jenkins with JCasC YAML as part of your infrastructure code

Would you like me to show an idempotent version of the Python script — one that:

Fetches the current config (/export),

Merges a new cloud definition if missing,

Then applies it back?

That would give you a truly “RESTful upsert” behavior for Jenkins clouds.

You said:
if i use api4jenkins, how to change the code? and if i need to add one cloud but there is some other cloud, how to change the code
ChatGPT said:

Perfect — using api4jenkins
 is much cleaner and safer than raw requests calls because it wraps the Jenkins REST API and CSRF handling for you.

Let’s walk through both of your needs step-by-step:

✅ Goal

Use api4jenkins Python library.

Add a new Kubernetes cloud via JCasC REST API (/configuration-as-code/apply).

Keep existing clouds (don’t overwrite them).

⚙️ 1. Install the library
pip install api4jenkins pyyaml

🧩 2. How Jenkins JCasC treats clouds

When you POST YAML to /configuration-as-code/apply,
Jenkins replaces the entire jenkins.clouds list with what you provide.

So, to safely add a new cloud (and preserve existing ones),
you must first:

Fetch the current configuration via /configuration-as-code/export.

Parse the YAML.

Add your new cloud to the jenkins.clouds list (if it doesn’t already exist).

Re-apply the merged config.

🐍 3. Full working example with api4jenkins
from api4jenkins import Jenkins
import yaml

# Jenkins connection
jenkins_url = "http://jenkins.example.com"
user = "admin"
token = "your_api_token"

jenkins = Jenkins(jenkins_url, auth=(user, token))

# 1️⃣ Get current JCasC configuration
export_url = f"{jenkins_url}/configuration-as-code/export"
resp = jenkins.session.get(export_url)
resp.raise_for_status()
config = yaml.safe_load(resp.text)

# 2️⃣ Ensure structure exists
if "jenkins" not in config:
    config["jenkins"] = {}
if "clouds" not in config["jenkins"]:
    config["jenkins"]["clouds"] = []

# 3️⃣ Define the new cloud you want to add
new_cloud = {
    "kubernetes": {
        "name": "my-new-k8s-cloud",
        "serverUrl": "https://your-k8s-api-server",
        "namespace": "jenkins",
        "credentialsId": "k8s-cred-id",
        "skipTlsVerify": False,
        "containerCapStr": "10",
        "retentionTimeout": 5
    }
}

# 4️⃣ Check if this cloud already exists (by name)
existing_clouds = config["jenkins"]["clouds"]
cloud_names = [
    c.get("kubernetes", {}).get("name")
    for c in existing_clouds
    if "kubernetes" in c
]

if new_cloud["kubernetes"]["name"] not in cloud_names:
    existing_clouds.append(new_cloud)
    print(f"Added new cloud: {new_cloud['kubernetes']['name']}")
else:
    print(f"Cloud {new_cloud['kubernetes']['name']} already exists — skipping")

# 5️⃣ Apply the updated configuration via JCasC
apply_url = f"{jenkins_url}/configuration-as-code/apply"
yaml_payload = yaml.dump(config, sort_keys=False)

resp = jenkins.session.post(
    apply_url,
    data=yaml_payload.encode("utf-8"),
    headers={"Content-Type": "application/x-yaml"},
)
resp.raise_for_status()

print("✅ Configuration applied successfully")

🧠 4. What This Script Does
Step	Description
1	Fetches your current JCasC YAML config from /configuration-as-code/export.
2	Parses it safely into a Python dict.
3	Defines a new Kubernetes cloud (no PodTemplates).
4	Checks if the same cloud name already exists (idempotent).
5	Re-applies the merged YAML to Jenkins — preserving other clouds.
⚙️ 5. Example Result

If you had two existing clouds and added one new:

jenkins:
  clouds:
    - kubernetes:
        name: "old-cloud"
        ...
    - kubernetes:
        name: "staging-cloud"
        ...
    - kubernetes:
        name: "my-new-k8s-cloud"
        serverUrl: "https://your-k8s-api-server"
        namespace: "jenkins"
        credentialsId: "k8s-cred-id"
        skipTlsVerify: false
        containerCapStr: "10"
        retentionTimeout: 5


All clouds remain intact 👌

⚠️ 6. Tips

api4jenkins handles auth and CSRF crumb automatically.

Use jenkins.session for custom REST calls not directly wrapped by the library.

You can use the same method to modify credentials, nodes, etc. via JCasC.

Would you like me to extend this example so that it can also update a cloud (e.g., if serverUrl or credentialsId changes)?
That would make it a fully idempotent upsert REST automation.

You said:
if using api4jenkins to implment the approach about /cloud/doCreate. how to do it
ChatGPT said:

Excellent — now you’re asking how to use api4jenkins to emulate the UI’s /cloud/doCreate endpoint — i.e. directly creating a Jenkins Kubernetes Cloud as if you clicked “Add Cloud” in the UI, rather than via JCasC.

That’s totally possible — but since /cloud/doCreate isn’t part of Jenkins’ official REST API, you’ll need to use the jenkins.session object from api4jenkins to manually post the correct form fields (just like you’d do with requests).

The benefit is:
✅ You still get authentication + crumb handling automatically from api4jenkins.

Let’s walk through a working version.

🧩 Background: /cloud/doCreate

Endpoint: POST /jenkins/cloud/doCreate

Expected form field:

stapler-class: full class name of the cloud implementation

other fields (name, serverUrl, namespace, etc.) are form-bound

Response: 302 redirect (success) or HTML error.

So we’ll replicate that form post.

✅ Python code using api4jenkins
from api4jenkins import Jenkins

jenkins_url = "http://jenkins.example.com"
user = "admin"
token = "your_api_token"

# Connect to Jenkins (handles auth + crumb)
jenkins = Jenkins(jenkins_url, auth=(user, token))

# Define form fields for the Kubernetes cloud
data = {
    "stapler-class": "org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud",
    "name": "my-k8s-cloud",
    "serverUrl": "https://your-k8s-api-server",
    "namespace": "jenkins",
    "credentialsId": "k8s-cred-id",
    "skipTlsVerify": "false",
    "jenkinsUrl": "",
    "jenkinsTunnel": "",
    "containerCapStr": "10",
    "maxRequestsPerHostStr": "32",
    "retentionTimeout": "5",
    "usageRestricted": "false",
    "stapler-class-bag": "true",
    "_.add": "",  # needed if no pod templates are provided
}

# POST to the /cloud/doCreate endpoint
resp = jenkins.session.post(f"{jenkins_url}/cloud/doCreate", data=data, allow_redirects=False)

print("Status:", resp.status_code)
print("Location:", resp.headers.get("Location"))
print("Response snippet:", resp.text[:300])

🧠 Explanation
Part	Purpose
jenkins.session.post()	Uses api4jenkins’s built-in authenticated session (crumb included).
stapler-class	The key that tells Jenkins which cloud class to instantiate.
Field names	Must match the Jelly form input names in the KubernetesCloud configuration UI.
allow_redirects=False	Stops requests from following Jenkins’ redirect, so you can inspect the HTTP status directly (expect 302 if success).
✅ Expected Response

If successful:

Status: 302
Location: /jenkins/configure


If something’s wrong (missing plugin, wrong class name, bad crumb, etc.), you’ll get:

Status: 400 or 500
Response snippet: <html><head><title>Error</title>...

⚠️ Limitations
Limitation	Description
Not official API	This endpoint is an internal Stapler form handler.
Fragile	Field names may change between plugin versions.
Limited	You can’t easily configure nested objects like PodTemplates this way.
Non-idempotent	Re