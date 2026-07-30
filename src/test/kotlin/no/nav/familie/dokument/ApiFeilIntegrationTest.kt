package no.nav.familie.dokument

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.storage.integrationTest.OppslagSpringRunnerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import kotlin.test.assertEquals

class ApiFeilIntegrationTest : OppslagSpringRunnerTest() {
    @BeforeEach
    fun setHeaders() {
        headers.setBearerAuth(token())
        headers.contentType = MediaType.APPLICATION_JSON
    }

    @Test
    fun `skal få 200 når autentisert og vi bruker get`() {
        // Arrange
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }
        every { storageMock.get(any<BlobId>()) } returns blob

        restTemplate.exchange<Any>(localhost("/api/soknad/barnetrygd"), HttpMethod.POST, HttpEntity(mapOf<String, String>(), headers))

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/api/soknad/barnetrygd"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `skal få 400 når man sender inn feil type objekt, liste i stedet for objekt`() {
        // Arrange
        val ugyldigJson = """ { "søknad""""

        // Act
        val response =
            restTemplate.exchange<Any>(
                localhost("/api/soknad/barnetrygd"),
                HttpMethod.POST,
                HttpEntity(ugyldigJson, headers),
            )

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test // Tester handleExceptionInternal
    fun `skal få 415 når man sender inn feil type Content-Type`() {
        // Arrange
        headers.contentType = MediaType.TEXT_PLAIN

        // Act
        val response =
            restTemplate.exchange<Any>(
                localhost("/api/soknad/barnetrygd"),
                HttpMethod.POST,
                HttpEntity("Hei", headers),
            )

        // Assert
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.statusCode)
    }

    @Test
    fun `skal få 401 når ikke autentisert `() {
        // Arrange
        headers.setBearerAuth("")

        // Act
        val response =
            restTemplate.exchange<Any>(
                localhost("/api/soknad/barnetrygd"),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
            )

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `skal få 404 når endepunkt ikke eksisterer`() {
        // Act
        val response =
            restTemplate.exchange<Any>(
                localhost("/ikke-eksisterende-endepunkt"),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
            )

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `skal få 500 når autentisert endepunkt kaster feil`() {
        // Arrange
        every { storageMock.get(any<BlobId>()) } throws RuntimeException("Simulert lagringsfeil")

        // Act
        val response =
            restTemplate.exchange<Any>(
                localhost("/api/soknad/barnetrygd"),
                HttpMethod.GET,
                HttpEntity<Any>(headers),
            )

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}
