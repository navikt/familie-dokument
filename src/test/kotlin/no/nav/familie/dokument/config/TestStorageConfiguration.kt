package no.nav.familie.dokument.config

import com.amazonaws.services.s3.model.AmazonS3Exception
import io.mockk.*
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.attachment.AttachmentToStorableFormatConverter
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import no.nav.familie.dokument.storage.s3.S3Storage
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.*
import java.io.InputStream


@Profile("dev")
@Configuration
@Import(TokenGeneratorConfiguration::class)
class TestStorageConfiguration {

    val lokalStorage: MutableMap<String, ByteArray> = HashMap()

    @Bean
    @Primary
    fun encryptedStorage(): EncryptedStorage {
        val storage: EncryptedStorage = mockk()

        val slot = slot<String>()
        val slotPut = slot<String>()
        val slotInputStream = slot<InputStream>()
        every { storage[capture(slot), any()] } answers {
            lokalStorage.getOrElse(slot.captured, {
                val e = AmazonS3Exception("Noe gikk galt")
                e.statusCode = 404
                throw e
            })
        }
        every { storage.put(capture(slotPut), any(), capture(slotInputStream)) } answers {
            lokalStorage[slotPut.captured] = slotInputStream.captured.readAllBytes()
        }
        every { storage.delete(capture(slotPut), any()) } answers {
            lokalStorage.remove(slotPut.captured)
        }
        return storage
    }

    @Bean
    @Primary
    fun S3Storage(): S3Storage {
        val storage: S3Storage = mockk()

        every { storage[any(), any()] } returns "filinnhold".toByteArray()

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

        every { storage.put(any(), any(), any()) } just Runs
        every { storage[any(), any()] } returns "filinnhold".toByteArray()

        return storage
    }

    @Bean
    fun corsFilter(corsProperties: CorsProperties): FilterRegistrationBean<CORSResponseFilter> {
        val filterRegistration = FilterRegistrationBean<CORSResponseFilter>()
        filterRegistration.filter = CORSResponseFilter(corsProperties)
        filterRegistration.order = 0
        return filterRegistration
    }

}
