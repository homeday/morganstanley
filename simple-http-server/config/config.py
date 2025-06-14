import os
import yaml

class Config:
    # Load configuration from YAML file
    with open(os.getenv('CONFIG_FILE', 'config/config.yaml'), 'r') as file:
        config = yaml.safe_load(file)

    # Server configuration
    HOST = config['server']['host']
    PORT = config['server']['port']

    # Logging configuration
    LOG_FILE = config['logging']['file']
    LOG_FORMAT = config['logging']['format']
    LOG_LEVEL = config['logging']['level']
