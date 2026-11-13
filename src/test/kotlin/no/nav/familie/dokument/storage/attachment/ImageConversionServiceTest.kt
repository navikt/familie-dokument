package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.InvalidDocumentFormat
import no.nav.familie.dokument.InvalidImageDimensions
import no.nav.familie.dokument.TestUtil.toByteArray
import no.nav.familie.dokument.storage.attachment.Testbilder.PNG_PALETT
import no.nav.familie.dokument.storage.attachment.Testbilder.PNG_RGB
import no.nav.familie.dokument.storage.attachment.Testbilder.PNG_RGBA
import no.nav.familie.dokument.storage.attachment.Testbilder.jpeg
import no.nav.familie.dokument.storage.attachment.Testbilder.jpegUtenBildedata
import no.nav.familie.dokument.storage.attachment.Testbilder.komprimertPngChunk
import no.nav.familie.dokument.storage.attachment.Testbilder.png
import no.nav.familie.dokument.storage.attachment.Testbilder.pngMedNuller
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

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
    internal fun `minneanslaget for jpeg tar med koeffisientene for hele bildet`() {
        // 7700x7700 i farge: 2 bytes x 3 kanaler per piksel til koeffisienter, pluss nedskalert bilde (3850x3850) x (3 + 4 + 4 + 1) bytes
        val minne = ImageConversionService.estimertMinneVedUtpakking(7700, 7700, bytesPerPiksel = 3, fargekanaler = 3, erJpeg = true)

        assertThat(minne).isEqualTo(2L * 7700 * 7700 * 3 + 3850L * 3850 * 12)
    }

    @Test
    internal fun `minneanslaget for png tar med tre rader i full bredde`() {
        // 6600x5272 med 16 bit RGBA blir ikke nedskalert: (8 + 4 + 4 + 1) bytes per piksel, pluss tre rader i full bredde
        val minne = ImageConversionService.estimertMinneVedUtpakking(6600, 5272, bytesPerPiksel = 8, fargekanaler = 4, erJpeg = false)

        assertThat(minne).isEqualTo(6600L * 5272 * 17 + 3L * 6600 * 8)
    }

    @Test
    internal fun `jpeg som krever for mye minne avvises uten å pakkes ut`() {
        val bombe = jpegUtenBildedata(10000, 9000, fargekanaler = 3)

        assertThatThrownBy { imageConversionService.convert(bombe, Format.JPEG) }
            .isInstanceOf(InvalidImageDimensions::class.java)
            .hasMessage("CODE=IMAGE_DIMENSIONS_TOO_LARGE")
    }

    @Test
    internal fun `png med like mange piksler bruker mindre minne og godtas`() {
        val bilde = imageConversionService.lesBilde(png(10000, 9000))

        assertThat(bilde.width).isEqualTo(3334)
        assertThat(bilde.height).isEqualTo(3000)
    }

    @Test
    internal fun `svært bredt bilde avvises fordi radene tar for mye minne`() {
        val bredt = pngMedNuller(33_000_000, 1, 16, PNG_RGBA)

        assertThatThrownBy { imageConversionService.convert(bredt, Format.PNG) }
            .isInstanceOf(InvalidImageDimensions::class.java)
            .hasMessage("CODE=IMAGE_DIMENSIONS_TOO_LARGE")
    }

    @Test
    internal fun `bredt bilde som bruker lite minne godtas`() {
        val bilde = imageConversionService.lesBilde(png(100_000, 1))

        assertThat(bilde.width).isEqualTo(100_000)
    }

    @Test
    internal fun `bilde med ødelagt header avvises som ugyldig format`() {
        val ødelagtPng = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10, 0, 0)

        assertThatThrownBy { imageConversionService.convert(ødelagtPng, Format.PNG) }
            .isInstanceOf(InvalidDocumentFormat::class.java)
    }

    // JDK-en begrenser selv hvor mye av komprimerte tekstfelter og fargeprofiler den pakker ut. Testen fanger opp om det endres.
    @ParameterizedTest
    @CsvSource("$PNG_PALETT, zTXt", "$PNG_PALETT, iCCP", "$PNG_RGB, zTXt")
    internal fun `komprimerte metadata i png gir ikke minnebombe`(
        fargetype: Int,
        chunktype: String,
    ) {
        val fireGiB = 4L shl 30
        val png = pngMedNuller(1000, 1000, 8, fargetype, ekstraChunks = listOf(komprimertPngChunk(chunktype, fireGiB)))

        assertPdfMedEttBilde(imageConversionService.convert(png, Format.PNG), bredde = 1000, høyde = 1000)
    }

    @Test
    internal fun `største tillatte png med 16 bit, alfakanal og interlacing leses inn nedskalert`() {
        val png = pngMedNuller(15800, 15800, 16, PNG_RGBA, interlaced = true)

        val bilde = imageConversionService.lesBilde(png)

        assertThat(bilde.width).isEqualTo(3160)
        assertThat(bilde.height).isEqualTo(3160)
    }

    @Test
    internal fun `png med 16 bit og alfakanal konverteres til pdf`() {
        val pdfBytes = imageConversionService.convert(pngMedNuller(2000, 1500, 16, PNG_RGBA), Format.PNG)

        assertPdfMedEttBilde(pdfBytes, bredde = 1500, høyde = 2000)
    }

    @ParameterizedTest
    @CsvSource("1, false", "1, true", "3, false", "3, true")
    internal fun `jpeg med én eller tre fargekanaler konverteres til pdf`(
        fargekanaler: Int,
        progressiv: Boolean,
    ) {
        val pdfBytes = imageConversionService.convert(jpeg(1200, 1600, fargekanaler, progressiv), Format.JPEG)

        assertPdfMedEttBilde(pdfBytes, bredde = 1200, høyde = 1600)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `jpeg med fire fargekanaler avvises`(progressiv: Boolean) {
        val cmyk = jpeg(800, 600, fargekanaler = 4, progressiv = progressiv)

        assertThatThrownBy { imageConversionService.convert(cmyk, Format.JPEG) }
            .isInstanceOf(InvalidDocumentFormat::class.java)
    }

    @Test
    internal fun `uleselig bilde avvises`() {
        val søppel = byteArrayOf(1, 2, 3, 4, 5)

        assertThatThrownBy { imageConversionService.convert(søppel, Format.JPEG) }
            .isInstanceOf(InvalidDocumentFormat::class.java)
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
