package no.nav.familie.dokument.storage.attachment

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.dokument.InvalidDocumentFormat
import no.nav.familie.dokument.TestUtil.toByteArray
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttachmentConverterTest {

    private val imageConversionService: ImageConversionService = mockk()
    private val flattenPdfService: FlattenPdfService = mockk()
    private val converter = AttachmentToStorableFormatConverter(imageConversionService, flattenPdfService)
    private val convertedDummy: ByteArray = toByteArray("dummy/pdf_dummy.pdf")

    @BeforeEach
    fun setUp() {
        every { imageConversionService.convert(any(), any()) } returns convertedDummy
        every { flattenPdfService.convert(any()) } returns convertedDummy
    }

    @Test
    fun at_ulovlig_format_kaster_exception() {
        val txtVedlegg = toByteArray("dummy/txt_dummy.txt")
        assertThatThrownBy { converter.toStorageFormat(txtVedlegg) }.isInstanceOf(InvalidDocumentFormat::class.java)
    }

    @Test
    fun at_pdf_aksepteres() {
        val pdfVedlegg = toByteArray("dummy/pdf_dummy.pdf")
        val storable = converter.toStorageFormat(pdfVedlegg)
        assertThat(storable).isEqualTo(pdfVedlegg)
        verify(exactly = 0) { imageConversionService.convert(any(), any()) }
        verify(exactly = 1) { flattenPdfService.convert(any()) }
    }

    @Test
    fun at_bilder_konverteres() {
        var converted: ByteArray
        var vedlegg: ByteArray

        vedlegg = toByteArray("dummy/jpg_dummy.jpg")
        converted = converter.toStorageFormat(vedlegg)
        assertThat(converted).isEqualTo(convertedDummy)

        vedlegg = toByteArray("dummy/png_dummy.png")
        converted = converter.toStorageFormat(vedlegg)
        assertThat(converted).isEqualTo(convertedDummy)

        verify(exactly = 2) { imageConversionService.convert(any(), any()) }
    }
}
