#!/bin/bash

PEM_FILE=your_concatenated.pem
KEYSTORE=your_keystore.jks
PASSWORD=your_keystore_password

# Count the number of certificates in the PEM file
CERT_COUNT=$(grep -c 'END CERTIFICATE' $PEM_FILE)

# Loop through each certificate and import it
for ((i=0; i<$CERT_COUNT; i++)); do
    ALIAS="cert_$i"
    awk "n==$i { print }; /END CERTIFICATE/ { n++ }" $PEM_FILE | \
    keytool -importcert -alias $ALIAS -keystore $KEYSTORE -storepass $PASSWORD -noprompt
done
