package no.nav.familie.dokument.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.dokument.storage.attachment.AttachmentConfiguration
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.attachment.AttachmentToStorableFormatConverter
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import no.nav.familie.dokument.storage.s3.S3Storage
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.*
import java.util.*


@Profile("dev")
@Configuration
@Import(TokenGeneratorConfiguration::class)
class TestStorageConfiguration {

    @Bean
    @Primary
    fun encryptedStorage(): EncryptedStorage {
        val storage: EncryptedStorage = mockk()


        every { storage[any(), any()] } returns Optional.of("filinnhold".toByteArray())

        return storage
    }

    @Bean
    @Primary
    fun S3Storage(): S3Storage {
        val storage: S3Storage = mockk()

        every { storage[any(), any()] } returns Optional.of("filinnhold".toByteArray())

        return storage
    }

    @Bean
    @Primary
    internal fun converter(imageConversionService: ImageConversionService): AttachmentToStorableFormatConverter {
        return mockk()
    }

    @Bean
    @Primary
    fun attachmentStorage(
            @Autowired encryptedStorage: EncryptedStorage,
            storableFormatConverter: AttachmentToStorableFormatConverter): AttachmentStorage {
        val storage: AttachmentStorage = mockk()

        every { storage.put(any(), any(),any()) }  just Runs
        every { storage[any(), any()] } returns Optional.of("filinnhold".toByteArray())

        return storage
    }
}
