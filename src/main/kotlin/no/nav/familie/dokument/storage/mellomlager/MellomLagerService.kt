package no.nav.familie.dokument.storage.mellomlager

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration.Companion.STONAD_ENCRYPTED_STORAGE
import no.nav.familie.dokument.storage.google.GcpRateLimitException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class MellomLagerService internal constructor(
    @Qualifier(STONAD_ENCRYPTED_STORAGE)
    private val delegate: EncryptedStorage,
) : Storage<String, String> {

    @Retryable(
        includes = [GcpRateLimitException::class],
        maxRetries = 3,
        delay = 1000,
    )
    override fun put(directory: String, key: String, data: String) {
        delegate.put(directory, key, ByteArrayInputStream(data.toByteArray()))
    }

    override fun get(directory: String, key: String): String {
        return String(delegate[directory, key])
    }

    override fun delete(directory: String, key: String) {
        delegate.delete(directory, key)
    }
}
