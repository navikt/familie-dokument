package no.nav.familie.dokument.storage.attachment

import io.mockk.*
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Objects
import java.util.Optional

import org.assertj.core.api.Assertions.assertThat


class AttachmentStorageTest {

    private val delegate : EncryptedStorage = mockk()
    private val converter : AttachmentToStorableFormatConverter = mockk()
    private val attachmentStorage = AttachmentStorage(delegate, converter)

    private val pdfByteArray = readStream(toStream("dummy/pdf_dummy.pdf")).toByteArray()
    private val pdfByteString = readStream(toStream("dummy/pdf_dummy.pdf")).toString(StandardCharsets.UTF_8)


    @Before
    @Throws(IOException::class)
    fun setUp() {
        every { converter.toStorageFormat(any()) } returns pdfByteArray
        every { delegate.put(any(),any(),any()) } just Runs
    }

    @Test
    @Throws(IOException::class)
    fun converts_before_put() {
        attachmentStorage.put("directory123", "UUID123", toStream("dummy/jpg_dummy.jpg"))

        val slot = slot<ByteArrayInputStream>()

        every { delegate.put("directory123","UUID",capture(slot)) } answers {
            val capturedStream = readStream(slot.captured).toString(StandardCharsets.UTF_8)
            assertThat(capturedStream).isEqualTo(pdfByteString)
        }
    }

    @Test
    @Throws(IOException::class)
    fun converted_after_get() {
        val optionalByteArray = Optional.ofNullable(pdfByteArray)
        every { delegate[any(),any()] } returns Optional.empty()
        every { delegate["directory123", "UUID123"] } returns optionalByteArray

        attachmentStorage.put("directory123", "UUID123", toStream("dummy/pdf_dummy.pdf"))
        assertThat(attachmentStorage["directory123", "UUID123"]).isEqualTo(optionalByteArray)
        assertThat(attachmentStorage["directory123", "UUID1234"]).isEmpty
        assertThat(attachmentStorage["directory1234", "UUID123"]).isEmpty
    }

    @Throws(IOException::class)
    private fun toStream(filename: String): ByteArrayInputStream {
        Objects.requireNonNull(filename, "filename")
        val vedleggsfil = File("src/test/resources/$filename")
        require(vedleggsfil.exists()) { "Ikke gyldig fil $filename" }
        return ByteArrayInputStream(FileUtils.readFileToByteArray(vedleggsfil))
    }

    private fun readStream(inputStream: ByteArrayInputStream): ByteArrayOutputStream {
        val buffer = ByteArrayOutputStream()
        inputStream.copyTo(buffer,65536)
        return buffer
    }
}