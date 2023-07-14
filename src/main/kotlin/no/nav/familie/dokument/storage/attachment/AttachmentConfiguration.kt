package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration.Companion.ATTACHMENT_ENCRYPTED_STORAGE
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile

@Configuration
@Import(EncryptedStorageConfiguration::class)
@Profile("!local")
class AttachmentConfiguration {

    @Bean
    internal fun converter(imageConversionService: ImageConversionService, flattenPdfService: FlattenPdfService): AttachmentToStorableFormatConverter {
        return AttachmentToStorableFormatConverter(imageConversionService, flattenPdfService)
    }

    @Bean
    fun attachmentStorage(
        @Qualifier(ATTACHMENT_ENCRYPTED_STORAGE) storage: EncryptedStorage,
        storableFormatConverter: AttachmentToStorableFormatConverter
    ): AttachmentStorage {
        return AttachmentStorage(storage, storableFormatConverter)
    }
}
