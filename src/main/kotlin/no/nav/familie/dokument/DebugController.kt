package no.nav.familie.dokument

import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.google.GcpStorageConfiguration.Companion.ATTACHMENT_GCP_STORAGE
import no.nav.familie.dokument.storage.google.GcpStorageConfiguration.Companion.STONAD_GCP_STORAGE
import no.nav.familie.dokument.storage.google.GcpStorageWrapper
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream

@RestController
@RequestMapping("debugger")
@Profile("preprod")
@Unprotected
class DebugController(@Qualifier(STONAD_GCP_STORAGE) val soknadGcpStorage: GcpStorageWrapper,
@Qualifier(ATTACHMENT_GCP_STORAGE) val attachmentGcpStorage: GcpStorageWrapper) {

    @GetMapping(path = ["testGcpSoknad/{directory}/{filename}"],
    produces = [MediaType.APPLICATION_JSON_VALUE])
    fun testGcpSoknad(@PathVariable("directory") directory: String,
                          @PathVariable("filename") filename: String,
                          @RequestParam("content") content: String
    ): ByteArray{
        val jsonData= "{content: ${content}}"
        soknadGcpStorage.put(directory, filename, ByteArrayInputStream(content.toByteArray()))
        return soknadGcpStorage[directory, filename]
    }

    @GetMapping(path = ["testGcpAttachment/{directory}/{filename}"],
    produces = [MediaType.APPLICATION_PDF_VALUE])
    fun testGcpAttachment(@PathVariable("directory") directory: String,
                      @PathVariable("filename") filename: String
    ): ByteArray{
        val pdfData = DebugController::class.java.getResource("/test_gcp_storage_attachment.pdf").readBytes()
        soknadGcpStorage.put(directory, filename, ByteArrayInputStream(pdfData))
        return soknadGcpStorage[directory, filename]
    }

}