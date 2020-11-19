package no.nav.familie.dokument.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.File
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

    private fun getFont(fontName: String): File {
        return File(this::class.java.classLoader.getResource("/fonts/$fontName")!!.file)
    }

    fun genererPdf(w3cDokument: Document, outputStream: ByteArrayOutputStream) {
        val builder = PdfRendererBuilder()
        try {
            builder
                .useFont(
                    getFont("SourceSansPro-Regular.ttf"),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.NORMAL,
                    true
                )
                .useFont(
                    getFont("SourceSansPro-Bold.ttf"),
                    "Source Sans Pro",
                    700,
                    BaseRendererBuilder.FontStyle.OBLIQUE,
                    true
                )
                .useFont(
                    getFont("SourceSansPro-It.ttf"),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.ITALIC,
                    true
                )
                .useColorProfile(colorProfile)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                .withW3cDocument(w3cDokument, "")
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()
        } catch (e: IOException) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
    }

    companion object {

        @get:Throws(IOException::class)
        val colorProfile: ByteArray
            get() {
                val cpr = ClassPathResource("sRGB.icc")
                return FileCopyUtils.copyToByteArray(cpr.inputStream)
            }
    }
}