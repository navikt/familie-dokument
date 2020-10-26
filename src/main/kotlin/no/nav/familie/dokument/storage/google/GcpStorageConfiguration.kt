package no.nav.familie.dokument.storage.google

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.threeten.bp.Duration

@Configuration
class GcpStorageConfiguration {

    @Bean
    fun retrySettings(@Value("\${storage_service.timeout.ms:3000}") timeoutMs: Long): RetrySettings? {
        return RetrySettings.newBuilder()
                .setTotalTimeout(Duration.ofMillis(timeoutMs))
                .build()
    }

    @Bean
    fun gcpStorage(storage: Storage,
                   @Value("\${gcp.storage.bucketname}") bucketName: String,
                   @Value("\${attachment.max.size.mb}") maxFileSizeMB: Int): GcpStorage {
        return GcpStorage(bucketName, maxFileSizeMB, storage)
    }

    @Bean
    fun storage(retrySettings: RetrySettings): Storage{
        val storage = StorageOptions
                .newBuilder()
                .setRetrySettings(retrySettings)
                .build()
                .service
        LOG.info("Google Storage intialized")
        return storage
    }

    @Bean(ATTACHMENT_GCP_STORAGE)
    fun attachmentStorage(storage: GcpStorage): GcpStorageWrapper {
        return GcpStorageWrapper(storage, MediaType.APPLICATION_PDF_VALUE)
    }

    @Bean(STONAD_GCP_STORAGE)
    fun stonadStorage(storage: GcpStorage): GcpStorageWrapper {
        return GcpStorageWrapper(storage, MediaType.APPLICATION_JSON_VALUE)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(GcpStorageConfiguration::class.java)
        const val ATTACHMENT_GCP_STORAGE = "attachmentGcpStorage"
        const val STONAD_GCP_STORAGE = "stonadGcpStorage"
    }
}