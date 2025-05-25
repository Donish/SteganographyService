package config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

data class VaultConf(val url: String, val token: String, val transitKey: String, val kvPath: String)
data class S3Conf(val bucket: String, val region: String)
data class AppConfig(val vault: VaultConf, val s3: S3Conf)

object Config {
    private val mapper = jacksonObjectMapper().registerModule(KotlinModule())
    val cfg: AppConfig by lazy {
        val file = File("config.yml")
        mapper.readValue(file, AppConfig::class.java)
    }
}