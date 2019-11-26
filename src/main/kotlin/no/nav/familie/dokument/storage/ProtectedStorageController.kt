package no.nav.familie.dokument.storage

import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/vedlegg")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class ProtectedStorageController(@Autowired val storage: AttachmentStorage,
                                 @Autowired val contextHolder: TokenValidationContextHolder,
                                 @Value("\${attachment.max.size.mb}") val maxFileSizeInMb: Int):
        AbstractStorageController(storage, contextHolder::hentFnr, maxFileSizeInMb)  {

        @PostMapping(path = ["{bucket}"],
                     consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
                     produces = [MediaType.APPLICATION_JSON_VALUE])
        override fun addAttachment(@PathVariable("bucket") bucket: String,
                                   @RequestParam("file") multipartFile: MultipartFile): Map<String, String> {
            return super.addAttachment(bucket, multipartFile)
        }




}
