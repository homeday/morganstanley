# Simple HTTP Server

This project is a simple HTTP server implemented in Python. It serves as a basic example of how to set up an HTTP server and handle requests.

## Project Structure

```
simple-http-server
├── src
│   ├── server.py       # Entry point of the HTTP server
│   └── utils.py        # Utility functions for the server
├── config
│   └── config.py       # Configuration file for the server
├── requirements.txt     # Project dependencies
└── README.md            # Project documentation
```

## Requirements

To run this project, you need to have Python installed on your machine. Additionally, you will need to install the required dependencies listed in `requirements.txt`.

## Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   cd simple-http-server
   ```

2. Install the required packages:
   ```
   pip install -r requirements.txt
   ```

## Configuration

The server configuration is located in `config/config.py`. You can modify the following settings:
- `HOST`: The host address to bind the server.
- `PORT`: The port number to listen on.
- `LOG_FILE`: The file where logs will be written.
- `LOG_FORMAT`: The format of the log messages.
- `LOG_LEVEL`: The logging level (e.g., `INFO`, `DEBUG`).

## Running the Server

To start the HTTP server, run the following command:
```
python src/server.py
```

The server will start and listen for incoming requests. You can access it at `http://localhost:5000` by default.

## Logging

Each request is logged with a unique request ID, method, path, headers, and response status. Logs are written to the file specified in `LOG_FILE`.

## Usage

You can define your routes and handle requests in `server.py`. Utility functions can be added in `utils.py` to assist with various tasks such as logging and data formatting.

## Troubleshooting

### Common Issues
- **Missing Dependencies**: Ensure you have installed all required packages using:
  ```bash
  pip install -r requirements.txt
  ```
- **Port Conflicts**: If the default port `5000` is in use, modify the `PORT` setting in `config/config.py`.

### Example Requests
Use `curl` to interact with the server:
- Get the welcome message:
  ```bash
  curl http://localhost:5000/
  ```
- Get sample data:
  ```bash
  curl http://localhost:5000/api/data
  ```
- Get server info:
  ```bash
  curl http://localhost:5000/api/info
  ```

## License

This project is open-source and available under the MIT License.