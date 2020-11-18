package no.nav.familie.dokument.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

@Service
class PdfService() {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    val contentRoot = Path.of("./content/")

    fun lagPdf(html: String, css: String?): ByteArray {
        log.debug("Generer pdf")

        val dokument = Jsoup.parse(html)
        val pdf =
            if (css != null) lagPdfMedCss(dokument, css)
            else genererPdf(dokument, "")
        return pdf
    }

    private fun lagPdfMedCss(xhtmlDokument: Document, css: String): ByteArray {
        val cssNavn = UUID.randomUUID()
        xhtmlDokument.head().append("<link rel='stylesheet' type='text/css' href='${cssNavn}.css'>")
        val tempFolder = lagMidlerditigMappe()
        try {
            val midlertidigCssFile = lagCssFil(tempFolder.absolutePath, cssNavn, css)
            val pdf = genererPdf(xhtmlDokument, tempFolder.toURI().toString())
            midlertidigCssFile.delete()
            return pdf
        } catch (e: IOException) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
    }

    private fun lagMidlerditigMappe(): File {
        val tempFolderPath = contentRoot.toString() + "/temp"
        val tempFolder = File(tempFolderPath)
        tempFolder.mkdir()
        return tempFolder
    }

    private fun lagCssFil(tempFolderPath: String, cssNavn: UUID, css: String): File {
        val midlertidigCssFil = File("${tempFolderPath}/${cssNavn}.css")
        midlertidigCssFil.writeBytes(css.toByteArray(StandardCharsets.UTF_8))
        return midlertidigCssFil
    }

    private fun getFont(contentRoot: Path, fontName: String): Path {
        return contentRoot.resolve("fonts/$fontName")
    }

    fun genererPdf(dokument: Document?, cssPath: String = ""): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val w3cDokument = W3CDom().fromJsoup(dokument)
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
                .withW3cDocument(w3cDokument, cssPath)
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()
        } catch (e: IOException) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
        return outputStream.toByteArray()
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