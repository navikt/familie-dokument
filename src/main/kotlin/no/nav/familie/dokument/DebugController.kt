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

    @GetMapping("testSoknadStorage/{directory}/{filename}")
    fun testSoknadStorage(@PathVariable("directory") directory: String,
                          @PathVariable("filename") filename: String,
                          @RequestParam("content") content: String
    ): String{
        soknadGcpStorage.put(directory, filename, ByteArrayInputStream(content.toByteArray()))
        return String(soknadGcpStorage[directory, filename])
    }
}