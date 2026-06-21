package com.runninghub.core.common

/**
 * 计算字符串的 MD5 摘要。
 *
 * 当前仅用于兼容 RunningHub 用户中心密码登录接口的历史协议：密码在提交前需要
 * 使用 MD5 摘要传输。该函数不应被视为通用安全散列能力；新的敏感数据存储或签名流程
 * 必须使用更合适的平台安全 API。
 *
 * @param input 待计算摘要的原始字符串，按 UTF-8 字节参与计算。
 * @return 32 位小写十六进制 MD5 字符串。
 */
expect fun md5(input: String): String
