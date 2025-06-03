import logging
import uuid
from flask import Flask, jsonify, request, g
from config.config import Config  # Import the configuration

# Configure logging using settings from the configuration file
logging.basicConfig(filename=Config.LOG_FILE, level=getattr(logging, Config.LOG_LEVEL), 
                    format=Config.LOG_FORMAT)

app = Flask(__name__)

def log_message(message):
    """Utility function for logging messages."""
    logging.info(message)

@app.before_request
def log_request():
    g.request_id = str(uuid.uuid4())  # Generate a unique request ID
    headers = dict(request.headers)  # Convert headers to a dictionary for logging
    log_message(f"Request ID: {g.request_id} - Incoming request: {request.method} {request.path} - Headers: {headers}")

@app.after_request
def log_response(response):
    log_message(f"Request ID: {g.request_id} - Response status: {response.status_code} for {request.method} {request.path}")
    return response

@app.errorhandler(404)
def handle_404_error(e):
    log_message(f"Request ID: {g.request_id} - 404 Error: Resource not found for {request.path}")
    return jsonify(error="Resource not found", status=404), 404

@app.route('/')
def home():
    return jsonify(message="Welcome to the Simple HTTP Server!")

@app.route('/api/data')
def get_data():
    data = {"key": "value", "another_key": "another_value"}
    return jsonify(data)

@app.route('/api/info')
def get_info():
    info = {"server_name": "Simple HTTP Server", "version": "1.0.0"}
    return jsonify(info)

@app.route('/<path:unmatched_path>', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH'])
def handle_unmatched_routes(unmatched_path):
    return jsonify(error="Route not found", path=unmatched_path, status=404), 404

if __name__ == '__main__':
    app.run(debug=True, host=Config.HOST, port=Config.PORT)


# from flask import Flask, redirect, url_for, session, request
# from authlib.integrations.flask_client import OAuth
# import os
# import json

# app = Flask(__name__)
# app.secret_key = os.environ.get("FLASK_SECRET", "supersecret")

# # OIDC client config (replace with your Jenkins values)
# OIDC_CLIENT_ID = 'jenkins-client-id'
# OIDC_CLIENT_SECRET = 'jenkins-client-secret'
# JENKINS_ISSUER = 'https://your-jenkins-url'  # No trailing slash
# REDIRECT_URI = 'http://localhost:5000/callback'

# oauth = OAuth(app)

# oauth.register(
#     name='jenkins',
#     client_id=OIDC_CLIENT_ID,
#     client_secret=OIDC_CLIENT_SECRET,
#     server_metadata_url=f'{JENKINS_ISSUER}/.well-known/openid-configuration',
#     client_kwargs={
#         'scope': 'openid email profile'
#     }
# )

# @app.route('/')
# def index():
#     user = session.get('user')
#     if user:
#         return f"<h1>Logged in</h1><pre>{json.dumps(user, indent=2)}</pre>"
#     return '<a href="/login">Login with Jenkins OIDC</a>'

# @app.route('/login')
# def login():
#     redirect_uri = url_for('callback', _external=True)
#     return oauth.jenkins.authorize_redirect(redirect_uri)

# @app.route('/callback')
# def callback():
#     token = oauth.jenkins.authorize_access_token()
#     userinfo = oauth.jenkins.parse_id_token(token)
#     session['user'] = userinfo
#     return redirect('/')

# @app.route('/logout')
# def logout():
#     session.clear()
#     return redirect('/')

# if __name__ == '__main__':
#     app.run(debug=True)

# ✅ Jenkins OIDC Setup (Recap)
# On Jenkins (OIDC Provider Plugin):

# Register a client:

# Client ID: jenkins-client-id

# Client Secret: jenkins-client-secret

# Redirect URI: http://localhost:5000/callback

# Ensure .well-known/openid-configuration is available.

# Users must be logged in to Jenkins for this to work.


# from flask import Flask, redirect, url_for, session, request
# from authlib.integrations.flask_client import OAuth
# from authlib.jose import jwt, JsonWebKey
# import os
# import json
# import requests

# app = Flask(__name__)
# app.secret_key = os.environ.get("FLASK_SECRET", "supersecret")

# # ----------------------
# # 🔧 Configuration
# # ----------------------
# OIDC_CLIENT_ID = 'jenkins-client-id'
# OIDC_CLIENT_SECRET = 'jenkins-client-secret'
# JENKINS_ISSUER = 'https://your-jenkins-url'  # e.g., https://jenkins.example.com
# REDIRECT_URI = 'http://localhost:5000/callback'

# oauth = OAuth(app)

# oauth.register(
#     name='jenkins',
#     client_id=OIDC_CLIENT_ID,
#     client_secret=OIDC_CLIENT_SECRET,
#     server_metadata_url=f'{JENKINS_ISSUER}/.well-known/openid-configuration',
#     client_kwargs={
#         'scope': 'openid email profile'
#     }
# )

# # ----------------------
# # 🔐 Manual Token Validation
# # ----------------------
# def validate_id_token(token):
#     # Fetch OIDC discovery and JWKS
#     discovery = requests.get(f"{JENKINS_ISSUER}/.well-known/openid-configuration").json()
#     jwks_uri = discovery['jwks_uri']
#     jwks = requests.get(jwks_uri).json()
    
#     # Decode & verify token
#     claims = jwt.decode(token, JsonWebKey.import_key_set(jwks), claims_options={
#         "iss": {"values": [JENKINS_ISSUER]},
#         "aud": {"values": [OIDC_CLIENT_ID]},
#     })
#     claims.validate()
#     return claims

# # ----------------------
# # 🌐 Routes
# # ----------------------
# @app.route('/')
# def index():
#     user = session.get('user')
#     if user:
#         groups = user.get('groups', [])
#         return f"""
#             <h1>Logged in</h1>
#             <p><strong>Username:</strong> {user.get('sub')}</p>
#             <p><strong>Email:</strong> {user.get('email')}</p>
#             <p><strong>Groups:</strong> {groups}</p>
#             <h3>Raw Token:</h3>
#             <pre>{json.dumps(user, indent=2)}</pre>
#             <p><a href="/logout">Logout</a></p>
#         """
#     return '<a href="/login">Login with Jenkins OIDC</a>'

# @app.route('/login')
# def login():
#     redirect_uri = url_for('callback', _external=True)
#     return oauth.jenkins.authorize_redirect(redirect_uri)

# @app.route('/callback')
# def callback():
#     token = oauth.jenkins.authorize_access_token()
#     id_token = token.get('id_token')

#     try:
#         # ✅ Validate signature and claims manually
#         userinfo = validate_id_token(id_token)
#         session['user'] = userinfo
#         return redirect('/')
#     except Exception as e:
#         return f"<h2>OIDC Validation Error</h2><pre>{str(e)}</pre>", 400

# @app.route('/logout')
# def logout():
#     session.clear()
#     return redirect('/')

# if __name__ == '__main__':
#     app.run(debug=True)
