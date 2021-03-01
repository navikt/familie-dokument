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
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/jpg_dummy.jpg"), Format.JPEG)
        File("./jpg_pdf.pdf").writeBytes(pdfBytes)
    }

    @Test
    internal fun `convert png`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_dummy.png"), Format.PNG)
        File("./png_pdf.pdf").writeBytes(pdfBytes)
    }

    @Test
    internal fun `convert png - rotert`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_dummy_rotated.png"), Format.PNG)
        File("./png_rotated_pdf.pdf").writeBytes(pdfBytes)
    }

    @Test
    internal fun `convert png av type 0`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_type_0.png"), Format.PNG)
        File("./png_type0_pdf.pdf").writeBytes(pdfBytes)
    }

}