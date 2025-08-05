import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.security.SecureRandom

def encrypt(String plainText, String base64Key) {
    byte[] keyBytes = Base64.decoder.decode(base64Key)
    SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES")

    // IV: 16 random bytes
    byte[] iv = new byte[16]
    new SecureRandom().nextBytes(iv)
    IvParameterSpec ivSpec = new IvParameterSpec(iv)

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"))

    // Return IV + encrypted (both base64 encoded)
    return Base64.encoder.encodeToString(iv + encrypted)
}



@Library('your-shared-lib') _
pipeline {
  agent any
  stages {
    stage('Encrypt') {
      steps {
        script {
          withCredentials([string(credentialsId: 'aes-secret-key', variable: 'AES_KEY_B64')]) {
            def secret = 'sensitive-string'
            def encryptedB64 = yourLib.encrypt(secret, AES_KEY_B64)
            echo "Encrypted (Base64): ${encryptedB64}"

            // Save to file for decryption step
            writeFile file: 'secret.enc.b64', text: encryptedB64
          }
        }
      }
    }

    stage('Decrypt') {
      steps {
        script {
          withCredentials([string(credentialsId: 'aes-secret-key', variable: 'AES_KEY_B64')]) {
            // Decode base64 key to hex for openssl
            def keyHex = AES_KEY_B64.decodeBase64().encodeHex().toString()

            sh """
              ENCRYPTED_B64=\$(cat secret.enc.b64)
              ENCRYPTED_RAW=\$(echo \$ENCRYPTED_B64 | base64 -d)

              # Split IV (first 16 bytes) and ciphertext
              IV_HEX=\$(echo \$ENCRYPTED_RAW | head -c 16 | xxd -p)
              CT_HEX=\$(echo \$ENCRYPTED_RAW | tail -c +17 | xxd -p)

              echo "IV: \$IV_HEX"
              echo "CT: \$CT_HEX"

              echo \$CT_HEX | xxd -r -p > ct.bin
              echo \$IV_HEX | xxd -r -p > iv.bin

              openssl enc -aes-256-cbc -d -K ${keyHex} -iv \$(xxd -p -c 32 iv.bin) -in ct.bin
            """
          }
        }
      }
    }
  }
}

# Sample 32-byte AES key
head -c 32 /dev/urandom | base64 > key.b64
base64 -d key.b64 | xxd -p

# Use shared lib to encrypt (or mimic it in Java code)
# Save result to file


ENCRYPTED_B64=... # from pipeline
echo "$ENCRYPTED_B64" | base64 -d > all.bin
head -c 16 all.bin > iv.bin
tail -c +17 all.bin > ct.bin

openssl enc -aes-256-cbc -d -K <hexkey> -iv <hexiv> -in ct.bin
