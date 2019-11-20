package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(EncryptedStorageConfiguration::class)
class AttachmentConfiguration {

    @Bean
    internal fun converter(imageConversionService: ImageConversionService): AttachmentToStorableFormatConverter {
        return AttachmentToStorableFormatConverter(imageConversionService)
    }

    @Bean
    fun attachmentStorage(
            @Autowired storage: EncryptedStorage,
            storableFormatConverter: AttachmentToStorableFormatConverter): AttachmentStorage {
        return AttachmentStorage(storage, storableFormatConverter)
    }
}
