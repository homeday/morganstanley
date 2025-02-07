from flask import Flask, request, jsonify, make_response
import requests
import redis
import time

app = Flask(__name__)

# Configure Redis client
redis_client = redis.StrictRedis(host='localhost', port=6379, db=0, decode_responses=True)
LOCK_TIMEOUT = 10  # Timeout for the lock in seconds

def get_token():
    token = redis_client.get('k8s_token')
    
    if not token:
        # Attempt to acquire lock
        if redis_client.setnx('token_lock', 'locked'):
            try:
                redis_client.expire('token_lock', LOCK_TIMEOUT)
                
                # Fetch the token from the portal
                response = requests.get('https://portal.example.com/token', cert=('/path/to/client.crt', '/path/to/client.key'), verify='/path/to/ca.crt')
                token = response.json().get('token')
                
                # Cache the token with an expiration time of 1 day (86400 seconds)
                redis_client.set('k8s_token', token, ex=86400)
            finally:
                # Release the lock
                redis_client.delete('token_lock')
        else:
            # Wait for the other process to set the token
            time.sleep(1)
            return get_token()
    
    return token

@app.route('/proxy', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH'])
def proxy():
    token = get_token()
    
    # Forward the request to the Kubernetes API server
    kubernetes_api_url = f'https://your_k8s_api_server{request.full_path}'
    headers = {key: value for key, value in request.headers if key != 'Host'}
    headers['Authorization'] = f'Bearer {token}'
    
    # Handle various HTTP methods
    if request.method == 'GET':
        response = requests.get(kubernetes_api_url, headers=headers, verify='/path/to/ca.crt')
    elif request.method == 'POST':
        response = requests.post(kubernetes_api_url, headers=headers, json=request.json, verify='/path/to/ca.crt')
    elif request.method == 'PUT':
        response = requests.put(kubernetes_api_url, headers=headers, json=request.json, verify='/path/to/ca.crt')
    elif request.method == 'DELETE':
        response = requests.delete(kubernetes_api_url, headers=headers, verify='/path/to/ca.crt')
    elif request.method == 'PATCH':
        response = requests.patch(kubernetes_api_url, headers=headers, json=request.json, verify='/path/to/ca.crt')

    # Create a response object to return
    proxy_response = make_response(response.content, response.status_code)
    for key, value in response.headers.items():
        proxy_response.headers[key] = value
    
    return proxy_response

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, ssl_context=('/path/to/server.crt', '/path/to/server.key'))




import click

@click.command()
@click.option('--fields', '-f', help='Fields in the format key1=value1,key2=value2')
def main(fields):
    fields_dict = {}

    if fields:
        for field in fields.split(','):
            if '=' in field:
                key, value = field.split('=', 1)
                fields_dict[key] = value
            else:
                click.echo(f"Invalid format for field: {field}")
                return

    click.echo("Parsed fields:")
    for key, value in fields_dict.items():
        click.echo(f'{key}: {value}')

if __name__ == "__main__":
    main()


# python your_script.py --fields name=John,age=30,city=LosAngeles


import click

@click.command()
@click.option('--field', '-f', multiple=True, help='Field in the format key=value')
def main(field):
    fields_dict = {}

    for f in field:
        if '=' in f:
            key, value = f.split('=', 1)
            fields_dict[key] = value
        else:
            click.echo(f"Invalid format for field: {f}")
            return

    click.echo("Parsed fields:")
    for key, value in fields_dict.items():
        click.echo(f'{key}: {value}')

if __name__ == "__main__":
    main()


# python your_script.py --field name=John --field age=30 --field city=LosAngeles
# https://plugins.jenkins.io/hashicorp-vault-plugin/