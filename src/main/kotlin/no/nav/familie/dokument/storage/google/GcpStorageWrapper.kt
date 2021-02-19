package no.nav.familie.dokument.storage.google

import no.nav.familie.dokument.storage.Storage
import java.io.InputStream

class GcpStorageWrapper(val storage: GcpStorage, val mediaTypeValue: String): Storage<InputStream, ByteArray> {

    override fun put(directory: String, key: String, data: InputStream) {
        storage.put(directory, key, data, mediaTypeValue)
    }

    override operator fun get(directory: String, key: String): ByteArray {
        return storage.get(directory, key)
    }

    override fun delete(directory: String, key: String) {
        storage.delete(directory, key)
    }
}