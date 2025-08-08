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