import steganography.SteganographyService
import utils.FileUtils

fun main() {
//    SwingUtilities.invokeLater { createUi() }
    some()
}

fun some() {
    val loginSecret = "this_secret"
    val stegoService = SteganographyService()
    val sourceFile = FileUtils.getResourceFile(fileName = "video.mp4")


    stegoService.embedSecret(
        secret = loginSecret,
        inputFile = sourceFile
    )

//    val file = FileUtils.getResourceFile("dice_stego.png")
//    val extractedSecret = stegoService.extractSecret(
//        inputFile = file
//    )
//    println("Secret: $extractedSecret")

}