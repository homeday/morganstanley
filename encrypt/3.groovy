import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.Base64

// GCM requires 12-byte IV and 16-byte tag
def encryptWithAESGCM(String plaintext, String base64Key) {
    byte[] key = Base64.decoder.decode(base64Key)

    // Generate 12-byte IV
    byte[] iv = new byte[12]
    new SecureRandom().nextBytes(iv)

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding")
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES")
    GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv) // 128-bit tag

    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
    byte[] ciphertextWithTag = cipher.doFinal(plaintext.bytes)

    // GCM output = ciphertext + tag (last 16 bytes of ciphertextWithTag)
    byte[] finalOutput = iv + ciphertextWithTag

    return Base64.encoder.encodeToString(finalOutput)
}

pipeline {
  agent any
  stages {
    stage('Encrypt') {
      steps {
        script {
          withCredentials([string(credentialsId: 'aes-gcm-key', variable: 'AES_KEY_B64')]) {
            def plaintext = "top-secret-data"
            def encrypted = yourLib.encryptWithAESGCM(plaintext, AES_KEY_B64)

            echo "Encrypted (base64): ${encrypted}"

            writeFile file: 'gcm_encrypted.b64', text: encrypted
          }
        }
      }
    }

    stage('Decrypt with openssl') {
      steps {
        script {
          withCredentials([string(credentialsId: 'aes-gcm-key', variable: 'AES_KEY_B64')]) {
            def keyHex = AES_KEY_B64.decodeBase64().encodeHex().toString()

            sh """
              set -e

              base64 -d gcm_encrypted.b64 > encrypted.raw

              # Extract IV (first 12 bytes), then ciphertext, then tag (last 16 bytes)
              head -c 12 encrypted.raw > iv.bin
              FILE_SIZE=\$(stat -c%s encrypted.raw)
              let CT_TAG_SIZE=FILE_SIZE-12
              dd if=encrypted.raw bs=1 skip=12 count=\$CT_TAG_SIZE of=ct_tag.bin status=none

              # Separate tag (last 16 bytes) and ciphertext
              let CT_SIZE=CT_TAG_SIZE-16
              dd if=ct_tag.bin bs=1 count=\$CT_SIZE of=ciphertext.bin status=none
              tail -c 16 ct_tag.bin > tag.bin

              # Decrypt
              openssl enc -d -aes-256-gcm \\
                -K '${keyHex}' \\
                -iv \$(xxd -p iv.bin | tr -d '\\n') \\
                -in ciphertext.bin \\
                -out decrypted.txt \\
                -nosalt \\
                -p \\
                -tag \$(xxd -p tag.bin | tr -d '\\n')

              echo "Decrypted:"
              cat decrypted.txt
            """
          }
        }
      }
    }
  }
}
apiVersion: v1
kind: Pod
metadata:
  name: jenkins-agent
spec:
  volumes:
    - name: workdir
      emptyDir: {}
    - name: aes-secret
      secret:
        secretName: jenkins-aes-key

  initContainers:
    - name: decrypt-init
      image: alpine:latest
      command: ["/bin/sh", "-c"]
      args:
        - |
          set -e

          AES_KEY_HEX=$(cat /secrets/aes-key.b64 | base64 -d | xxd -p -c 256)

          base64 -d /workspace/gcm_encrypted.b64 > /workspace/encrypted.raw

          head -c 12 /workspace/encrypted.raw > /workspace/iv.bin
          SIZE=$(stat -c %s /workspace/encrypted.raw)
          CT_TAG_SIZE=$((SIZE - 12))
          dd if=/workspace/encrypted.raw bs=1 skip=12 count=$CT_TAG_SIZE of=/workspace/ct_tag.bin status=none

          CT_SIZE=$((CT_TAG_SIZE - 16))
          dd if=/workspace/ct_tag.bin bs=1 count=$CT_SIZE of=/workspace/ciphertext.bin status=none
          tail -c 16 /workspace/ct_tag.bin > /workspace/tag.bin

          openssl enc -d -aes-256-gcm \
            -K "$AES_KEY_HEX" \
            -iv $(xxd -p /workspace/iv.bin | tr -d '\n') \
            -in /workspace/ciphertext.bin \
            -out /workspace/decrypted.txt \
            -nosalt \
            -p \
            -tag $(xxd -p /workspace/tag.bin | tr -d '\n')

      volumeMounts:
        - name: aes-secret
          mountPath: /secrets
        - name: workdir
          mountPath: /workspace

  containers:
    - name: jenkins-agent
      image: jenkins/inbound-agent
      volumeMounts:
        - name: workdir
          mountPath: /workspace
