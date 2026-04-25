package com.runninghub.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * 静态资源本地化管理器
 * 支持从 URL 下载资源到本地文件系统，并通过 MD5 校验确保资源一致性。
 */
object ResourceCacheManager {
    private const val TAG = "ResourceCacheManager"
    private const val CACHE_DIR = "app_resources"

    // 用于追踪正在下载的 URL，防止并发冲突
    private val downloadingUrls = mutableSetOf<String>()

    /**
     * 获取经过校验的本地资源路径
     * @param context 上下文
     * @param url 资源远程链接
     * @param targetFileName 本地文件名
     * @param md5 预期的 MD5 值（可选，如果不提供则只检查文件是否存在）
     * @return 本地文件的绝对路径
     */
    suspend fun getResourcePath(
        context: Context,
        url: String,
        targetFileName: String,
        md5: String? = null
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, CACHE_DIR).apply { 
            if (!exists()) mkdirs()
        }
        val localFile = File(dir, targetFileName)
        
        var needsDownload = false

        if (!localFile.exists()) {
            needsDownload = true
        } else if (md5 != null) {
            val currentMd5 = calculateFileMd5(localFile)
            if (currentMd5 != md5.lowercase()) {
                needsDownload = true
            }
        }

        if (needsDownload) {
            val shouldDownload = synchronized(downloadingUrls) {
                if (downloadingUrls.contains(url)) {
                    false
                } else {
                    downloadingUrls.add(url)
                    true
                }
            }

            if (shouldDownload) {
                try {
                    Log.d(TAG, "Starting download: $url")
                    downloadResource(url, localFile)
                    Log.i(TAG, "Successfully cached resource: $targetFileName")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download resource from $url", e)
                } finally {
                    synchronized(downloadingUrls) {
                        downloadingUrls.remove(url)
                    }
                }
            } else {
                // 如果已经在下载中，循环等待一会儿或者直接返回现有文件（如果存在）
                var retryCount = 0
                while (synchronized(downloadingUrls) { downloadingUrls.contains(url) } && retryCount < 50) {
                    kotlinx.coroutines.delay(100)
                    retryCount++
                }
            }
        }

        localFile.absolutePath
    }

    private fun downloadResource(url: String, file: File) {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        // Set User-Agent to avoid some basic blocks
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.getInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun calculateFileMd5(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    digest.update(buffer, 0, bytes)
                    bytes = input.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
