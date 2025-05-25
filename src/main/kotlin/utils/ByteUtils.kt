package utils

import java.io.File
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

object ByteUtils {

    fun Int.toByteArray(length: Int): ByteArray {
        val bytes = ByteArray(length)
        for (i in 0 ..< length) {
            bytes[length - 1 - i] = (this shr (8 * i) and 0xFF).toByte()
        }
        return bytes
    }

    fun ByteArray.lastIndexOfSlice(slice: ByteArray): Int {
        if (slice.isEmpty() || this.size < slice.size) return -1
        outer@ for (i in this.size - slice.size downTo 0) {
            for (j in slice.indices) {
                if (this[i + j] != slice[j]) continue@outer
            }
            return i
        }
        return -1
    }

    fun ByteArray.toInt(): Int {
        require(this.size == 4) { "Size must equal 4 bytes" }

        return (this[0].toInt() and 0xFF shl 24) or
                (this[1].toInt() and 0xFF shl 16) or
                (this[2].toInt() and 0xFF shl 8) or
                (this[3].toInt() and 0xFF)
    }
}
