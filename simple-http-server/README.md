# Simple HTTP Server

This project is a simple HTTP server implemented in Python. It serves as a basic example of how to set up an HTTP server and handle requests.

## Project Structure

```
simple-http-server
├── src
│   ├── server.py       # Entry point of the HTTP server
│   └── utils.py        # Utility functions for the server
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

## Running the Server

To start the HTTP server, run the following command:
```
python src/server.py
```

The server will start and listen for incoming requests. You can access it at `http://localhost:5000` by default.

## Usage

You can define your routes and handle requests in `server.py`. Utility functions can be added in `utils.py` to assist with various tasks such as logging and data formatting.

## License

This project is open-source and available under the MIT License.