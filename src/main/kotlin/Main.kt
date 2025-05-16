import frontend.createUi
import steganography.SteganographyService
import utils.FileUtils
import javax.swing.SwingUtilities

fun main() {
//    SwingUtilities.invokeLater { createUi() }
    some()
}

fun some() {
    val loginSecret = "this_secret"
    val stegoService = SteganographyService()
    val sourceFile = FileUtils.getResourceFile(fileName = "song.mp3")


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