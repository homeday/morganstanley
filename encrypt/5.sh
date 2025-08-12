#!/bin/bash

# 增强版 keytab 解密脚本
# 解决空字节和换行问题
# ./decrypt_keytab.sh \
#                             "$ENCRYPTED_KEYTAB" \
#                             "$ENC_KEY" \
#                             "$HMAC_KEY" \
#                             "decrypted.keytab"

# 输入参数
ENCRYPTED_RESULT="$1"
ENC_KEY="$2"
HMAC_KEY="$3"
OUTPUT_KEYTAB="${4:-decrypted.keytab}"  # 默认输出文件名
KEY_FORMAT="${5:-raw}"

# 分割输入
IV_BASE64=$(echo "$ENCRYPTED_RESULT" | cut -d'|' -f1)
CIPHERTEXT_BASE64=$(echo "$ENCRYPTED_RESULT" | cut -d'|' -f2)
HMAC_BASE64=$(echo "$ENCRYPTED_RESULT" | cut -d'|' -f3)

# 创建临时文件（避免命令替换中的变量）
TEMP_DIR=$(mktemp -d)
TEMP_IV_BIN="${TEMP_DIR}/iv.bin"
TEMP_CIPHERTEXT_BIN="${TEMP_DIR}/ciphertext.bin"
TEMP_HMAC_BIN="${TEMP_DIR}/hmac.bin"
TEMP_AUTH_DATA="${TEMP_DIR}/auth_data.bin"
TEMP_DECRYPTED_BASE64="${TEMP_DIR}/decrypted.base64"

