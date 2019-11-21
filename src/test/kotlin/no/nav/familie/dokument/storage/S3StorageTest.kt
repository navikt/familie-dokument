package no.nav.familie.dokument.storage

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.dokument.storage.s3.S3Storage
import org.junit.jupiter.api.Test
import java.util.*

class S3StorageTest {

    @Test
    fun testMockk() {
        val storage: S3Storage = mockk()
        every { storage[any(), any()] } returns Optional.of("filinnhold".toByteArray())
    }
}