package no.nav.familie.dokument.storage

import no.nav.familie.dokument.BadRequestCode
import no.nav.familie.dokument.InvalidDocumentSize
import no.nav.familie.dokument.pdf.PdfService
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.virusscan.VirusScanService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("familie/dokument/api/mapper", "api/mapper")
@RequiredIssuers(
    ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"]),
)
class StorageController(
    val storage: AttachmentStorage,
    val virusScanService: VirusScanService,
    val contextHolder: TokenValidationContextHolder,
    @Value("\${attachment.max.size.mb}") val maxFileSizeInMb: Int,
    val hasher: Hasher,
    val pdfService: PdfService,
) {

    // / TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @PostMapping(
        path = ["{bucket}"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun addAttachment(
        @PathVariable("bucket") bucket: String,
        @RequestParam("file") multipartFile: MultipartFile,
    ): ResponseEntity<Map<String, String>> {
        if (multipartFile.isEmpty) {
            throw InvalidDocumentSize(BadRequestCode.DOCUMENT_MISSING)
        }

        val bytes = multipartFile.bytes
        val maxFileSizeInBytes = maxFileSizeInMb * 1024 * 1024
        log.debug("Dokument lastet opp med størrelse (bytes): " + bytes.size)

        if (bytes.size > maxFileSizeInBytes) {
            throw InvalidDocumentSize(BadRequestCode.IMAGE_TOO_LARGE)
        }

        virusScanService.scan(bytes, multipartFile.originalFilename)

        val directory = hasher.lagFnrHash(contextHolder.hentFnr())

        val uuid = UUID.randomUUID().toString()

        storage.put(directory, uuid, bytes)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("dokumentId" to uuid, "filnavn" to multipartFile.originalFilename))
    }

    @PostMapping(
        path = ["/merge/{bucket}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun mergeAndStoreDocuments(
        @PathVariable("bucket") bucket: String,
        @RequestBody documentList: List<UUID>,
    ): ResponseEntity<Map<String, String>> {
        if (documentList.isEmpty()) {
            throw InvalidDocumentSize(BadRequestCode.DOCUMENT_MISSING)
        }

        val directory = hasher.lagFnrHash(contextHolder.hentFnr())

        val dokumenter = documentList.map { storage.get(directory, it.toString()) }

        val mergedeDokumenter = pdfService.mergeDokumenter(dokumenter)

        val uuid = UUID.randomUUID().toString()

        storage.put(directory, uuid, mergedeDokumenter)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("dokumentId" to uuid))
    }

    @GetMapping(path = ["{bucket}/{dokumentId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAttachmentRessurs(
        @PathVariable("bucket") bucket: String,
        @PathVariable("dokumentId") dokumentId: String,
    ): ResponseEntity<Ressurs<ByteArray>> {
        return ResponseEntity.ok(Ressurs.success(getAttachment(bucket, dokumentId)))
    }

    // / TODO: "bucket"-path brukes ikke ennå. "familievedlegg" brukes alltid
    @GetMapping(path = ["{bucket}/{dokumentId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getAttachment(
        @PathVariable("bucket") bucket: String,
        @PathVariable("dokumentId") dokumentId: String,
    ): ByteArray {
        val directory = hasher.lagFnrHash(contextHolder.hentFnr())
        val data = storage[directory, dokumentId]
        log.debug("Loaded file $dokumentId")
        return data
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
