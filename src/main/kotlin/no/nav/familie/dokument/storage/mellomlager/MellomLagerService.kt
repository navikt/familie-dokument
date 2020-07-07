package no.nav.familie.dokument.storage.mellomlager

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class MellomLagerService internal constructor(private val delegate: EncryptedStorage) : Storage<String, String> {

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