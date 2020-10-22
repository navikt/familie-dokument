package no.nav.familie.dokument.storage.google

import com.google.api.gax.retrying.RetrySettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.threeten.bp.Duration

@Configuration
class GoogleStorageConfiguration {

    @Bean
    fun retrySettings(@Value("\${storage_service.timeout.ms:3000}") timeoutMs: Long): RetrySettings? {
        return RetrySettings.newBuilder()
                .setTotalTimeout(Duration.ofMillis(timeoutMs))
                .build()
    }

    @Bean fun gcpStorage(retrySettings: RetrySettings,
                         @Value("\${attachment.max.size.mb}") maxFileSizeMB: Int): GcpStorage {
        return GcpStorage(maxFileSizeMB, retrySettings)
    }
}