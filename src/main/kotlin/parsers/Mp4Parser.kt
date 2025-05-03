package parsers

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object Mp4Parser {

    fun createMp4CommentAtom(comment: String): ByteArray {
        val data = comment.toByteArray(StandardCharsets.UTF_8)
        val size = 8 + data.size
        val header = ByteBuffer.allocate(4).putInt(size).array() +
                "©cmt".toByteArray(StandardCharsets.US_ASCII)
        return header + data
    }

    fun findAtomOffset(bytes: ByteArray, atomType: String): Int? {
        var cursor = 0
        while (cursor + 8 <= bytes.size) {
            val size = ByteBuffer.wrap(bytes, cursor, 4).int
            if (size <= 8 || cursor + size > bytes.size) break
            val type = String(bytes, cursor + 4, 4, StandardCharsets.US_ASCII)
            if (type == atomType) return cursor
            cursor += size
        }
        return null
    }

    fun findFreeAtomIndex(bytes: ByteArray): Int {
        var index = 0
        while (index + 8 <= bytes.size) {
            val size = ByteBuffer.wrap(bytes, index, 4).int
            if (size < 8 || index + size > bytes.size) break
            val type = String(bytes, index + 4, 4)
            if (type == "free") {
                return index
            }

            index += size
        }
        return -1
    }

}