package factories

import steganography.MP3Stego
import steganography.PDFStego
import steganography.PNGStego
import steganography.Stego
import java.io.File

object StegoEngineFactory {
    fun forFile(file: File): Stego = when (file.extension.lowercase()) {
        "pdf"  -> PDFStego
        "mp3"  -> MP3Stego
        "png"  -> PNGStego
        else   -> throw UnsupportedOperationException(
            "Format «${file.extension}» is not supported")
    }

    fun forExtension(ext: String): Stego = when (ext.lowercase()) {
        "pdf"  -> PDFStego
        "mp3"  -> MP3Stego
        "png"  -> PNGStego
        else   -> throw UnsupportedOperationException("Format «$ext» is not supported")
    }
}