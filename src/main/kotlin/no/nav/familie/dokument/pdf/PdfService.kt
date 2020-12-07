package no.nav.familie.dokument.pdf

import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@Service
class PdfService {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagPdf(html: String): ByteArray {
        log.debug("Generer pdf")

        val pdfOutputStream = ByteArrayOutputStream()
        val w3cDokument = W3CDom().fromJsoup(Jsoup.parse(html))
        genererPdf(w3cDokument, pdfOutputStream)

        return pdfOutputStream.toByteArray()
    }

    private class FontSupplier(val fontName: String) : FSSupplier<InputStream> {

        override fun supply(): InputStream {
            return ClassPathResource("/fonts/$fontName").inputStream
        }
    }

    fun genererPdf(w3cDokument: Document, outputStream: ByteArrayOutputStream) {
        val builder = PdfRendererBuilder()
        try {
            builder
                .useFont(
                    FontSupplier("SourceSansPro-Regular.ttf"),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.NORMAL,
                    true
                )
                .useFont(
                    FontSupplier("SourceSansPro-Bold.ttf"),
                    "Source Sans Pro",
                    700,
                    BaseRendererBuilder.FontStyle.OBLIQUE,
                    true
                )
                .useFont(
                    FontSupplier("SourceSansPro-It.ttf"),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.ITALIC,
                    true
                )
                .useColorProfile(colorProfile)
                .useSVGDrawer(BatikSVGDrawer())
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

        val colorProfile: ByteArray
            get() {
                val cpr = ClassPathResource("sRGB.icc")
                return FileCopyUtils.copyToByteArray(cpr.inputStream)
            }
    }
}