package no.nav.familie.dokument.storage.encryption

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.google.GcpStorageWrapper
import no.nav.familie.sikkerhet.EksternBrukerUtils.hentFnrFraToken
import java.io.InputStream

class EncryptedStorage(
    private val delegate: GcpStorageWrapper,
    private val encryptor: Encryptor,
) : Storage<InputStream, ByteArray> {
    override fun put(
        directory: String,
        key: String,
        data: InputStream,
    ) {
        delegate.put(directory, key, encryptor.encryptedStream(hentFnrFraToken(), data))
    }

    override operator fun get(
        directory: String,
        key: String,
    ): ByteArray =
        delegate[directory, key].let {
            encryptor.decrypt(hentFnrFraToken(), it)
        }

    override fun delete(
        directory: String,
        key: String,
    ) {
        delegate.delete(directory, key)
    }
}
