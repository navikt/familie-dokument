package no.nav.familie.dokument.storage.attachment

import io.mockk.every
import io.mockk.mockk
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

import org.assertj.core.api.Assertions.assertThat
import java.util.*

class AttachmentConverterTest {

    private val imageConversionService : ImageConversionService = mockk()
    private val converter = AttachmentToStorableFormatConverter(imageConversionService)
    private val convertedDummy: ByteArray = toByteArray("dummy/pdf_dummy.pdf")

    @Before
    @Throws(IOException::class)
    fun setUp() {
        every { imageConversionService.convert(any(), any()) } returns convertedDummy
    }

    @Test(expected = RuntimeException::class)
    @Throws(RuntimeException::class, IOException::class)
    fun at_ulovlig_format_kaster_exception() {
        val txtVedlegg = toByteArray("dummy/txt_dummy.txt")
        converter.toStorageFormat(txtVedlegg)
    }

    @Test
    @Throws(IOException::class)
    fun at_pdf_aksepteres() {
        val pdfVedlegg = toByteArray("dummy/pdf_dummy.pdf")
        val storable = converter.toStorageFormat(pdfVedlegg)
        assertThat(storable).isEqualTo(pdfVedlegg)
    }

    @Test
    @Throws(IOException::class)
    fun at_bilder_konverteres() {
        var converted: ByteArray
        var vedlegg: ByteArray

        vedlegg = toByteArray("dummy/jpg_dummy.jpg")
        converted = converter.toStorageFormat(vedlegg)
        assertThat(converted).isEqualTo(convertedDummy)

        vedlegg = toByteArray("dummy/png_dummy.png")
        converted = converter.toStorageFormat(vedlegg)
        assertThat(converted).isEqualTo(convertedDummy)
    }

    @Throws(IOException::class)
    private fun toByteArray(filename: String): ByteArray {
        val vedleggsfil = File("src/test/resources/$filename")
        val inputStream = ByteArrayInputStream(FileUtils.readFileToByteArray(vedleggsfil))
        val buffer = ByteArrayOutputStream()
        inputStream.copyTo(buffer,65536)
        return buffer.toByteArray()
    }
}
