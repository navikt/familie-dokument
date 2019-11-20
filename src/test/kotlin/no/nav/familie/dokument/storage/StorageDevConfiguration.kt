package no.nav.familie.dokument.storage

import no.nav.familie.dokument.storage.s3.S3Storage
import org.mockito.ArgumentMatchers.any
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
        val storage = Mockito.mock(S3Storage::class.java)

        // TODO: Finn en mock-løsning som funker med Kotlin
        // Mockito.`when`(storage[any(), any()]).thenReturn(Optional.of("filinnhold".toByteArray()))

        return storage
    }
}