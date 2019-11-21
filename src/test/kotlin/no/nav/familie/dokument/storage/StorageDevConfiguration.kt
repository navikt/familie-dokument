package no.nav.familie.dokument.storage

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.dokument.storage.s3.S3Storage
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.*


@Profile("dev")
@Configuration
class StorageDevConfiguration {

    @Bean
    @Primary
    fun s3Storage(): S3Storage {
        val storage = mockk<S3Storage>()
        every { storage[any(), any()] } returns Optional.of("filinnhold".toByteArray())
        return storage
    }
}