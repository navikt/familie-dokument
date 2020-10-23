package no.nav.familie.dokument.storage.google

import java.io.InputStream

class GcpStorageWrapper(val storage: GcpStorage, val mediaTypeValue : String) {

    fun put(directory: String, key: String, data: InputStream) {
        storage.put(directory, key, data, mediaTypeValue)
    }

    operator fun get(directory: String, key: String): ByteArray {
        return storage.get(directory, key)
    }

    fun delete(directory: String, key: String) {
        storage.delete(directory, key)
    }
}