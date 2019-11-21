package no.nav.familie.dokument.storage.encryption

import no.nav.familie.dokument.storage.s3.S3Storage
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class EncryptedStorageConfiguration {

    @Bean
    internal fun encryptor(secretKeyProvider: SecretKeyProvider): Encryptor {
        return Encryptor(secretKeyProvider)
    }

    @Bean
    internal fun secretKeyProvider(
            @Value("\${FAMILIE_DOKUMENT_STORAGE_ENCRYPTION_PASSWORD}") passphrase: String): SecretKeyProvider {
        return SecretKeyProvider(passphrase)
    }

    @Bean
    internal fun encryptedStorage(@Autowired contextHolder: TokenValidationContextHolder, @Autowired storage: S3Storage, encryptor: Encryptor): EncryptedStorage {
        return EncryptedStorage(contextHolder, storage, encryptor)
    }

}
