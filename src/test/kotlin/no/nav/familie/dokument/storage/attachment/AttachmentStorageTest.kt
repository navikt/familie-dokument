package no.nav.familie.dokument.storage.attachment

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.TestUtil.toByteArray
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets


class AttachmentStorageTest {

    private val delegate: EncryptedStorage = mockk()
    private val converter: AttachmentToStorableFormatConverter = mockk()
    private val attachmentStorage = AttachmentStorage(delegate, converter)

    private val pdfByteArray = toByteArray("dummy/pdf_dummy.pdf")
    private val pdfByteString = toByteArray("dummy/pdf_dummy.pdf").toString(StandardCharsets.UTF_8)


    @BeforeEach
    fun setUp() {
        every { converter.toStorageFormat(any()) } returns pdfByteArray
        every { delegate.put(any(), any(), any()) } just Runs
    }

    @Test
    fun converts_before_put() {
        attachmentStorage.put("directory123", "UUID123", toByteArray("dummy/jpg_dummy.jpg"))

        val slot = slot<ByteArrayInputStream>()

        every { delegate.put("directory123", "UUID", capture(slot)) } answers {
            val capturedStream = readStream(slot.captured).toString(StandardCharsets.UTF_8)
            assertThat(capturedStream).isEqualTo(pdfByteString)
        }
    }

    @Test
    fun converted_after_get() {
        every { delegate[any(), any()] } throws (RuntimeException())
        every { delegate["directory123", "UUID123"] } returns pdfByteArray

        attachmentStorage.put("directory123", "UUID123", toByteArray("dummy/pdf_dummy.pdf"))
        assertThat(attachmentStorage["directory123", "UUID123"]).isEqualTo(pdfByteArray)
        assertThrows<RuntimeException> { attachmentStorage["directory123", "UUID1234"] }
        assertThrows<RuntimeException> { attachmentStorage["directory1234", "UUID123"] }
    }

    private fun readStream(inputStream: ByteArrayInputStream): ByteArrayOutputStream {
        val buffer = ByteArrayOutputStream()
        inputStream.copyTo(buffer, 65536)
        return buffer
    }
}
