#!/bin/bash

# Path to the Java cacerts file
CACERTS_PATH="$JAVA_HOME/lib/security/cacerts"
# Password for the keystore (default is 'changeit')
STOREPASS="changeit"

# Directory to save the exported certificates
OUTPUT_DIR="./exported_certs"
mkdir -p $OUTPUT_DIR

# List all aliases in the cacerts file
aliases=$(keytool -list -keystore $CACERTS_PATH -storepass $STOREPASS | grep 'alias name:' | awk '{print $3}')

# Loop through each alias and export the certificate
for alias in $aliases; do
  echo "Exporting certificate for alias: $alias"
  keytool -exportcert -alias $alias -file "$OUTPUT_DIR/$alias.pem" -keystore $CACERTS_PATH -storepass $STOREPASS -rfc
done

echo "All certificates have been exported to $OUTPUT_DIR"






java -Djavax.net.debug=ssl -jar your-application.jar
java -Djavax.net.ssl.trustStore=/path/to/truststore -Djavax.net.ssl.trustStorePassword=password -jar your-application.jar

java -Djavax.net.ssl.trustStore=/etc/ssl/certs/mytruststore.jks -Djavax.net.ssl.trustStorePassword=myPassword -jar myApplication.jar
keytool -list -v -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
java -Djavax.net.ssl.trustStore=/etc/ssl/certs/ca-certificates.crt -Djavax.net.ssl.trustStoreType=PEM -Djavax.net.ssl.trustStorePassword=changeit -jar your-application.jar


Debian-based systems (like Ubuntu): /etc/ssl/certs/ca-certificates.crt
Red Hat-based systems (like CentOS): /etc/pki/tls/certs/ca-bundle.crt


#
sudo keytool -importcert -keystore $JAVA_HOME/lib/security/cacerts -file /path/to/your/rootCA.pem -alias yourAlias

import requests

response = requests.get("https://example.com", verify="/etc/ssl/certs/ca-certificates.crt")
print(response.status_code)

import requests
import certifi

response = requests.get("https://example.com", verify=certifi.where())


export REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt


import pip._vendor.requests as requests
import certifi

# Check the CA bundle location
print("Requests CA Bundle: ", requests.certs.where())
print("Certifi CA Bundle: ", certifi.where())


pip debug --verbose


sudo keytool -importcert -keystore $JAVA_HOME/lib/security/cacerts -file /path/to/your/rootCA.pem -alias yourAlias

# Supported Formats
# PEM (Privacy-Enhanced Mail):

# Text-based format

# Commonly uses .pem, .crt, .cer, or .key file extensions

# Can contain multiple certificates and private keys

# DER (Distinguished Encoding Rules):

# Binary format

# Commonly uses .der or .cer file extensions

#openssl x509 -in rootCA.der -inform der -out rootCA.pem -outform pem


# import pem ca
sudo keytool -importcert -alias myrootca -file /path/to/rootCA.pem -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

# import der ca
sudo keytool -importcert -alias myrootca -file /path/to/rootCA.der -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -storetype JKS

openssl x509 -in /path/to/your/certificate.pem -text -noout

openssl x509 -in /path/to/your/certificate.der -inform der -text -noout

# Microsoft Edge:
# Navigate to the Portal: Open the HTTPS portal in Edge.

# View Certificate: Click on the padlock icon in the address bar, then click on "Certificate (Valid)".

# Certificate Hierarchy: In the "Certificate" window, you'll see a hierarchy of certificates.

# Select Root CA: Navigate to the top of the hierarchy to find the root CA certificate.

# Details Tab: Switch to the "Details" tab.

# Export Certificate: Click on "Copy to File..." and follow the wizard to export the certificate. Choose the appropriate format (usually PEM or DER).


# Steps to Export in PEM Format:
# When exporting the root CA from your browser, make sure to select the PEM format (usually offered as "Base-64 encoded" or similar options in the export dialog).

# Once you have the root CA certificate in PEM format, you can proceed to import it into the Java trust store as previously discussed.

# If you have any further questions or need more assistance, feel free to ask!