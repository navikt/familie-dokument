package no.nav.familie.dokument.storage

import no.nav.familie.dokument.pdf.HtmlDokument
import no.nav.familie.dokument.pdf.PdfService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/pdf")
@Unprotected
class PdfController(val pdfService: PdfService) {

    @PostMapping
    fun lagPdf(@RequestBody htmlDokument: HtmlDokument): ResponseEntity<*> {
        return ResponseEntity(
            pdfService.lagPdf(htmlDokument.html, htmlDokument.css),
            pdfService.lagPdfHeadere("dokument"),
            HttpStatus.OK
        )
    }
}
