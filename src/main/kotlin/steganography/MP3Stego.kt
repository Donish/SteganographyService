package steganography

import parsers.Mp3Parser
import utils.FileUtils.stegoCopy
import java.io.File
import java.util.*

object MP3Stego : Stego {
    override fun embed(secret: String, carrier: File): File {
        val originalBytes = carrier.readBytes()
        if (originalBytes.size < 10 || originalBytes[0] != 'I'.code.toByte() ||
            originalBytes[1] != 'D'.code.toByte() || originalBytes[2] != '3'.code.toByte()) {
            throw IllegalArgumentException("No ID3v2 header in MP3 file")
        }

        val commentFrame = Mp3Parser.buildCommFrame(Base64.getEncoder().encodeToString(secret.toByteArray()))
        val id3Size = Mp3Parser.getId3Size(bytes = originalBytes)
        val newSize = id3Size - 10 + commentFrame.size

        val sizeBytes = Mp3Parser.toSyncSafeBytes(newSize)
        for (i in 0 ..< 4) {
            originalBytes[6 + i] = sizeBytes[i]
        }

        val id3Part = originalBytes.sliceArray(0 ..< 10) + originalBytes.sliceArray(10 ..< id3Size) + commentFrame
        val audioPart = originalBytes.sliceArray(id3Size ..< originalBytes.size)

        return carrier.stegoCopy(id3Part + audioPart)
    }

    override fun extract(carrier: File): String {
        val bytes = carrier.readBytes()

        val id3Header = byteArrayOf(0x49, 0x44, 0x33)
        if (!bytes.take(3).toByteArray().contentEquals(id3Header)) {
            throw IllegalArgumentException("No ID3v2 header in MP3 file")
        }

        val id3Size = Mp3Parser.getId3Size(bytes)
        val id3Bytes = bytes.sliceArray(0 ..< id3Size)

        val comm = Mp3Parser.getCommFrame(id3Bytes)

        val encoding = comm.content[0]
        val skip = when (encoding.toInt()) {
            0 -> 4 // ISO-8859-1: encoding(1) + lang(3)
            1 -> 5 // UTF-16 BOM
            else -> throw IllegalArgumentException("Unsupported encoding: $encoding")
        }

        val afterLang = comm.content.drop(skip)
        val zeroIdx = afterLang.indexOf(0)
        val actualCommentBytes = afterLang.drop(zeroIdx + 1).toByteArray()

        val base64 = actualCommentBytes.toString(Charsets.ISO_8859_1)
        return String(Base64.getDecoder().decode(base64), Charsets.UTF_8)
    }
}
