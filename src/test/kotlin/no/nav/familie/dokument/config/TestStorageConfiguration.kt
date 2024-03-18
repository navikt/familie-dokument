package no.nav.familie.dokument.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.attachment.AttachmentToStorableFormatConverter
import no.nav.familie.dokument.storage.attachment.FlattenPdfService
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration.Companion.ATTACHMENT_ENCRYPTED_STORAGE
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration.Companion.STONAD_ENCRYPTED_STORAGE
import no.nav.familie.dokument.virusscan.VirusScanService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.io.InputStream

@Profile("local", "local-fd")
@Configuration
class TestStorageConfiguration {

    val lokalStorage: MutableMap<String, ByteArray> = HashMap()
    val lokalStorageAttachment: MutableMap<String, ByteArray> = HashMap()

    @Primary
    @Bean(STONAD_ENCRYPTED_STORAGE)
    fun encryptedStorage(): EncryptedStorage {
        val storage: EncryptedStorage = mockk()

        val slotUser = slot<String>()
        val slotKey = slot<String>()
        val slotInputStream = slot<InputStream>()
        every { storage[capture(slotUser), capture(slotKey)] } answers {
            lokalStorage.getOrElse(slotUser.captured + "_" + slotKey.captured, {
                throw RuntimeException("Noe gikk galt")
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
        return AttachmentToStorableFormatConverter(ImageConversionService(), FlattenPdfService())
    }

    @Bean
    @Primary
    fun attachmentStorage(
        @Qualifier(ATTACHMENT_ENCRYPTED_STORAGE) encryptedStorage: EncryptedStorage,
        storableFormatConverter: AttachmentToStorableFormatConverter,
    ): AttachmentStorage {
        val storage: AttachmentStorage = mockk()

        every { storage.put(any(), any(), any()) } answers {
            val user = firstArg<String>()
            val documentId = secondArg<String>()
            val document = thirdArg<ByteArray>()
            val storeable = storableFormatConverter.toStorageFormat(document)
            lokalStorageAttachment[user + "_" + documentId] = storeable
        }
        every { storage[any(), any()] } answers {
            val user = firstArg<String>()
            val documentId = secondArg<String>()
            lokalStorageAttachment[user + "_" + documentId] ?: error("Noe gikk galt")
        }

        return storage
    }

    @Bean
    @Primary
    fun virusScanService(): VirusScanService = mockk(relaxed = true)
}
