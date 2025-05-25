package parsers

import utils.ByteUtils.toInt
import utils.ByteUtils.toByteArray
import java.nio.charset.StandardCharsets

data class Id3Frame(
    val id: String,
    val size: Int,
    val flags: ByteArray,
    val content: ByteArray
)

object Mp3Parser {

    fun buildCommFrame(comment: String): ByteArray {
        val frameId = "COMM".toByteArray()
        val encoding = byteArrayOf(0x00)
        val language = "eng".toByteArray()
        val shortDescription = byteArrayOf(0x00)
        val commentBytes = comment.toByteArray(StandardCharsets.ISO_8859_1)
        val content = encoding + language + shortDescription + commentBytes
        val size = content.size
        return frameId + size.toByteArray(4) + byteArrayOf(0x00, 0x00) + content
    }

    fun getId3Size(bytes: ByteArray): Int {
        val sizeBytes = bytes.sliceArray(6..9)
        return (sizeBytes[0].toInt() and 0x7F shl 21) +
                (sizeBytes[1].toInt() and 0x7F shl 14) +
                (sizeBytes[2].toInt() and 0x7F shl 7) +
                (sizeBytes[3].toInt() and 0x7F) + 10
    }

    fun getCommFrame(id3Part: ByteArray): Id3Frame {
        var cursor = 10

        val frames = id3Part.sliceArray(cursor ..< id3Part.size)
        val framesString = frames.toString(Charsets.US_ASCII)
        val commIdx = framesString.indexOf("COMM")
        if (commIdx == -1) throw IllegalStateException("COMM not found")

        val commBytes = framesString.slice(commIdx ..< framesString.length).toByteArray()

        val id = commBytes.sliceArray(0 ..< 4).toString(Charsets.US_ASCII)
        val sizeBytes = commBytes.sliceArray(4 ..< 8)
        val size = sizeBytes.toInt()
        val flags = commBytes.sliceArray(8 ..< 10)
        val content = commBytes.sliceArray(10 ..< commBytes.size)

        return Id3Frame(
            id = id,
            size = size,
            flags = flags,
            content = content
        )
    }

    fun toSyncSafeBytes(value: Int): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = ((value shr 21) and 0x7F).toByte()
        bytes[1] = ((value shr 14) and 0x7F).toByte()
        bytes[2] = ((value shr 7) and 0x7F).toByte()
        bytes[3] = (value and 0x7F).toByte()
        return bytes
    }
}
