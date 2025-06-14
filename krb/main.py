from requests_gssapi import HTTPSPNEGOAuth
import requests

# 确保参数正确
params = {
    'client_id': 'openshift-browser-client',
    'idp': 'pf-cide',
    'redirect_uri': f'{token_endpoint}/display',  # 注意是 uri 不是 url
    'response_type': 'code'
}

# 显式指定SPN和认证选项
auth = HTTPSPNEGOAuth(
    mutual_authentication="OPTIONAL",
    target_name="HTTP@oauth-openshift.apps.csbqms3.cks.ms.com.cn"
)

response = requests.get(
    authorization_endpoint,
    params=params,
    auth=auth,
    verify='/etc/pki/cls/certs/ca-bundle.crt',
    allow_redirects=True
)


from requests_gssapi import HTTPSPNEGOAuth
import requests

response = requests.get(
    url=authorization_endpoint,
    params={
        'client_id': 'openshift-browser-client',
        'idp': 'pf-cide',
        'redirect_uri': f'{token_endpoint}/display',  # 注意是 redirect_uri
        'response_type': 'code'
    },
    verify='/etc/pki/cls/certs/ca-bundle.crt',
    auth=HTTPSPNEGOAuth(
        mutual_authentication="OPTIONAL",  # 或 "REQUIRED", "DISABLED"
        target_name="HTTP@oauth-openshift.apps.csbqms3.cks.ms.com.cn"  # 显式指定SPN
    )
)


from requests_oauthlib import OAuth2Session
from requests_gssapi import HTTPSPNEGOAuth

oauth = OAuth2Session(
    client_id='openshift-browser-client',
    scope=['openid']
)

# 先使用 Kerberos 获取授权码
auth_response = oauth.authorization_url(
    authorization_endpoint,
    idp='pf-cide',
    redirect_uri=f'{token_endpoint}/display'
)[0]  # 返回授权URL

# 使用 Kerberos 认证获取授权码
code_response = requests.get(
    auth_response,
    auth=HTTPSPNEGOAuth(),
    verify='/etc/pki/cls/certs/ca-bundle.crt',
    allow_redirects=False
)


import gssapi
import requests

def get_spnego_token(host):
    service_name = gssapi.Name(f"HTTP@{host}", gssapi.NameType.hostbased_service)
    ctx = gssapi.SecurityContext(name=service_name, usage="initiate")
    token = ctx.step()  # 生成令牌
    return token.decode('utf-8')

# 使用令牌发起请求
headers = {
    "Authorization": f"Negotiate {get_spnego_token('oauth-openshift.apps.csbqms3.cks.ms.com.cn')}"
}

response = requests.get(
    authorization_endpoint,
    params={...},
    headers=headers,
    verify='/etc/pki/cls/certs/ca-bundle.crt'
)


import requests
from requests.adapters import HTTPAdapter
from requests_gssapi import HTTPSPNEGOAuth
from urllib.parse import urljoin


class SPNEGORedirectAdapter(HTTPAdapter):
    """Custom HTTPAdapter that re-applies SPNEGO auth on redirects."""
    def send(self, request, **kwargs):
        response = super().send(request, **kwargs)

        # Follow redirects manually and reapply auth
        redirect_count = 0
        max_redirects = kwargs.get("max_redirects", 5)

        while response.is_redirect and redirect_count < max_redirects:
            redirect_url = response.headers["location"]
            redirect_url = urljoin(response.url, redirect_url)  # Handle relative redirects

            # Build a new request for the redirect
            new_request = requests.Request(
                method="GET",
                url=redirect_url,
                headers=request.headers,
                cookies=response.cookies
            ).prepare()

            # Update cookies and send again
            response = super().send(new_request, **kwargs)
            redirect_count += 1

        return response


# Setup session
session = requests.Session()
session.verify = '/etc/pki/tls/certs/ca-bundle.crt'
session.auth = HTTPSPNEGOAuth(opportunistic_auth=True)

# Mount the adapter for HTTPS so it uses our redirect logic
session.mount("https://", SPNEGORedirectAdapter())

# Make the initial request (redirects will be handled internally)
response = session.get(
    "https://oauth-openshift.apps.cshgms3.cks.ms.com.cn/oauth/authorize",
    params={
        "client_id": "openshift-browser-client",
        "idp": "pf-oidc",
        "redirect_url": "https://your.redirect.url/display",
        "response_type": "code"
    }
)

# Output final result
print("✅ Final response code:", response.status_code)
print("✅ Final URL:", response.url)
print("✅ Body snippet:", response.text[:500])



import os
from contextlib import contextmanager

@contextmanager
def temporary_env_var(key, value):
    old_value = os.environ.get(key)
    os.environ[key] = value
    try:
        yield
    finally:
        if old_value is None:
            del os.environ[key]
        else:
            os.environ[key] = old_value


import subprocess
import base64
import tempfile
import os

def refresh_kerberos_ticket(keytab_path=None, keytab_b64=None, principal=None, ccache=None):
    if keytab_path is None and keytab_b64 is None:
        raise ValueError("Either keytab_path or keytab_b64 must be provided")

    # If base64 string is provided, decode and write to temp file
    if keytab_b64:
        with tempfile.NamedTemporaryFile(delete=False) as temp_keytab:
            temp_keytab.write(base64.b64decode(keytab_b64))
            temp_keytab_path = temp_keytab.name
    else:
        temp_keytab_path = keytab_path

    try:
        cmd = ['kinit', '-k', '-t', temp_keytab_path]
        if ccache:
            cmd += ['-c', ccache]
        cmd.append(principal)

        subprocess.run(cmd, check=True, capture_output=True)
        print("Kerberos ticket refreshed successfully.")
    except subprocess.CalledProcessError as e:
        print("Failed to refresh Kerberos ticket:")
        print(e.stderr.decode())
    finally:
        # Clean up temporary keytab file if created
        if keytab_b64:
            try:
                os.remove(temp_keytab_path)
            except OSError:
                pass


from api4jenkins import Jenkins

jenkins = Jenkins('http://jenkins-url', auth=('user', 'api_token'))
cred_config_url = 'credentials/store/system/domain/_/credential/my-cred-id/config.xml'

resp = jenkins.raw_session.get(cred_config_url)
print(resp.text)  # XML config (password/secret hidden)

resp = jenkins.raw_session.post(
    cred_config_url,
    headers={'Content-Type': 'application/xml'},
    data=updated_xml_string
)
print(resp.status_code)


<org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl>
  <scope>GLOBAL</scope>
  <id>my-secret-text-id</id>
  <description>Updated secret description</description>
  <secret>newSecretTextValue</secret>
</org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl>


apiVersion: security.openshift.io/v1
kind: SecurityContextConstraints
metadata:
  name: jenkins-agent-scc
allowPrivilegedContainer: false
allowHostNetwork: false
allowHostPorts: false
allowHostPID: false
allowHostIPC: false
allowHostDirVolumePlugin: false
readOnlyRootFilesystem: false
runAsUser:
  type: MustRunAsRange
seLinuxContext:
  type: MustRunAs
fsGroup:
  type: MustRunAs
supplementalGroups:
  type: RunAsAny
volumes:
  - configMap
  - emptyDir
  - secret
  - projected
  - persistentVolumeClaim
users:
  - system:serviceaccount:jenkins:jenkins-agent-sa