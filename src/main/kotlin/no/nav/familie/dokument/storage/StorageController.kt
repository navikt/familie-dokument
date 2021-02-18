package no.nav.familie.dokument.storage

import no.nav.familie.dokument.InvalidDocumentSize
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.virusscan.VirusScanService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("familie/dokument/api/mapper", "api/mapper")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class StorageController(val storage: AttachmentStorage,
                        val virusScanService: VirusScanService,
                        val contextHolder: TokenValidationContextHolder,
                        @Value("\${attachment.max.size.mb}") val maxFileSizeInMb: Int,
                        val hasher: Hasher) {

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @PostMapping(path = ["{bucket}"],
                 consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addAttachment(@PathVariable("bucket") bucket: String,
                      @RequestParam("file") multipartFile: MultipartFile): ResponseEntity<Map<String, String>> {

        if (multipartFile.isEmpty) {
            throw InvalidDocumentSize("Dokumentet som lastes opp er tomt - size: [${multipartFile.size}] ")
        }

        val bytes = multipartFile.bytes
        val maxFileSizeInBytes = maxFileSizeInMb * 1024 * 1024
        log.debug("Dokument lastet opp med størrelse (bytes): " + bytes.size)

        if (bytes.size > maxFileSizeInBytes) {
            throw InvalidDocumentSize("Dokumentstørrelsen(${bytes.size} bytes) overstiger grensen(${maxFileSizeInMb} mb)")
        }

        //TODO slett try catch, kun for bruk i test for å sjekke om dette virker
        try {
            virusScanService.scan(bytes, multipartFile.name)
            log.info("VirusScan ok")
        } catch (e: Exception) {
            log.warn("Feilet scanning", e)
        }

        val directory = hasher.lagFnrHash(contextHolder.hentFnr())

        val uuid = UUID.randomUUID().toString()

        storage.put(directory, uuid, bytes)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("dokumentId" to uuid, "filnavn" to multipartFile.name))
    }

    /// TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @GetMapping(path = ["{bucket}/{dokumentId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAttachment(@PathVariable("bucket") bucket: String,
                      @PathVariable("dokumentId") dokumentId: String): ResponseEntity<Ressurs<ByteArray>> {
        val directory = hasher.lagFnrHash(contextHolder.hentFnr())
        val data = storage[directory, dokumentId]
        log.debug("Loaded file $dokumentId")
        return ResponseEntity.ok(Ressurs.success(data))
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
