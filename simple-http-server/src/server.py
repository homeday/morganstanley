import logging
import uuid
from flask import Flask, jsonify, request, g, redirect, url_for, session
from authlib.integrations.flask_client import OAuth
import os
import json
from config.config import Config  # Import the configuration

# Configure logging using settings from the configuration file
logging.basicConfig(filename=Config.LOG_FILE, level=getattr(logging, Config.LOG_LEVEL), 
                    format=Config.LOG_FORMAT)

app = Flask(__name__)
app.secret_key = os.environ.get("FLASK_SECRET_KEY", "default-secret-key")

# OIDC client configuration
OIDC_CLIENT_ID = os.environ.get("OIDC_CLIENT_ID", "jenkins-client-id")
OIDC_CLIENT_SECRET = os.environ.get("OIDC_CLIENT_SECRET", "jenkins-client-secret")
JENKINS_ISSUER = os.environ.get("JENKINS_ISSUER", "https://your-jenkins-url")
REDIRECT_URI = os.environ.get("REDIRECT_URI", "http://localhost:5000/callback")

# Initialize OAuth
oauth = OAuth(app)
oauth.register(
    name='jenkins',
    client_id=OIDC_CLIENT_ID,
    client_secret=OIDC_CLIENT_SECRET,
    server_metadata_url=f'{JENKINS_ISSUER}/.well-known/openid-configuration',
    client_kwargs={'scope': 'openid email profile'}
)

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

@app.route('/login')
def login():
    redirect_uri = url_for('callback', _external=True)
    return oauth.jenkins.authorize_redirect(redirect_uri)

@app.route('/callback')
def callback():
    token = oauth.jenkins.authorize_access_token()
    userinfo = oauth.jenkins.parse_id_token(token)
    session['user'] = userinfo
    return redirect('/')

@app.route('/logout')
def logout():
    session.clear()
    return redirect('/')

@app.route('/')
def home():
    user = session.get('user')
    if user:
        return f"<h1>Logged in</h1><pre>{json.dumps(user, indent=2)}</pre>"
    return '<a href="/login">Login with Jenkins OIDC</a>'

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
