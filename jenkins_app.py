from flask import Flask, redirect, url_for, session, request
from authlib.integrations.flask_client import OAuth
from authlib.jose import jwt, JsonWebKey
import os
import requests

app = Flask(__name__)
app.secret_key = os.urandom(24)

# OIDC & Client config
JENKINS_ISSUER = "https://jenkins.example.com/oidc"
CLIENT_ID = "my-python-client"
REDIRECT_URI = "http://localhost:5000/auth"

# Fetch OIDC Discovery & JWKS on startup
DISCOVERY_URL = f"{JENKINS_ISSUER}/.well-known/openid-configuration"
discovery = requests.get(DISCOVERY_URL).json()
JWKS_URI = discovery['jwks_uri']
JWKS = requests.get(JWKS_URI).json()

oauth = OAuth(app)
oauth.register(
    name='jenkins',
    client_id=CLIENT_ID,
    client_secret='',  # Jenkins plugin does not require this
    server_metadata_url=DISCOVERY_URL,
    client_kwargs={'scope': 'openid email profile'}
)

def verify_id_token(id_token, jwks, issuer, client_id):
    key_set = JsonWebKey.import_key_set(jwks)
    claims = jwt.decode(
        id_token,
        key_set,
        claims_options={
            'iss': {'essential': True, 'value': issuer},
            'aud': {'essential': True, 'value': client_id},
            'exp': {'essential': True}
        }
    )
    claims.validate()
    return claims

@app.route('/')
def index():
    user = session.get('user')
    if user:
        return f"Welcome {user['username']} ({user['email']})! <a href='/logout'>Logout</a>"
    else:
        return '<a href="/login">Login with Jenkins OIDC</a>'

@app.route('/login')
def login():
    return oauth.jenkins.authorize_redirect(redirect_uri=REDIRECT_URI)

@app.route('/auth')
def auth():
    token = oauth.jenkins.authorize_access_token()
    id_token = token.get('id_token')
    if not id_token:
        return "ID token missing from response", 400

    try:
        claims = verify_id_token(
            id_token=id_token,
            jwks=JWKS,
            issuer=JENKINS_ISSUER,
            client_id=CLIENT_ID
        )
    except Exception as e:
        return f"Invalid ID token: {str(e)}", 400

    session['user'] = {
        'username': claims.get('preferred_username'),
        'email': claims.get('email')
    }
    return redirect('/')

@app.route('/logout')
def logout():
    session.clear()
    return redirect('/')

if __name__ == '__main__':
    app.run(port=5000, debug=True)



server {
    listen 443 ssl;
    server_name jenkins.example.com;

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    # Enforce only the allowed redirect_uri for security
    location = /oidc/authorize {
        if ($arg_redirect_uri != "http://localhost:5000/auth") {
            return 403;
        }
        proxy_pass http://localhost:8080/oidc/authorize;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Proxy all other oidc endpoints
    location /oidc/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /.well-known/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

Notes:

Replace /etc/nginx/certs/fullchain.pem and privkey.pem with your SSL cert paths (e.g., from Let’s Encrypt).

The if block blocks requests with wrong or unexpected redirect_uri — critical for security.

X-Forwarded-* headers ensure Jenkins knows the original request scheme and IP.


2. Jenkins OIDC Provider Plugin Setup
Step-by-step:
Install the plugin

Go to:
Manage Jenkins → Manage Plugins → Available

Search for OpenID Connect Provider Plugin and install.

Generate RSA Keypair for Signing (if you haven’t already):

bash
Copy
Edit
openssl genpkey -algorithm RSA -out jenkins-oidc-private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in jenkins-oidc-private.pem -out jenkins-oidc-public.pem
Configure the plugin:
Go to
Manage Jenkins → Configure Global Security →
OpenID Connect Provider section:

Check Enable OpenID Connect Provider

Set Issuer URL:
https://jenkins.example.com/oidc

Paste your Private Key (contents of jenkins-oidc-private.pem) into the Signing Key field.

Optionally, set claims like email, preferred_username if desired.

Add your client (Flask app) as a trusted client:

In the OpenID Connect Provider → Clients section:

Add a new client with:

Client ID: my-python-client (or your chosen client id)

Redirect URIs:
http://localhost:5000/auth

Save.

Restart Jenkins if needed to apply.

3. Final Testing
Run your Flask app locally (http://localhost:5000)

Visit the root page, click “Login with Jenkins OIDC”

You should be redirected to Jenkins login

After login, Jenkins issues signed ID token, validated by Flask app

User info is displayed in Flask

