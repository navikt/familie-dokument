package no.nav.familie.dokument.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Path

@Service
class PdfService @Autowired constructor(
    @param:Value("\${pdf.path.content.root}") private val contentRoot: Path
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagPdf(html: String): ByteArray {
        log.debug("Generer pdf")

        val pdfOutputStream = ByteArrayOutputStream()
        val w3cDokument = W3CDom().fromJsoup(Jsoup.parse(html))
        genererPdf(w3cDokument, pdfOutputStream)

        return pdfOutputStream.toByteArray()
    }

    private fun getFont(contentRoot: Path, fontName: String): Path {
        return contentRoot.resolve("fonts/$fontName")
    }

    fun genererPdf(w3cDokument: Document, outputStream: ByteArrayOutputStream) {
        val builder = PdfRendererBuilder()
        try {
            builder
                .useFont(
                    getFont(contentRoot, "SourceSansPro-Regular.ttf").toFile(),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.NORMAL,
                    true
                )
                .useFont(
                    getFont(contentRoot, "SourceSansPro-Bold.ttf").toFile(),
                    "Source Sans Pro",
                    700,
                    BaseRendererBuilder.FontStyle.OBLIQUE,
                    true
                )
                .useFont(
                    getFont(contentRoot, "SourceSansPro-It.ttf").toFile(),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.ITALIC,
                    true
                )
                //.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                .withW3cDocument(w3cDokument, "")
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()
        } catch (e: IOException) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
    }

    fun lagPdfHeadere(malNavn: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        val filename = "$malNavn.pdf"
        val contentDisposition = ContentDisposition.builder("attachment")
        contentDisposition.filename(filename)
        headers.contentDisposition = contentDisposition.build()
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0"
        return headers
    }
}