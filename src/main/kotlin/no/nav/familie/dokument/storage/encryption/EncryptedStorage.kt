package no.nav.familie.dokument.storage.encryption

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.google.GcpStorageWrapper
import no.nav.familie.dokument.storage.hentFnr
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import java.io.InputStream

class EncryptedStorage constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val delegate: GcpStorageWrapper,
    private val encryptor: Encryptor
) : Storage<InputStream, ByteArray> {

    override fun put(directory: String, key: String, data: InputStream) {
        delegate.put(directory, key, encryptor.encryptedStream(contextHolder.hentFnr(), data))
    }

    override operator fun get(directory: String, key: String): ByteArray {
        return delegate[directory, key].let { encryptor.decrypt(contextHolder.hentFnr(), it) }
    }

    override fun delete(directory: String, key: String) {
        delegate.delete(directory, key)
    }
}
