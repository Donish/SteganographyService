package vault

import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule

data class VaultConfig(
    val vault: VaultSettings
)

data class VaultSettings(
    val address: String,
    val token: String,
    val secretPath: String
)

fun loadVaultConfig(file: File): VaultSettings {
    val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
    val config = mapper.readValue(file, VaultConfig::class.java)
    return config.vault
}