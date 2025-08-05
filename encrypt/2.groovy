import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.security.SecureRandom

def encryptWithHMAC(String plaintext, String base64AesKey, String base64HmacKey) {
    byte[] aesKey = Base64.decoder.decode(base64AesKey)
    byte[] hmacKey = Base64.decoder.decode(base64HmacKey)

    // Generate IV
    byte[] iv = new byte[16]
    new SecureRandom().nextBytes(iv)

    // AES encryption
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv))
    byte[] ciphertext = cipher.doFinal(plaintext.bytes)

    // Concatenate IV + ciphertext
    byte[] ivAndCiphertext = iv + ciphertext

    // Compute HMAC over IV + ciphertext
    Mac hmac = Mac.getInstance("HmacSHA256")
    hmac.init(new SecretKeySpec(hmacKey, "HmacSHA256"))
    byte[] hmacBytes = hmac.doFinal(ivAndCiphertext)

    // Final output = IV + ciphertext + HMAC
    byte[] finalPayload = ivAndCiphertext + hmacBytes

    // Return Base64-encoded string
    return Base64.encoder.encodeToString(finalPayload)
}


pipeline {
  agent any
  stages {
    stage('Encrypt with HMAC') {
      steps {
        script {
          withCredentials([
            string(credentialsId: 'aes-encryption-key', variable: 'AES_KEY_B64'),
            string(credentialsId: 'aes-hmac-key', variable: 'HMAC_KEY_B64')
          ]) {
            def plaintext = "my-secret-string"
            def encrypted = yourLib.encryptWithHMAC(plaintext, AES_KEY_B64, HMAC_KEY_B64)
            echo "Encrypted (base64): ${encrypted}"

            writeFile file: 'encrypted.payload.b64', text: encrypted
          }
        }
      }
    }

    stage('Decrypt with openssl (verify HMAC)') {
      steps {
        script {
          withCredentials([
            string(credentialsId: 'aes-encryption-key', variable: 'AES_KEY_B64'),
            string(credentialsId: 'aes-hmac-key', variable: 'HMAC_KEY_B64')
          ]) {
            def aesKeyHex = AES_KEY_B64.decodeBase64().encodeHex().toString()
            def hmacKeyHex = HMAC_KEY_B64.decodeBase64().encodeHex().toString()

            sh '''
              set -e

              # Decode from base64
              base64 -d encrypted.payload.b64 > encrypted.payload.raw

              # Extract IV (first 16 bytes), ciphertext, and HMAC (last 32 bytes)
              head -c 16 encrypted.payload.raw > iv.bin
              FILE_SIZE=$(stat -c%s encrypted.payload.raw)
              let CT_SIZE=FILE_SIZE-16-32

              dd if=encrypted.payload.raw bs=1 skip=16 count=$CT_SIZE of=ciphertext.bin status=none
              tail -c 32 encrypted.payload.raw > hmac.bin

              # Recompute HMAC and compare
              cat iv.bin ciphertext.bin | openssl dgst -sha256 -mac HMAC -macopt hexkey:'${hmacKeyHex}' -binary > hmac.expected
              cmp --silent hmac.expected hmac.bin || (echo "❌ HMAC check failed"; exit 1)

              echo "✅ HMAC verified. Proceeding to decrypt..."

              openssl enc -aes-256-cbc -d -K '${aesKeyHex}' -iv $(xxd -p iv.bin) -in ciphertext.bin
            '''
          }
        }
      }
    }
  }
}
