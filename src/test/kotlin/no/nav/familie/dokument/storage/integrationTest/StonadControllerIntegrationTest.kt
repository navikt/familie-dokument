package no.nav.familie.dokument.storage.integrationTest

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class StonadControllerIntegrationTest : OppslagSpringRunnerTest() {
    @BeforeEach
    fun setup() {
        headers.setBearerAuth(token())
        headers.contentType = MediaType.APPLICATION_JSON
    }

    @Test
    fun `Returner 201 Created ved lagring av søknad med gyldig json`() {
        // Arrange
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/barnetilsyn"),
                HttpMethod.POST,
                HttpEntity(gyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `Returner lagret søknad i getter`() {
        // Arrange
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """

        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()

        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        every { storageMock.get(any<BlobId>()) } returns blob

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/barnetilsyn"),
                HttpMethod.POST,
                HttpEntity(gyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        // Act
        val responseGet =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/barnetilsyn"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        // Assert
        assertThat(responseGet.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseGet.body).isEqualTo(gyldigJson)
    }

    @Test
    fun `Returner 400 Bad Request ved ugyldig json`() {
        // Arrange
        val ugyldigJson = """ { "søknad""""

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/barnetilsyn"),
                HttpMethod.POST,
                HttpEntity(ugyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Returner 500 Internal Server Error hvis Google Storage feil`() {
        // Arrange
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        every { storageMock.create(any(), any<ByteArray>()) } throws StorageException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/barnetilsyn"),
                HttpMethod.POST,
                HttpEntity(gyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `Returner 201 Created ved lagring av søknad med overgangsstonad-regelendring-2026`() {
        // Arrange
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/overgangsstonad-regelendring-2026"),
                HttpMethod.POST,
                HttpEntity(gyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `Returner lagret søknad i getter for overgangsstonad-regelendring-2026`() {
        // Arrange
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """

        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()

        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        every { storageMock.get(any<BlobId>()) } returns blob

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/overgangsstonad-regelendring-2026"),
                HttpMethod.POST,
                HttpEntity(gyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        // Act
        val responseGet =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/overgangsstonad-regelendring-2026"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        // Assert
        assertThat(responseGet.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseGet.body).isEqualTo(gyldigJson)
    }

    @Test
    fun `Returner 400 Bad Request for ukjent stønadtype`() {
        // Arrange
        val gyldigJson = """ { "søknad": { "feltA": "æØå"} } """

        // Act
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/ukjent-stonad"),
                HttpMethod.POST,
                HttpEntity(gyldigJson, headers),
            )

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Returner 204 No Content ved forsøk på å hente dokument som ikke finnes`() {
        // Arrange
        every { storageMock.get(any<BlobId>()) } returns null

        // Act
        val reponse =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/soknad/barnetilsyn"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        // Assert
        assertThat(reponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
