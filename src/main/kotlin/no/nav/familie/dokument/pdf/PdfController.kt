package no.nav.familie.dokument.pdf

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api")
@Unprotected
class PdfController(val pdfService: PdfService) {

    @PostMapping("/html-til-pdf")
    fun lagPdf(@RequestBody html: String): ByteArray {
        return pdfService.lagPdf(html)
    }
}
