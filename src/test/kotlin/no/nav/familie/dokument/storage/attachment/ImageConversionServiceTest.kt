package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.TestUtil.toByteArray
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled
internal class ImageConversionServiceTest {

    val imageConversionService = ImageConversionService()

    @Test
    internal fun `convert jpg`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/jpg_dummy.jpg"))
        File("./jpg_pdf.pdf").writeBytes(pdfBytes)
    }

    @Test
    internal fun `convert png`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_dummy.png"))
        File("./png_pdf.pdf").writeBytes(pdfBytes)
    }
}