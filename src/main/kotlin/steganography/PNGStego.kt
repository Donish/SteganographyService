package steganography

import utils.Extensions
import utils.FileUtils.copyImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

object PNGStego : Stego {
    override fun embed(secret: String, carrier: File): File {
        val image = ImageIO.read(carrier)
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

        val tmpFile =  Files.createTempFile("stego", ".${Extensions.PNG.value}").toFile()
        ImageIO.write(img, Extensions.PNG.value, tmpFile)
        return tmpFile
    }

    override fun extract(carrier: File): String {
        val img = ImageIO.read(carrier)
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
}
