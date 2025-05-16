package utils

import java.awt.image.BufferedImage
import java.io.File
import java.lang.IllegalArgumentException

object FileUtils {

    fun getResourceFile(fileName: String): File {
        val resource = object {}.javaClass.getResource("/$fileName")
            ?: throw IllegalArgumentException("file $fileName not found")
        return File(resource.toURI())
    }

    fun BufferedImage.copyImage(): BufferedImage =
        BufferedImage(width, height, type).apply { graphics.drawImage(this@copyImage, 0, 0, null) }

    fun File.stegoCopy(content: ByteArray): File =
        File(parentFile, nameWithoutExtension + "_stego." + extension).also { it.writeBytes(content) }
}