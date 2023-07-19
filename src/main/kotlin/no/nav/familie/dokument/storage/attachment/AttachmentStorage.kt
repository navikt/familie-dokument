package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import java.io.ByteArrayInputStream

class AttachmentStorage constructor(
    private val delegate: EncryptedStorage,
    private val storableFormatConverter: AttachmentToStorableFormatConverter,
) :
    Storage<ByteArray, ByteArray> {

    override fun put(directory: String, key: String, data: ByteArray) {
        val storeable = storableFormatConverter.toStorageFormat(data)
        delegate.put(directory, key, ByteArrayInputStream(storeable))
    }

    override operator fun get(directory: String, key: String): ByteArray {
        return delegate[directory, key]
    }

    override fun delete(directory: String, key: String) {
        throw NotImplementedError("AttachmentStorage::delete(): Ikke implementert")
    }
}
