package steganography

import config.S3Gateway
import config.VaultGateway
import factories.StegoEngineFactory
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.time.LocalDate
import java.util.UUID

object StegoProvider {
    fun embed(file: File, secret: String) {
        val stegoEngine = StegoEngineFactory.forFile(file = file)
        val tmp = stegoEngine.embed(
            secret = secret,
            carrier = file
        )

        val id = UUID.randomUUID().toString()
        val key = "${LocalDate.now()}/$id.${file.extension}"
        val url = S3Gateway.upload(
            local = file,
            key = key
        )

        val hash = tmp.inputStream().use { DigestUtils.sha256Hex(it) }
        VaultGateway.storeLink(id, url, hash)
    }

    fun extract(id: String, out: File) {
        val (url, expectedHash) = VaultGateway.readLink(id)
        S3Gateway.download(
            url = url,
            out = out
        )

        if (DigestUtils.sha256Hex(out.inputStream()) != expectedHash) error("Hash mismatch")

        val engine = StegoEngineFactory.forFile(out)
        val cipher = engine.extract(out)
        val secret = String(VaultGateway.decrypt(cipher))
        println("SECRET: $secret")
    }
}