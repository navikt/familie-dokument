package no.nav.familie.dokument.storage

interface Storage<T, U> {

    fun put(directory: String, key: String, data: T)

    operator fun get(directory: String, key: String): U

    fun delete(directory: String, key: String)
}
