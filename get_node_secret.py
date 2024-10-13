import hmac
import hashlib

# jenkins.model.Jenkins.getInstance().getComputer("NODE_NAME").getJnlpMac()


# Replace with the actual path to your Jenkins secret key
secret_key_path = '/jenkins_home/secrets/hudson.util.Secret'

# Read the secret key
with open(secret_key_path, 'rb') as f:
    secret_key = f.read()

# Node name for which you want to generate the secret
node_name = 'wsl_agent_lunar'

# Generate HMAC
hmac_generator = hmac.new(secret_key, node_name.encode('utf-8'), hashlib.sha256)
node_secret = hmac_generator.hexdigest()

print(f'The generated secret for node {node_name} is: {node_secret}')