# 始终清理临时文件
cleanup() {
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

# 解码 Base64 到文件（避免变量中的空字节）
echo -n "$IV_BASE64" | base64 -d > "$TEMP_IV_BIN"
echo -n "$CIPHERTEXT_BASE64" | base64 -d > "$TEMP_CIPHERTEXT_BIN"
echo -n "$HMAC_BASE64" | base64 -d > "$TEMP_HMAC_BIN"

# 处理密钥格式
if [ "$KEY_FORMAT" = "base64" ]; then
    ENC_KEY=$(echo -n "$ENC_KEY" | base64 -d)
    HMAC_KEY=$(echo -n "$HMAC_KEY" | base64 -d)
fi

# 创建认证数据 (IV + Ciphertext)
cat "$TEMP_IV_BIN" "$TEMP_CIPHERTEXT_BIN" > "$TEMP_AUTH_DATA"

# 计算 HMAC
CALC_HMAC_BIN=$(openssl dgst -sha256 -hmac "$HMAC_KEY" -binary "$TEMP_AUTH_DATA")

# 安全比较 HMAC（使用文件比较）
if ! cmp -s "$TEMP_HMAC_BIN" <(echo -n "$CALC_HMAC_BIN"); then
    echo "ERROR: HMAC 验证失败! 数据可能被篡改" >&2
    exit 1
fi

# 准备密钥和IV的十六进制
to_hex_file() {
    file="$1"
    hex=""
    while IFS= read -r -n 1 char; do
        printf -v hex_char "%02x" "'$char"
        hex="${hex}${hex_char}"
    done < "$file"
    echo -n "$hex"
}

IV_HEX=$(to_hex_file "$TEMP_IV_BIN")
ENC_KEY_HEX=$(to_hex "$ENC_KEY")

# 解密到文件（直接处理二进制）
openssl enc -d -aes-256-cbc \
    -K "$ENC_KEY_HEX" \
    -iv "$IV_HEX" \
    -in "$TEMP_CIPHERTEXT_BIN" \
    -out "$TEMP_DECRYPTED_BASE64"

# if [ $? -ne 0 ]; then
#     echo "ERROR: 解密失败" >&2
#     exit 1
# fi

# 解码Base64得到原始keytab
base64 -d "$TEMP_DECRYPTED_BASE64" > "$OUTPUT_KEYTAB"

# 验证keytab文件
if [ ! -s "$OUTPUT_KEYTAB" ]; then
    echo "ERROR: 输出文件为空或无效" >&2
    exit 1
fi

echo "Keytab 解密成功: $OUTPUT_KEYTAB"


hex_to_bin() {
    local hex="$1"
    local outfile="$2"
    # 移除任何非十六进制字符
    hex=$(echo "$hex" | tr -cd '[:xdigit:]')
    # 确保长度为偶数
    if [ $(( ${#hex} % 2 )) -ne 0 ]; then
        hex="0$hex"
    fi
    # 转换
    rest="$hex"
    while [ -n "$rest" ]; do
        byte="${rest:0:2}"
        rest="${rest:2}"
        printf "\\x$byte" >> "$outfile"
    done
}



#!/bin/bash

# 增强版 keytab 解密脚本（十六进制输入）
# 解决空字节和换行问题

# 输入参数
ENCRYPTED_HEX_RESULT="$1"
ENC_KEY="$2"
HMAC_KEY="$3"
OUTPUT_KEYTAB="${4:-decrypted.keytab}"  # 默认输出文件名
KEY_FORMAT="${5:-raw}"  # raw 或 base64

# 分割输入
IV_HEX=$(echo "$ENCRYPTED_HEX_RESULT" | cut -d'|' -f1)
ENCRYPTED_HEX=$(echo "$ENCRYPTED_HEX_RESULT" | cut -d'|' -f2)
HMAC_HEX=$(echo "$ENCRYPTED_HEX_RESULT" | cut -d'|' -f3)

# 处理密钥格式
if [ "$KEY_FORMAT" = "base64" ]; then
    ENC_KEY=$(echo -n "$ENC_KEY" | base64 -d)
    HMAC_KEY=$(echo -n "$HMAC_KEY" | base64 -d)
fi

# 创建临时目录
TEMP_DIR=$(mktemp -d)
trap "rm -rf '$TEMP_DIR'" EXIT

# 十六进制转二进制函数
hex_to_bin() {
    local hex="$1"
    local outfile="$2"
    
    # 使用 xxd 如果可用（高效）
    if command -v xxd &> /dev/null; then
        echo -n "$hex" | xxd -r -p > "$outfile"
    else
        # 纯 Bash 实现（适用于较小文件）
        rest="$hex"
        while [ -n "$rest" ]; do
            byte="${rest:0:2}"
            rest="${rest:2}"
            printf "\\x$byte" >> "$outfile"
        done
    fi
}

# 转换各部分为二进制文件
hex_to_bin "$IV_HEX" "$TEMP_DIR/iv.bin"
hex_to_bin "$ENCRYPTED_HEX" "$TEMP_DIR/encrypted.bin"
hex_to_bin "$HMAC_HEX" "$TEMP_DIR/hmac.bin"

# 创建认证数据 (IV + Ciphertext)
cat "$TEMP_DIR/iv.bin" "$TEMP_DIR/encrypted.bin" > "$TEMP_DIR/auth_data.bin"

# 计算 HMAC
CALC_HMAC_BIN=$(openssl dgst -sha256 -hmac "$HMAC_KEY" -binary "$TEMP_DIR/auth_data.bin")
echo -n "$CALC_HMAC_BIN" > "$TEMP_DIR/calc_hmac.bin"

# 安全比较 HMAC
if ! cmp -s "$TEMP_DIR/hmac.bin" "$TEMP_DIR/calc_hmac.bin"; then
    echo "ERROR: HMAC 验证失败! 数据可能被篡改" >&2
    exit 1
fi

# 解密数据
openssl enc -d -aes-256-cbc \
    -K $(echo -n "$ENC_KEY" | od -A n -t x1 | tr -d ' \n') \
    -iv $(cat "$TEMP_DIR/iv.bin" | od -A n -t x1 | tr -d ' \n') \
    -in "$TEMP_DIR/encrypted.bin" \
    -out "$OUTPUT_KEYTAB"

# 检查解密结果
if [ $? -ne 0 ] || [ ! -s "$OUTPUT_KEYTAB" ]; then
    echo "ERROR: 解密失败或输出为空" >&2
    exit 1
fi

echo "Keytab 解密成功: $OUTPUT_KEYTAB"


def encryptKeytabToHex(String keytabPath, String encKey, String hmacKey, boolean keysAreBase64 = false) {
    def base64 = org.apache.commons.codec.binary.Base64
    def random = new java.security.SecureRandom()
    
    try {
        // 读取 keytab 文件为字节数组
        byte[] keytabBytes = new File(keytabPath).bytes
        
        // 生成随机 IV
        byte[] ivBytes = new byte[16]
        random.nextBytes(ivBytes)
        
        // 处理密钥
        byte[] encKeyBytes
        if (keysAreBase64) {
            encKeyBytes = base64.decodeBase64(encKey)
        } else {
            encKeyBytes = encKey.getBytes("UTF-8")
        }
        
        byte[] hmacKeyBytes
        if (keysAreBase64) {
            hmacKeyBytes = base64.decodeBase64(hmacKey)
        } else {
            hmacKeyBytes = hmacKey.getBytes("UTF-8")
        }
        
        // 验证密钥长度
        if (encKeyBytes.length != 16 && encKeyBytes.length != 24 && encKeyBytes.length != 32) {
            error "无效的加密密钥长度: ${encKeyBytes.length} 字节 (需要 16, 24 或 32 字节)"
        }
        
        // AES 加密
        def ivSpec = new javax.crypto.spec.IvParameterSpec(ivBytes)
        def keySpec = new javax.crypto.spec.SecretKeySpec(encKeyBytes, "AES")
        def cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        byte[] encrypted = cipher.doFinal(keytabBytes)
        
        // 创建认证数据 (IV + Ciphertext)
        ByteArrayOutputStream authData = new ByteArrayOutputStream()
        authData.write(ivBytes)
        authData.write(encrypted)
        
        // 计算 HMAC
        def mac = javax.crypto.Mac.getInstance("HmacSHA256")
        def hmacKeySpec = new javax.crypto.spec.SecretKeySpec(hmacKeyBytes, "HmacSHA256")
        mac.init(hmacKeySpec)
        byte[] hmacBytes = mac.doFinal(authData.toByteArray())
        
        // 转换为十六进制字符串
        String ivHex = ivBytes.collect { String.format("%02X", it) }.join('')
        String encryptedHex = encrypted.collect { String.format("%02X", it) }.join('')
        String hmacHex = hmacBytes.collect { String.format("%02X", it) }.join('')
        
        // 返回格式：IV_HEX|ENCRYPTED_HEX|HMAC_HEX
        return "${ivHex}|${encryptedHex}|${hmacHex}"
    } catch (Exception e) {
        error "Keytab 加密失败: ${e.toString()}"
    }
}