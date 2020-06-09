package no.nav.familie.dokument.storage.s3

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

import java.io.ByteArrayInputStream

import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

@Testcontainers
class S3StorageTest {

    @Container
    var localStackContainer = LocalStackContainer().withServices(S3)

    lateinit var storage: S3Storage

    @BeforeEach
    fun setUp() {
        val s3 = S3StorageConfiguration().s3(
                localStackContainer.getEndpointConfiguration(S3),
                localStackContainer.defaultCredentialsProvider)

        storage = S3Storage(s3, 20)
    }

    @Test
    fun testStorage() {
        storage.put("dir", "file", ByteArrayInputStream("testeksempel1".toByteArray()))
        storage.put("dir", "file2", ByteArrayInputStream("testeksempel2".toByteArray()))

        assertEquals("testeksempel1",String(storage["dir", "file"]))
        assertEquals("testeksempel2",String(storage["dir", "file2"]))
    }

    @Test
    fun `skal overskrive gammel verdi hvis samme nøkkel blir brukt`() {
        storage.put("dir", "fileX", ByteArrayInputStream("testeksempel1".toByteArray()))
        storage.put("dir", "fileX", ByteArrayInputStream("testeksempel2".toByteArray()))

        assertEquals("testeksempel2",String(storage["dir", "fileX"]))
    }

}
