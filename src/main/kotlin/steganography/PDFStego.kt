package steganography

import utils.ByteUtils.lastIndexOfSlice
import utils.FileUtils.stegoCopy
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

object PDFStego : Stego {
    override fun embed(secret: String, carrier: File): File {
        val secretComment = "\n%stealth-secret: ${Base64.getEncoder().encodeToString(secret.toByteArray())}\n"
        val originalBytes = carrier.readBytes()
        val eofMarker = "%EOF".toByteArray()
        val eofIndex = originalBytes.lastIndexOfSlice(eofMarker)
        if (eofIndex == -1) {
            throw IllegalStateException("%EOF not found in PDF")
        }

        val beforeEOF = originalBytes.copyOfRange(0, eofIndex)
        val afterEOF = originalBytes.copyOfRange(eofIndex, originalBytes.size)

        val modifiedBytes = beforeEOF + secretComment.toByteArray() + afterEOF
        return carrier.stegoCopy(modifiedBytes)
    }

    override fun extract(carrier: File): String {
        val content = carrier.readText()
        val regex = Regex("%stealth-secret: ([A-Za-z0-9+/=]+)")
        val match = regex.find(content) ?: throw IllegalStateException("Secret not found in PDF")
        val base64 = match.groupValues[1]
        return String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8)
    }
}
