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


