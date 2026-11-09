package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.InvalidDocumentFormat
import no.nav.familie.dokument.InvalidImageDimensions
import no.nav.familie.dokument.TestUtil.toByteArray
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal class ImageConversionServiceTest {
    val imageConversionService = ImageConversionService()

    @Test
    internal fun `convert jpg`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/jpg_dummy.jpg"), Format.JPEG)
        assertPdfMedEttBilde(pdfBytes, bredde = 970, høyde = 1374)
    }

    @Test
    internal fun `convert png`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_dummy.png"), Format.PNG)
        assertPdfMedEttBilde(pdfBytes, bredde = 1256, høyde = 1564)
    }

    @Test
    internal fun `convert png - rotert`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_dummy_rotated.png"), Format.PNG)
        assertPdfMedEttBilde(pdfBytes, bredde = 1256, høyde = 1564)
    }

    @Test
    internal fun `convert png av type 0`() {
        val pdfBytes = imageConversionService.convert(toByteArray("dummy/png_type_0.png"), Format.PNG)
        assertPdfMedEttBilde(pdfBytes, bredde = 567, høyde = 850)
    }

    @Test
    internal fun `convert png file too small`() {
        assertThatThrownBy { imageConversionService.convert(toByteArray("dummy/300_300px.png"), Format.PNG) }
            .isInstanceOf(InvalidImageDimensions::class.java)
            .hasMessage("CODE=IMAGE_DIMENSIONS_TOO_SMALL")
    }

    @Test
    internal fun `bilde med for mange piksler avvises uten å pakkes ut`() {
        val bombe = toByteArray("dummy/dekompresjonsbombe-25000x25000.png")
        assertThat(bombe).hasSizeLessThan(100_000)

        assertThatThrownBy { imageConversionService.convert(bombe, Format.PNG) }
            .isInstanceOf(InvalidImageDimensions::class.java)
            .hasMessage("CODE=IMAGE_DIMENSIONS_TOO_LARGE")
    }

    @Test
    internal fun `stort bilde leses inn nedskalert`() {
        val bilde = imageConversionService.lesBilde(png(7500, 6000))

        assertThat(bilde.width).isEqualTo(3750)
        assertThat(bilde.height).isEqualTo(3000)
    }

    @Test
    internal fun `vanlig mobilbilde leses inn i full størrelse`() {
        val bilde = imageConversionService.lesBilde(png(4000, 3000))

        assertThat(bilde.width).isEqualTo(4000)
        assertThat(bilde.height).isEqualTo(3000)
    }

    @Test
    internal fun `stort bilde konverteres til pdf`() {
        val pdfBytes = imageConversionService.convert(png(7500, 6000), Format.PNG)

        assertPdfMedEttBilde(pdfBytes, bredde = 3000, høyde = 3750)
    }

    @Test
    internal fun `subsampling holder bildet minst like stort som A4 i 300 dpi`() {
        val a4 = 2480L * 3508L
        assertThat(ImageConversionService.subsamplingFaktor(1)).isEqualTo(1)
        assertThat(ImageConversionService.subsamplingFaktor(4 * a4 - 1)).isEqualTo(1)
        assertThat(ImageConversionService.subsamplingFaktor(4 * a4)).isEqualTo(2)
        assertThat(ImageConversionService.subsamplingFaktor(200_000_000L)).isEqualTo(4)
        assertThat(ImageConversionService.subsamplingFaktor(ImageConversionService.MAKS_ANTALL_PIKSLER)).isEqualTo(5)
    }

    @Test
    internal fun `uleselig bilde avvises`() {
        val søppel = byteArrayOf(1, 2, 3, 4, 5)

        assertThatThrownBy { imageConversionService.convert(søppel, Format.JPEG) }
            .isInstanceOf(InvalidDocumentFormat::class.java)
    }

    private fun png(
        bredde: Int,
        høyde: Int,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(bredde, høyde, BufferedImage.TYPE_BYTE_BINARY), "png", out)
        return out.toByteArray()
    }

    private fun assertPdfMedEttBilde(
        pdfBytes: ByteArray,
        bredde: Int,
        høyde: Int,
    ) {
        Loader.loadPDF(pdfBytes).use { pdf ->
            assertThat(pdf.numberOfPages).isEqualTo(1)
            val ressurser = pdf.getPage(0).resources
            val bilde = ressurser.getXObject(ressurser.xObjectNames.single())
            assertThat(bilde).isInstanceOfSatisfying(PDImageXObject::class.java) {
                assertThat(it.width).isEqualTo(bredde)
                assertThat(it.height).isEqualTo(høyde)
            }
        }
    }
}
