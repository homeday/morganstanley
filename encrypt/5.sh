#!/bin/bash

# 增强版 keytab 解密脚本
# 解决空字节和换行问题

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