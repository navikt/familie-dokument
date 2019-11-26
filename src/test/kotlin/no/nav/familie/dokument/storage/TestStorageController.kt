package no.nav.familie.dokument.storage

import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/dokument")
@Unprotected
@Profile("dev")
class TestStorageController(
        @Autowired storage: AttachmentStorage,
        @Autowired contextHolder: TokenValidationContextHolder,
        @Value("\${attachment.max.size.mb}") maxFileSizeInMb: Int) :
        AbstractStorageController(storage, contextHolder::hentFnr, maxFileSizeInMb) {

    @PostMapping(path = ["{bucket}"],
                 consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    override fun addAttachment(@PathVariable("bucket") bucket: String,
                               @RequestParam("file") multipartFile: MultipartFile): Map<String, String> {
        return super.addAttachment(bucket, multipartFile)
    }

    @GetMapping(path = ["{bucket}/{vedleggsId}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    override fun getAttachment(
            @PathVariable("bucket") bucket: String,
            @PathVariable("vedleggsId") vedleggsId: String): ByteArray {
        return super.getAttachment(bucket, vedleggsId)
    }
}