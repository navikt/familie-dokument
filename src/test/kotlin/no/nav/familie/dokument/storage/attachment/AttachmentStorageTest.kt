package no.nav.familie.dokument.storage.attachment

import io.mockk.*
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Objects

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException


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
        every { delegate[any(),any()] } throws (RuntimeException())
        every { delegate["directory123", "UUID123"] } returns pdfByteArray

        attachmentStorage.put("directory123", "UUID123", toStream("dummy/pdf_dummy.pdf"))
        assertThat(attachmentStorage["directory123", "UUID123"]).isEqualTo(pdfByteArray)
        assertThrows<RuntimeException>{ attachmentStorage["directory123", "UUID1234"] }
        assertThrows<RuntimeException>{ attachmentStorage["directory1234", "UUID123"] }
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
