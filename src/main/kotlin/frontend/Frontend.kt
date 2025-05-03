package frontend

import steganography.SteganographyService
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

// todo: добавление ключа секрета
// todo: переделать extract: секрет должен вытаскиваться по ключу
fun createUi() {
    val frame = JFrame("Stego Vault")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.layout = BorderLayout()

    val fileLabel = JLabel("File is not selected")
    val secretField = JTextField(30)
    val resultArea = JTextArea(5, 40).apply { isEditable = false }

    val chooseButton = JButton("Select file")
    val embedButton = JButton("Embed secret")
    val extractButton = JButton("Extract secret")
    var selectedFile: File? = null
    val steganographyService = SteganographyService()

    chooseButton.addActionListener {
        val selector = JFileChooser()
        selector.fileFilter = FileNameExtensionFilter("Valid files", "PNG", "PDF", "MP3", "MP4")
        val result = selector.showOpenDialog(frame)
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = selector.selectedFile
            fileLabel.text = "Selected: ${selectedFile?.name}"
        }
    }

    embedButton.addActionListener { _: ActionEvent ->
        val file = selectedFile
        val secret = secretField.text
        if (file == null || secret.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Select file and enter your secret", "Error", JOptionPane.ERROR_MESSAGE)
            return@addActionListener
        }

//        val outFile = File(file.parent, "output-${file.name}")
        try {
            steganographyService.embedSecret(
                secret = secret,
                inputFile = file
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(frame, e.message, "Error", JOptionPane.ERROR_MESSAGE)
        }

        resultArea.text = "Secret embed into"
    }

    extractButton.addActionListener {
        val file = selectedFile
        if (file == null) {
            JOptionPane.showMessageDialog(frame, "Select file", "Error", JOptionPane.ERROR_MESSAGE)
            return@addActionListener
        }

        val extracted = try {
            steganographyService.extractSecret(inputFile = file)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }

        resultArea.text = "Extracted secret: $extracted"
    }

    val inputPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(fileLabel)
        add(Box.createVerticalStrut(5))
        add(JLabel("Secret:"))
        add(secretField)
        add(Box.createVerticalStrut(5))
        add(chooseButton)
        add(embedButton)
        add(extractButton)
    }

    frame.add(inputPanel, BorderLayout.NORTH)
    frame.add(JScrollPane(resultArea), BorderLayout.CENTER)

    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
}