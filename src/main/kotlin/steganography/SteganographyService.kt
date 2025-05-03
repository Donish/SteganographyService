package steganography

import parsers.Mp3Parser
import parsers.Mp4Parser
import utils.Constants.RESOURCES
import utils.Constants.RESOURCES_ERROR_MSG
import utils.ByteUtils.toInt
import utils.ByteUtils.lastIndexOfSlice
import utils.Extensions
import utils.FileUtils.copyImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.imageio.ImageIO

// todo: добавить ключ к секрету
class SteganographyService {

    fun embedSecret(secret: String, inputFile: File) {
        val resourcesDir = File(RESOURCES)
        if (!resourcesDir.exists()) {
            throw IllegalStateException(RESOURCES_ERROR_MSG)
        }
        val outputFile = File(resourcesDir, inputFile.nameWithoutExtension + "_stego." + inputFile.extension)

        when (inputFile.extension) {
            Extensions.PNG.value -> embedInPng(
                secret = secret,
                input = inputFile,
                output = outputFile
            )
            Extensions.PDF.value -> embedInPdf(
                secret = secret,
                input = inputFile,
                output = outputFile
            )
            Extensions.MP3.value -> embedInMp3(
                secret = secret,
                input = inputFile,
                output = outputFile
            )
//            Extensions.MP4.value -> embedInMp4(
//                secret = secret,
//                input = inputFile,
//                output = outputFile
//            )
            else -> throw IllegalArgumentException("Invalid file extension [embedding]")
        }
    }

    fun extractSecret(inputFile: File): String {
        return when (inputFile.extension) {
            Extensions.PNG.value -> extractFromPng(input = inputFile)
            Extensions.PDF.value -> extractFromPdf(input = inputFile)
            Extensions.MP3.value -> extractFromMp3(input = inputFile)
//            Extensions.MP4.value -> extractFromMp4(input = inputFile)
            else -> throw IllegalArgumentException("Invalid file extension [extracting]")
        }
    }

    // PNG stego
    private fun embedInPng(secret: String, input: File, output: File) {
        val image = ImageIO.read(input)
        val binary = secret.toByteArray().flatMap { byte ->
            (7 downTo 0).map { bit ->
                (byte.toInt() shr bit) and 1
            }
        }

        val img = image.copyImage()
        var bitIdx = 0

        loop@ for (y in 0 ..< img.height) {
            for (x in 0 ..< img.width) {
                val rgb = img.getRGB(x, y)
                val newRgb = if (bitIdx < binary.size) {
                    val blue = rgb and 0xFF
                    val newBlue = (blue and 0xFE) or binary[bitIdx++]
                    (rgb and 0xFFFFFF00.toInt()) or newBlue
                } else rgb
                img.setRGB(x, y, newRgb)
                if (bitIdx >= binary.size) break@loop
            }
        }

        ImageIO.write(img, Extensions.PNG.value, output)
    }

    private fun extractFromPng(input: File): String {
        val img = ImageIO.read(input)
        val bits = mutableListOf<Int>()

        for (y in 0..< img.height) {
            for (x in 0 ..< img.width) {
                val blue = img.getRGB(x, y) and 0xFF
                bits.add(blue and 1)
            }
        }

        val bytes = bits.chunked(8)
            .map { it.fold(0) { acc, bit -> (acc shl 1) or bit } }
            .takeWhile { it != 0 }
            .map { it.toByte() }
            .toByteArray()

        return String(bytes)
    }

    // PDF stego
    private fun embedInPdf(secret: String, input: File, output: File) {
        val secretComment = "\n%stealth-secret: ${Base64.getEncoder().encodeToString(secret.toByteArray())}\n"
        val originalBytes = input.readBytes()
        val eofMarker = "%EOF".toByteArray()
        val eofIndex = originalBytes.lastIndexOfSlice(eofMarker)
        if (eofIndex == -1) {
            throw IllegalStateException("%EOF not found in PDF")
        }

        val beforeEOF = originalBytes.copyOfRange(0, eofIndex)
        val afterEOF = originalBytes.copyOfRange(eofIndex, originalBytes.size)

        val modifiedBytes = beforeEOF + secretComment.toByteArray() + afterEOF
        output.writeBytes(modifiedBytes)
    }

    private fun extractFromPdf(input: File): String {
        val content = input.readText()
        val regex = Regex("%stealth-secret: ([A-Za-z0-9+/=]+)")
        val match = regex.find(content) ?: throw IllegalStateException("Secret not found in PDF")
        val base64 = match.groupValues[1]
        return String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8)
    }

    // MP3 stego
    private fun embedInMp3(secret: String, input: File, output: File) {
        val originalBytes = input.readBytes()
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

        output.writeBytes(id3Part + audioPart)
    }

    private fun extractFromMp3(input: File): String {
        val bytes = input.readBytes()

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

    // MP4 stego no work todo
    private fun embedInMp4(secret: String, input: File, output: File) {
        val bytes = input.readBytes()
        val freeAtomIndex = Mp4Parser.findFreeAtomIndex(bytes)
        if (freeAtomIndex == -1) {
            throw IllegalStateException("Атом 'free' не найден в файле. Встраивание невозможно.")
        }

        val secretBase64 = Base64.getEncoder().encodeToString(secret.toByteArray())
        val originalSize = ByteBuffer.wrap(bytes, freeAtomIndex, 4).int
        val maxPayloadSize = originalSize - 8

        val secretBytes = secretBase64.toByteArray()
        if (secretBytes.size > maxPayloadSize) {
            throw IllegalArgumentException("Секрет слишком велик для атома 'free'. Максимум: $maxPayloadSize байт")
        }

        val newFreeAtomPayload = secretBytes + ByteArray(maxPayloadSize - secretBytes.size) { 0 }
        val newBytes = bytes.copyOf()
        System.arraycopy(newFreeAtomPayload, 0, newBytes, freeAtomIndex + 8, newFreeAtomPayload.size)

        output.writeBytes(newBytes)
    }

    private fun extractFromMp4(input: File): String {
        val bytes = input.readBytes()
        val atomType = "@cmt".toByteArray()

        var index = 0
        while (index < bytes.size - 8) {
            val size = bytes.copyOfRange(index, index + 4).toInt()
            val type = bytes.copyOfRange(index + 4, index + 8)

            if (type.contentEquals(atomType)) {
                val content = bytes.copyOfRange(index + 8, index + size)
                return String(Base64.getDecoder().decode(content), Charsets.UTF_8)
            }

            index += size
        }

        throw IllegalStateException("Atom @cmt not found in MP4")
    }
}