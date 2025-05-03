package vault

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URL
import org.slf4j.LoggerFactory

class VaultProvider(private val vaultSettings: VaultSettings) {

    private val log = LoggerFactory.getLogger("VaultStego")
    private val mapper = ObjectMapper()

    fun writeToVault(base64: String) {
        val url = URL("${vaultSettings.address}/v2/secret/image")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("X-Vault-Token", vaultSettings.token)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val body = mapOf("data" to mapOf("image" to base64))
        val json = mapper.writeValueAsString(body)
        conn.outputStream.write(json.toByteArray())
        log.info("Uploaded file into Vault. Status: ${conn.responseCode}")
    }

    fun readFromVault(): String {
        val url = URL("${vaultSettings.address}/v2/secret/image")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("X-Vault-Token", vaultSettings.token)
        conn.setRequestProperty("Content-Type", "application/json")

        val response = conn.inputStream.bufferedReader().readText()
        val tree = mapper.readTree(response)
        return tree["data"]["data"]["image"].asText()
    }
}