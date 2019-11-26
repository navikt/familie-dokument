package no.nav.familie.dokument.storage.encryption

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.hentFnr
import no.nav.familie.dokument.storage.s3.S3Storage
import no.nav.security.token.support.core.context.TokenValidationContextHolder

import java.io.InputStream
import java.util.Optional

class EncryptedStorage constructor(private val contextHolder: TokenValidationContextHolder,
                                            private val delegate: S3Storage,
                                            private val encryptor: Encryptor) : Storage {

    override fun put(directory: String, key: String, data: InputStream) {
        delegate.put(directory, key, encryptor.encryptedStream(contextHolder.hentFnr(), data))
    }

    override operator fun get(directory: String, key: String): Optional<ByteArray> {
        return delegate.get(directory, key)
                .map { content -> encryptor.decrypt(contextHolder.hentFnr(), content) }
    }

}
