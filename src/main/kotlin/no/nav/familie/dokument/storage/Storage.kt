package no.nav.familie.dokument.storage

import java.io.InputStream

interface Storage {

    fun put(directory: String, key: String, data: InputStream)

    operator fun get(directory: String, key: String): ByteArray?

}
