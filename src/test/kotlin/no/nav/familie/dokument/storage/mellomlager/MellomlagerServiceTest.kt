package no.nav.familie.dokument.storage.mellomlager

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.storage.encryption.EncryptedStorage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MellomlagerServiceTest {

    private val delegate: EncryptedStorage = mockk()
    private val mellomLagerService = MellomLagerService(delegate)
    private val jsonVerdi = """ { "a": "æøå" } """

    @BeforeEach
    fun setUp() {
        every { delegate.put(any(), any(), any()) } just Runs
    }

    @Test
    fun `skal putte søknad om overgangsstønad i mellomlager`() {
        val slot = slot<ByteArrayInputStream>()

        every { delegate.put(any(), any(), capture(slot)) } answers {
            slot.captured
        }
        mellomLagerService.put("directory123", "UUID123", jsonVerdi)

        assertThat(readStream(slot.captured).toString()).isEqualTo(jsonVerdi)
    }

    @Test
    fun `skal hente ut søknad om overgangsstønad i mellomlager`() {
        every { delegate[any(), any()] } throws (RuntimeException())
        every { delegate["directory123", "UUID123"] } returns jsonVerdi.toByteArray()

        assertThat(mellomLagerService["directory123", "UUID123"]).isEqualTo(jsonVerdi)

        assertThrows<RuntimeException> { mellomLagerService["directory1234", "UUID123"] }
    }

    private fun readStream(inputStream: ByteArrayInputStream): ByteArrayOutputStream {
        val buffer = ByteArrayOutputStream()
        inputStream.copyTo(buffer, 65536)
        return buffer
    }
}
