package no.nav.familie.dokument

import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("debugger")
@Profile("preprod")
@Unprotected
class DebugController(val soknadStorage: MellomLagerService, val attachmentStorage: AttachmentStorage) {

    @GetMapping("testSoknadStorage/{directory}/{filename}")
    fun testSoknadStorage(@PathVariable("directory") directory: String,
                          @PathVariable("filename") filename: String,
                          @RequestParam("content") content: String
    ): String{
        soknadStorage.put(directory, filename, content)
        return soknadStorage[directory, filename]
    }
}