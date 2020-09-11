package no.nav.familie.dokument.storage

import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.*

@RestController
@RequestMapping("api/mapper")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class StorageController(@Autowired val storage: AttachmentStorage,
                        @Autowired val contextHolder: TokenValidationContextHolder,
                        @Value("\${attachment.max.size.mb}") val maxFileSizeInMb: Int)  {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @PostMapping(path = ["{bucket}"],
                 consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addAttachment(@PathVariable("bucket")bucket: String,
                      @RequestParam("file") multipartFile: MultipartFile): ResponseEntity<Map<String, String>> {

        if (multipartFile.isEmpty) {
            log.info("Dokumentet som lastes opp er tomt - size: [{}] ", multipartFile.size)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }

        val bytes = multipartFile.bytes
        val maxFileSizeInBytes = maxFileSizeInMb*1024*1024
        log.debug("Dokument lastet opp med størrelse (bytes): " + bytes.size)

        if (bytes.size > maxFileSizeInBytes) {
            throw IllegalArgumentException(HttpStatus.PAYLOAD_TOO_LARGE.toString())
        }

        val directory = contextHolder.hentFnr()

        val uuid = UUID.randomUUID().toString()

        val file = ByteArrayInputStream(bytes)
        try {
            storage.put(directory, uuid, file)
        } catch (e: RuntimeException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        return ResponseEntity.ok(mapOf("dokumentId" to uuid, "filnavn" to multipartFile.name))
    }

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @GetMapping(path = ["{bucket}/{dokumentId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAttachment(@PathVariable("bucket") bucket: String,
                      @PathVariable("dokumentId") dokumentId: String): ResponseEntity<Ressurs<ByteArray>> {
        val directory = contextHolder.hentFnr()
        return try {
            val data = storage[directory, dokumentId]
            log.debug("Loaded file with {}", data)
            ResponseEntity.ok(Ressurs.Companion.success(data))
        } catch (e: RuntimeException) {
            secureLogger.error("Henting av vedlegg feilet", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Ressurs.Companion.failure(e.message))
        }
    }

    @Unprotected
    @GetMapping(path = ["ping"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun ping(): String {
        return "Kontakt med familie-dokument"
    }

    companion object {
        private val log = LoggerFactory.getLogger(StorageController::class.java)
    }

}
