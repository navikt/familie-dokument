package no.nav.familie.dokument.config

import com.amazonaws.services.s3.model.AmazonS3Exception
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.attachment.AttachmentToStorableFormatConverter
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
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
    val lokalStorageAttachment: MutableMap<String, ByteArray> = HashMap()

    @Bean
    @Primary
    fun encryptedStorage(): EncryptedStorage {
        val storage: EncryptedStorage = mockk()

        val slotUser = slot<String>()
        val slotKey = slot<String>()
        val slotInputStream = slot<InputStream>()
        every { storage[capture(slotUser), capture(slotKey)] } answers {
            lokalStorage.getOrElse(slotUser.captured + "_" + slotKey.captured, {
                val e = AmazonS3Exception("Noe gikk galt")
                e.statusCode = 404
                throw e
            })
        }
        every { storage.put(capture(slotUser), capture(slotKey), capture(slotInputStream)) } answers {
            lokalStorage[slotUser.captured + "_" + slotKey.captured] = slotInputStream.captured.readAllBytes()
        }
        every { storage.delete(capture(slotUser), capture(slotKey)) } answers {
            lokalStorage.remove(slotUser.captured + "_" + slotKey.captured)
        }
        return storage
    }

    @Bean
    @Primary
    fun converter(@Autowired imageConversionService: ImageConversionService): AttachmentToStorableFormatConverter {
        val slot = slot<ByteArray>()
        val converter: AttachmentToStorableFormatConverter = mockk()

        every { converter.toStorageFormat(capture(slot)) } answers { slot.captured }

        return converter
    }

    @Bean
    @Primary
    fun attachmentStorage(
            @Autowired encryptedStorage: EncryptedStorage,
            @Autowired storableFormatConverter: AttachmentToStorableFormatConverter): AttachmentStorage {
        val slot = slot<String>()
        val slotPut = slot<String>()
        val slotByteArray = slot<ByteArray>()
        val storage: AttachmentStorage = mockk()

        every { storage.put(capture(slotPut), any(), capture(slotByteArray)) } answers {
            lokalStorageAttachment[slotPut.captured] = slotByteArray.captured
        }
        every { storage[capture(slot), any()] } answers {
            lokalStorageAttachment.getOrElse(slot.captured, {
                val e = AmazonS3Exception("Noe gikk galt")
                e.statusCode = 404
                throw e
            })
        }

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
