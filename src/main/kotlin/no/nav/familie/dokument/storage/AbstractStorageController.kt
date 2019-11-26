package no.nav.familie.dokument.storage

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

abstract class AbstractStorageController(private val storage: Storage,
                                         private val hentFnr : ()->String,
                                         private val maxFileSizeInMb: Int)  {

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    open protected fun addAttachment(bucket: String, multipartFile: MultipartFile): Map<String, String> {

        if (multipartFile.isEmpty) {
            return emptyMap()
        }

        val bytes = multipartFile.bytes
        val maxFileSizeInBytes = maxFileSizeInMb*1024*1024
        log.debug("Vedlegg med lastet opp med størrelse: " + bytes.size)

        if (bytes.size > maxFileSizeInBytes) {
            throw IllegalArgumentException(HttpStatus.PAYLOAD_TOO_LARGE.toString())
        }

        val directory = hentFnr()

        val uuid = UUID.randomUUID().toString()

        val file = ByteArrayInputStream(bytes)

        storage.put(directory, uuid, file)

        return mapOf("vedleggsId" to uuid, "filnavn" to multipartFile.name)
    }

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    open protected fun getAttachment(bucket: String, vedleggsId: String): ByteArray {
        val directory = hentFnr()
        val data = storage[directory, vedleggsId].orElse(null)
        log.debug("Loaded file with {}", data)
        return data
    }

    companion object {
        private val log = LoggerFactory.getLogger(AbstractStorageController::class.java)
    }

}
