package steganography

import java.io.File

interface Stego {
    fun embed(secret: String, carrier: File): File
    fun extract(carrier: File): String
}