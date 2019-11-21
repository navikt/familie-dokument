package no.nav.familie.dokument.storage

import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID

@RestController
@RequestMapping("api/vedlegg")
//@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@Unprotected
class StorageController(@Autowired val storage: AttachmentStorage,
                        @Autowired val contextHolder: TokenValidationContextHolder,
                        @Value("\${attachment.max.size.mb}") val maxFileSize: Int) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(IOException::class)
    fun addAttachment(@RequestParam("file") multipartFile: MultipartFile): Map<String, String> {

        if (multipartFile.isEmpty) {
            return emptyMap()
        }

        val bytes = multipartFile.bytes
        log.debug("Vedlegg med lastet opp med størrelse: " + bytes.size)

        if (bytes.size > maxFileSize) {
            throw IllegalArgumentException(HttpStatus.PAYLOAD_TOO_LARGE.toString())
        }

        val directory = contextHolder.hentFnr()

        val uuid = UUID.randomUUID().toString()

        val file = ByteArrayInputStream(bytes)

        storage.put(directory, uuid, file)

        return mapOf("vedleggsId" to uuid, "filnavn" to multipartFile.name)
    }

    @GetMapping(path = ["{vedleggsId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getAttachment(@PathVariable("vedleggsId") vedleggsId: String): ByteArray {
        val directory = contextHolder.hentFnr()
        val data = storage[directory, vedleggsId].orElse(null)
        log.debug("Loaded file with {}", data)
        return data
    }

    companion object {

        private val log = LoggerFactory.getLogger(StorageController::class.java)
    }

}
