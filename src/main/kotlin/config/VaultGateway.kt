package config

import com.bettercloud.vault.Vault
import com.bettercloud.vault.VaultConfig
import java.net.URL
import java.util.*

object VaultGateway {
    private val cfg = Config.cfg.vault
    private val client by lazy {
        val conf = VaultConfig().address(cfg.url).token(cfg.token).build()
        Vault(conf)
    }

    fun encrypt(bytes: ByteArray): String {
        val base64 = Base64.getEncoder().encodeToString(bytes)
        val res = client.logical().write("transit/encrypt/${cfg.transitKey}",
            mapOf("plaintext" to base64))
        return res.data["ciphertext"] as String
    }

    fun decrypt(cipher: String): ByteArray {
        val res = client.logical().write("transit/decrypt/${cfg.transitKey}",
            mapOf("ciphertext" to cipher))
        val plain = res.data["plaintext"] as String
        return Base64.getDecoder().decode(plain)
    }

    fun storeLink(id: String, url: URL, hash: String) {
        val path = "${cfg.kvPath}/$id"
        client.logical().write(path, mapOf("url" to url.toString(), "hash" to hash))
    }

    fun readLink(id: String): URL {
        val data = client.logical().read("${cfg.kvPath}/$id").data
        return URL(data["url"])
    }
}