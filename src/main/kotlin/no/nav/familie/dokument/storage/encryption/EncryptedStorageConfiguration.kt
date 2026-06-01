package no.nav.familie.dokument.storage.encryption

import no.nav.familie.dokument.storage.google.GcpStorageConfiguration.Companion.ATTACHMENT_GCP_STORAGE
import no.nav.familie.dokument.storage.google.GcpStorageConfiguration.Companion.STONAD_GCP_STORAGE
import no.nav.familie.dokument.storage.google.GcpStorageWrapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class EncryptedStorageConfiguration {
    @Bean
    internal fun encryptor(secretKeyProvider: SecretKeyProvider): Encryptor = Encryptor(secretKeyProvider)

    @Bean
    internal fun secretKeyProvider(
        @Value("\${FAMILIE_DOKUMENT_STORAGE_ENCRYPTION_PASSWORD}") passphrase: String,
    ): SecretKeyProvider = SecretKeyProvider(passphrase)

    @Bean(ATTACHMENT_ENCRYPTED_STORAGE)
    internal fun attachmentEncryptedStorage(
        @Qualifier(ATTACHMENT_GCP_STORAGE) storage: GcpStorageWrapper,
        encryptor: Encryptor,
    ): EncryptedStorage = EncryptedStorage(storage, encryptor)

    @Profile("!local && !local-fd")
    @Bean(STONAD_ENCRYPTED_STORAGE)
    internal fun stonadEncryptedStorage(
        @Qualifier(STONAD_GCP_STORAGE) storage: GcpStorageWrapper,
        encryptor: Encryptor,
    ): EncryptedStorage = EncryptedStorage(storage, encryptor)

    companion object {
        const val ATTACHMENT_ENCRYPTED_STORAGE = "attachmentEncryptedStorage"
        const val STONAD_ENCRYPTED_STORAGE = "stonadEncryptedStorage"
    }
}
