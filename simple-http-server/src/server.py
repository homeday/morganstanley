from flask import Flask, jsonify

app = Flask(__name__)

@app.route('/')
def home():
    return jsonify(message="Welcome to the Simple HTTP Server!")

@app.route('/api/data')
def get_data():
    data = {"key": "value", "another_key": "another_value"}
    return jsonify(data)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)