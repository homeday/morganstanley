def log_request(method, path):
    print(f"Received {method} request for: {path}")

def format_response(data, status_code=200):
    return {
        "statusCode": status_code,
        "body": data
    }