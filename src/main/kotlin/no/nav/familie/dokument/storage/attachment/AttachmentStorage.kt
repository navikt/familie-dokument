package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.storage.Storage
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import org.apache.commons.io.IOUtils

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException

class AttachmentStorage internal constructor(private val delegate: EncryptedStorage, private val storableFormatConverter: AttachmentToStorableFormatConverter) : Storage<InputStream, ByteArray> {

    override fun put(directory: String, key: String, data: InputStream) {
        val storeable = storableFormatConverter.toStorageFormat(toByteArray(data))
        delegate.put(directory, key, ByteArrayInputStream(storeable))
    }

    override operator fun get(directory: String, key: String): ByteArray {
        return delegate[directory, key]
    }

    private fun toByteArray(data: InputStream): ByteArray {
        try {
            return IOUtils.toByteArray(data)
        } catch (e: IOException) {
            throw RuntimeException("Kunnne ikke lese inputstream", e)
        }

    }

    override fun delete(directory: String, key: String) {
        throw NotImplementedError("Ikke implementert")
    }
}
