def encryptWithHMAC(String plainText, String encKey, String hmacKey) {
    def base64 = org.apache.commons.codec.binary.Base64
    def random = new java.security.SecureRandom()
    
    // 生成随机 IV
    byte[] ivBytes = new byte[16]
    random.nextBytes(ivBytes)
    def ivBase64 = base64.encodeBase64String(ivBytes)
    
    try {
        // AES 加密
        def ivSpec = new javax.crypto.spec.IvParameterSpec(ivBytes)
        def keySpec = new javax.crypto.spec.SecretKeySpec(encKey.getBytes("UTF-8"), "AES")
        def cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"))
        def encryptedBase64 = base64.encodeBase64String(encrypted)
        
        // 创建包含 IV 和密文的数据用于 HMAC
        ByteArrayOutputStream dataToAuthenticate = new ByteArrayOutputStream()
        dataToAuthenticate.write(ivBytes)
        dataToAuthenticate.write(encrypted)
        byte[] authenticationData = dataToAuthenticate.toByteArray()
        
        // 计算 HMAC（基于 IV + Ciphertext）
        def mac = javax.crypto.Mac.getInstance("HmacSHA256")
        def hmacKeySpec = new javax.crypto.spec.SecretKeySpec(hmacKey.getBytes("UTF-8"), "HmacSHA256")
        mac.init(hmacKeySpec)
        byte[] hmacBytes = mac.doFinal(authenticationData)
        def hmacBase64 = base64.encodeBase64String(hmacBytes)
        
        // 返回格式：IV|CIPHERTEXT|HMAC
        return "${ivBase64}|${encryptedBase64}|${hmacBase64}"
    } catch (Exception e) {
        error "加密失败: ${e.toString()}"
    }
}

#!/bin/bash

# 增强版解密脚本
# 输入: ENCRYPTED_RESULT, ENC_KEY, HMAC_KEY

# 分割输入
IV_BASE64=$(echo "$1" | cut -d'|' -f1)
CIPHERTEXT_BASE64=$(echo "$1" | cut -d'|' -f2)
HMAC_BASE64=$(echo "$1" | cut -d'|' -f3)

# 解码 Base64
IV_BIN=$(echo -n "$IV_BASE64" | base64 -d)
CIPHERTEXT_BIN=$(echo -n "$CIPHERTEXT_BASE64" | base64 -d)
HMAC_BIN=$(echo -n "$HMAC_BASE64" | base64 -d)

# 创建临时文件存放认证数据
TEMP_AUTH_DATA=$(mktemp)
echo -n "${IV_BIN}${CIPHERTEXT_BIN}" > "$TEMP_AUTH_DATA"

# 计算 HMAC (基于 IV + Ciphertext)
CALC_HMAC_BIN=$(openssl dgst -sha256 -hmac "$HMAC_KEY" -binary "$TEMP_AUTH_DATA")
rm "$TEMP_AUTH_DATA"

# 安全比较 HMAC (避免时序攻击)
if ! cmp -s <(echo -n "$HMAC_BIN") <(echo -n "$CALC_HMAC_BIN"); then
    echo "ERROR: HMAC 验证失败! 数据可能被篡改" >&2
    exit 1
fi

# 解密数据
PLAINTEXT=$(echo -n "$CIPHERTEXT_BIN" | openssl enc -d -aes-256-cbc \
    -K $(echo -n "$ENC_KEY" | od -A n -t x1 | tr -d ' \n') \
    -iv $(echo -n "$IV_BIN" | od -A n -t x1 | tr -d ' \n'))

echo "解密成功: $PLAINTEXT"

---------------------------------------------
#!/bin/bash

# Replace these with your actual values
KEY_HEX="603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4"

# ENCRYPTED_HEX includes IV + ciphertext + HMAC concatenated as hex string
ENCRYPTED_HEX="a4f4dfe19a92ff9d1578d30dff421f90904f9ff2d13c0ff050c602dbf3a36522b828f62b8c7ceea56790be9e4fd2d76d8d0e8ecf0c5d64b1dfdb179309b9e1b1735fd94d3d6c4e657f10dd4e5c142b6a"

# Function: Convert hex string to binary
hex_to_bin() {
    echo "$1" | tr -d '\n ' | sed 's/../\\x&/g' | xargs printf "%b"
}

# Constants for lengths in hex chars
IV_LEN=32      # 16 bytes * 2 hex chars
HMAC_LEN=64    # 32 bytes * 2 hex chars

# Extract IV (first 32 hex chars)
IV_HEX="${ENCRYPTED_HEX:0:$IV_LEN}"

# Extract HMAC (last 64 hex chars)
HMAC_HEX="${ENCRYPTED_HEX: -$HMAC_LEN}"

# Extract ciphertext (middle)
CT_HEX="${ENCRYPTED_HEX:$IV_LEN:$((${#ENCRYPTED_HEX} - $IV_LEN - $HMAC_LEN))}"

echo "IV: $IV_HEX"
echo "HMAC: $HMAC_HEX"
echo "Ciphertext: $CT_HEX"

# Verify HMAC over IV + ciphertext
iv_ct_bin=$( (hex_to_bin "$IV_HEX"; hex_to_bin "$CT_HEX") )

computed_hmac=$(echo -n "$iv_ct_bin" | openssl dgst -sha256 -mac HMAC -macopt hexkey:$KEY_HEX | awk '{print $2}')

if [ "$computed_hmac" != "$HMAC_HEX" ]; then
    echo "❌ HMAC verification failed!"
    exit 1
else
    echo "✅ HMAC verified."
fi

# Decrypt ciphertext
echo -n "$(hex_to_bin "$CT_HEX")" | openssl enc -d -aes-256-cbc -K "$KEY_HEX" -iv "$IV_HEX" -nosalt
