package frontend

import config.S3Gateway
import config.VaultGateway
import factories.StegoEngineFactory
import org.apache.commons.codec.digest.DigestUtils
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter


fun createUi() {
    val frame = JFrame("Stego Vault")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.layout = BorderLayout()

    val fileLabel = JLabel("File is not selected")
    val secretField = JTextField(30)
    val idField = JTextField(30)
    val resultArea = JTextArea(5, 40).apply { isEditable = false }

    val chooseButton = JButton("Select file")
    val embedButton = JButton("Embed secret")
    val extractButton = JButton("Extract secret")
    var selectedFile: File? = null

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
        val engine = StegoEngineFactory.forFile(file)

        try {
            val tmp = engine.embed(
                secret = secret,
                carrier = file
            )
            val id = UUID.randomUUID().toString()
            val key = "${LocalDate.now()}/$id.${file.extension}"
            val url = S3Gateway.upload(tmp, key)
            val hash = tmp.inputStream().use { DigestUtils.sha256Hex(it) }
            VaultGateway.storeLink(id, url, hash)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(frame, e.message, "Error", JOptionPane.ERROR_MESSAGE)
        }

        resultArea.text = "Secret embed into"
    }

    extractButton.addActionListener {
        val id = idField.text
        if (id.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Enter secret id", "Error", JOptionPane.ERROR_MESSAGE)
            return@addActionListener
        }
        val url = VaultGateway.readLink(id)
        val tmp = File.createTempFile("tmp", ".tmp")
        S3Gateway.download(url, tmp)
        val engine = StegoEngineFactory.forFile(tmp)

        val extracted = try {
            engine.extract(tmp)
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