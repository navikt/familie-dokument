package no.nav.familie.dokument.storage.google

import com.google.api.gax.retrying.RetrySettings
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
    fun gcpStorage(retrySettings: RetrySettings,
                         @Value("\${attachment.max.size.mb}") maxFileSizeMB: Int): GcpStorage {
        return GcpStorage(maxFileSizeMB, retrySettings)
    }

    @Bean(ATTACHMENT_GCP_STORAGE)
    fun attachmentStorage(storage: GcpStorage): GcpStorageWrapper{
        return GcpStorageWrapper(storage, MediaType.APPLICATION_PDF_VALUE)
    }

    @Bean(STONAD_GCP_STORAGE)
    fun stonadStorage(storage: GcpStorage): GcpStorageWrapper{
        return GcpStorageWrapper(storage, MediaType.APPLICATION_JSON_VALUE)
    }

    companion object{
        const val ATTACHMENT_GCP_STORAGE = "attachmentGcpStorage"
        const val STONAD_GCP_STORAGE = "stonadGcpStorage"
    }
}