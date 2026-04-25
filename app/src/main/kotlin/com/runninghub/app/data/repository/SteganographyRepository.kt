package com.runninghub.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.xor

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SteganographyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Magic header to identify our hidden data
    // Python struct: i (4 bytes) -> Magic? No, let's look at the python code logic concept.
    // Actually, usually these tools have a specific header.
    // Based on SS_tools analysis:
    // It likely writes the length first, then the data.
    // Detailed analysis required for exact header structure.
    // Assuming standard LSB approach for now, but to be compatible with SS_tools, we need the exact spec.
    // Since I cannot re-browse 'duck_decode_node.py' in this turn without network, I will implement a generic robust LSB decoder first
    // and refine the header parsing based on the "common" structure or allow raw extraction to debug.
    
    // However, knowing the user wants "SS_tools" specifically, I should try to match it.
    // SS_tools often uses a password hash seed. 
    
    suspend fun decodeImage(uri: Uri, password: String?): DecodeResult = withContext(Dispatchers.Default) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext DecodeResult.Error("Cannot open file")
            
            // Allow mutable bitmap for potential pixel reading if needed, but here we just read pixels
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            if (bitmap == null) return@withContext DecodeResult.Error("Failed to decode bitmap")

            // Try bit depths: 2, 6, 8
            val bitDepths = listOf(2, 6, 8)
            
            for (k in bitDepths) {
                try {
                    val result = attemptDecode(bitmap, k, password)
                    if (result is DecodeResult.Success) {
                        return@withContext result
                    } else if (result is DecodeResult.NeedPassword) {
                        return@withContext result
                    }
                } catch (e: Exception) {
                    // Continue to next bit depth
                }
            }
            
            DecodeResult.Error("No hidden data found or password incorrect")
        } catch (e: Exception) {
            DecodeResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun attemptDecode(bitmap: Bitmap, k: Int, password: String?): DecodeResult {
        // 1. Extract raw bytes (sufficient for header) from LSB
        // We need at least header size. Let's extract enough for header first.
        // Header min size: 4 (total len) + 1 (has_pwd) + ...
        // Let's implement an iterator or stream to pull bytes as needed to save memory
        
        val extractor = LsbExtractor(bitmap, k)
        
        // Read Total Payload Length (4 bytes, Big Endian)
        val totalLenBytes = extractor.readBytes(4) ?: return DecodeResult.Error("EOF reading length")
        val totalLen = ByteBuffer.wrap(totalLenBytes).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFF
        
        // Basic sanity check on length
        if (totalLen <= 0 || totalLen > 500_000_000) { // arbitrary sanity limit 500MB
             throw IllegalArgumentException("Invalid length: $totalLen")
        }
        
        // Read has_password (1 byte)
        val hasPwdByte = extractor.readBytes(1)?.get(0)?.toInt() ?: throw IllegalArgumentException("EOF flag")
        
        if (hasPwdByte == 1) {
            // Encrypted
            if (password.isNullOrEmpty()) {
                return DecodeResult.NeedPassword
            }
            
            // Read Encrypted Header
            // pwd_hash (32) + salt (16) + ext_len (1)
            val pwdHashStored = extractor.readBytes(32) ?: throw IllegalArgumentException("EOF hash")
            val salt = extractor.readBytes(16) ?: throw IllegalArgumentException("EOF salt")
            val extLen = extractor.readBytes(1)?.get(0)?.toInt() ?: throw IllegalArgumentException("EOF extLen")
            
            // Verify password
            // Python: hashlib.sha256((password + salt.hex()).encode()).digest()
            val saltHex = salt.joinToString("") { "%02x".format(it) }
            val computedHash = sha256((password + saltHex).toByteArray(Charsets.UTF_8))
            
            if (!computedHash.contentEquals(pwdHashStored)) {
                return DecodeResult.Error("Password incorrect")
            }
            
            // Password correct, proceed
            val extensionBytes = extractor.readBytes(extLen) ?: throw IllegalArgumentException("EOF ext")
            val extension = String(extensionBytes, Charsets.UTF_8)
            
            // Sanity check extension
            if (extension.any { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' && it != '.' }) {
                throw IllegalArgumentException("Invalid extension format")
            }

            val dataLenBytes = extractor.readBytes(4) ?: throw IllegalArgumentException("EOF dataLen")
            val dataLen = ByteBuffer.wrap(dataLenBytes).order(ByteOrder.BIG_ENDIAN).int
            
            // --- VALIDATION START ---
            val expectedTotalLen = 1 + 32 + 16 + 1 + extLen + 4 + dataLen
            if (expectedTotalLen.toLong() != totalLen) {
                throw IllegalArgumentException("Data length mismatch (encrypted): expected $expectedTotalLen, got $totalLen")
            }
            // --- VALIDATION END ---

            // Read Encrypted Data
            val encryptedData = extractor.readBytes(dataLen) ?: throw IllegalArgumentException("EOF data")
            
            // Decrypt
            val base = password + saltHex
            val decryptedData = xorDecrypt(encryptedData, base)
            
            return handleBinPngIfNeeded(decryptedData, extension)
            
        } else {
            // Plain
            val extLen = extractor.readBytes(1)?.get(0)?.toInt() ?: throw IllegalArgumentException("EOF")
            val extensionBytes = extractor.readBytes(extLen) ?: throw IllegalArgumentException("EOF")
            val extension = String(extensionBytes, Charsets.UTF_8)

            // Sanity check extension
            if (extension.any { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' && it != '.' }) {
                throw IllegalArgumentException("Invalid extension format")
            }
            
            val dataLenBytes = extractor.readBytes(4) ?: throw IllegalArgumentException("EOF")
            val dataLen = ByteBuffer.wrap(dataLenBytes).order(ByteOrder.BIG_ENDIAN).int
            
            // --- VALIDATION START ---
            val expectedTotalLen = 1 + 1 + extLen + 4 + dataLen
            if (expectedTotalLen.toLong() != totalLen) {
                throw IllegalArgumentException("Data length mismatch (plain): expected $expectedTotalLen, got $totalLen")
            }
            // --- VALIDATION END ---

            val rawData = extractor.readBytes(dataLen) ?: throw IllegalArgumentException("EOF")
            
            return handleBinPngIfNeeded(rawData, extension)
        }
    }

    private fun handleBinPngIfNeeded(data: ByteArray, extension: String): DecodeResult {
        return if (extension.endsWith(".binpng", ignoreCase = true)) {
            // This is a special SS_tools container: a PNG where pixels are raw bytes
            val mp4Data = processBinPng(data)
            // Strip .binpng from the final filename
            val finalName = extension.removeSuffix(".binpng")
            DecodeResult.Success(mp4Data, "decoded.$finalName")
        } else {
            DecodeResult.Success(data, "decoded.$extension")
        }
    }

    private fun processBinPng(pngData: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(pngData, 0, pngData.size) ?: return pngData
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val output = ByteArray(width * height * 3)
        var idx = 0
        for (pixel in pixels) {
            output[idx++] = ((pixel shr 16) and 0xFF).toByte()
            output[idx++] = ((pixel shr 8) and 0xFF).toByte()
            output[idx++] = (pixel and 0xFF).toByte()
        }
        
        // RStrip 0x00 padding (matching Python: .rstrip(b"\x00"))
        var lastIndex = output.size - 1
        while (lastIndex >= 0 && output[lastIndex] == 0.toByte()) {
            lastIndex--
        }
        return if (lastIndex < 0) ByteArray(0) else output.copyOfRange(0, lastIndex + 1)
    }
    
    // Helper class to extract bytes from Bitmap LSB
    private class LsbExtractor(val bitmap: Bitmap, val k: Int) {
        private val width = bitmap.width
        private val height = bitmap.height
        private val pixels = IntArray(width * height)
        
        // Skip region from python script
        private val startX: Int = (width * 0.40).toInt()
        private val startY: Int = (height * 0.08).toInt()
        
        private var pixelIndex = 0 // Flattened index logical
        
        // Pre-caclulated skip offset in pixels
        // The python script iterates rows then cols.
        // for y in range(h): for x in range(w): if x < startX and y < startY: continue
        
        // To be efficient, we'll flatten this invalid region check or just iterate simply and skip.
        // Actually getting all pixels at once is faster for JNI/Android Bitmap
        init {
             bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        }

        private var currentByte = 0
        private var bitsFilled = 0
        
        // Current coordinate state
        private var x = 0
        private var y = 0
        private var channel = 0 // 0=R, 1=G, 2=B
        
        fun readBytes(count: Int): ByteArray? {
            val result = ByteArray(count)
            for (i in 0 until count) {
                var b = 0
                var bitsNeeded = 8
                
                while (bitsNeeded > 0) {
                    if (!isValidPixel(x, y)) {
                        advancePixel()
                        continue
                    }
                    
                    val pixel = pixels[y * width + x]
                    val channelVal = when(channel) {
                        0 -> (pixel shr 16) and 0xFF
                        1 -> (pixel shr 8) and 0xFF
                        2 -> pixel and 0xFF
                        else -> 0
                    }
                    
                    // Extract k bits
                    // logic: val & ((1<<k) - 1)
                    val bits = channelVal and ((1 shl k) - 1)
                    
                    // How many bits to take?
                    // The python code: 
                    // for bit in range(k-1, -1, -1):
                    //    current_byte = (current_byte << 1) | ((val >> bit) & 1)
                    // It takes bits MSB to LSB from the extracted K bits.
                    
                    // Let's emulate bit by bit for simplicity matching python
                    for (bitPos in k - 1 downTo 0) {
                        if (bitsNeeded == 0) break // Should not happen if we align byte boundaries? 
                        // Wait, python code handles stream of bits. 
                        // We are reading a byte.
                        
                        val bit = (bits shr bitPos) and 1
                        b = (b shl 1) or bit
                        bitsNeeded--
                    }
                    
                    // Advance channel
                    channel++
                    if (channel > 2) {
                        channel = 0
                        advancePixel()
                    }
                }
                result[i] = b.toByte()
            }
            return result
        }

        private fun isValidPixel(cx: Int, cy: Int): Boolean {
           return !(cx < startX && cy < startY)
        }
        
        private fun advancePixel() {
            x++
            if (x >= width) {
                x = 0
                y++
            }
        }
    }

    private fun sha256(input: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input)
    }
    
    private fun xorDecrypt(data: ByteArray, base: String): ByteArray {
        // key_stream = sha256(base + "0") + sha256(base + "1") ...
        val output = ByteArray(data.size)
        var keyBuffer = ByteArray(0)
        var keyIndex = 0
        var counter = 0
        
        for (i in data.indices) {
            if (keyIndex >= keyBuffer.size) {
                // Generate next block
                val keyBase = (base + counter.toString()).toByteArray(Charsets.UTF_8)
                keyBuffer = sha256(keyBase)
                keyIndex = 0
                counter++
            }
            output[i] = data[i] xor keyBuffer[keyIndex]
            keyIndex++
        }
        return output
    }
}

sealed class DecodeResult {
    data class Success(val data: ByteArray, val filename: String) : DecodeResult()
    data object NeedPassword : DecodeResult()
    data class Error(val message: String) : DecodeResult()
}
