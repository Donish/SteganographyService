package config

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client.*
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File
import java.net.URL
import java.time.Duration

object S3Gateway {
    private val cfg = Config.cfg.s3
    private val s3 = builder()
        .region(Region.of(cfg.region))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()

    fun upload(local: File, key: String): URL {
        val req = PutObjectRequest.builder()
            .bucket(cfg.bucket)
            .key(key)
            .build()
        s3.putObject(req, local.toPath())

        val presign = S3Presigner.builder()
            .region(Region.of(cfg.region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

        val presigned = presign.presignGetObject { b ->
            b.getObjectRequest { r -> r.bucket(cfg.bucket).key(key) }
                .signatureDuration(Duration.ofMinutes(5))
        }
        return presigned.url()
    }

    fun download(url: URL, out: File) =
        url.openStream().use { it.copyTo(out.outputStream()) }
}