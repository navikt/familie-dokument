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

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @PostMapping(path = ["{bucket}"],
                 consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addAttachment(@PathVariable("bucket")bucket: String,
                      @RequestParam("file") multipartFile: MultipartFile): Map<String, String> {

        if (multipartFile.isEmpty) {
            return emptyMap()
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

        storage.put(directory, uuid, file)

        return mapOf("dokumentId" to uuid, "filnavn" to multipartFile.name)
    }

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @GetMapping(path = ["{bucket}/{dokumentId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getAttachment(@PathVariable("bucket") bucket: String,
                      @PathVariable("dokumentId") dokumentId: String): ResponseEntity<Ressurs<ByteArray>> {
        val directory = contextHolder.hentFnr()
        val data = storage[directory, dokumentId].orElse(null)
        log.debug("Loaded file with {}", data)
        return ResponseEntity.ok(Ressurs.Companion.success(data))
    }

    @Unprotected
    @GetMapping(path = ["ping"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun ping(): String {
        return "pong"
    }

    companion object {
        private val log = LoggerFactory.getLogger(StorageController::class.java)
    }

}
