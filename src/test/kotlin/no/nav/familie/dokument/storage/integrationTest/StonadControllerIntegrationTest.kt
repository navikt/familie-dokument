package no.nav.familie.dokument.storage.integrationTest

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("integration-test")
class StonadControllerIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var storageMock: Storage

    @BeforeEach
    fun setup() {
        headers.setBearerAuth(søkerBearerToken())
        headers.contentType = MediaType.APPLICATION_JSON
    }

    @Test
    fun `Returner 201 Created ved lagring av søknad med gyldig json`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        val response = restTemplate.exchange<String>(
            localhost("/familie/dokument/api/soknad/barnetilsyn"),
            HttpMethod.POST,
            HttpEntity(gyldigJson, headers),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `Returner lagret søknad i getter`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """

        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()

        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        every { storageMock.get(any<BlobId>()) } returns blob

        val response = restTemplate.exchange<String>(
            localhost("/familie/dokument/api/soknad/barnetilsyn"),
            HttpMethod.POST,
            HttpEntity(gyldigJson, headers),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        val responseGet = restTemplate.exchange<String>(
            localhost("/familie/dokument/api/soknad/barnetilsyn"),
            HttpMethod.GET,
            HttpEntity<String>(headers),
        )

        assertThat(responseGet.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseGet.body).isEqualTo(gyldigJson)
    }

    @Test
    fun `Returner 400 Bad Request ved ugyldig json`() {
        val ugyldigJson = """ { "søknad""""
        val response = restTemplate.exchange<String>(
            localhost("/familie/dokument/api/soknad/barnetilsyn"),
            HttpMethod.POST,
            HttpEntity(ugyldigJson, headers),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Returner 500 Internal Server Error hvis Google Storage feil`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        every { storageMock.create(any(), any<ByteArray>()) } throws StorageException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")
        val response = restTemplate.exchange<String>(
            localhost("/familie/dokument/api/soknad/barnetilsyn"),
            HttpMethod.POST,
            HttpEntity(gyldigJson, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `Returner 201 No Content ved forsøk på å hente dokument som ikke finnes`() {
        every { storageMock.get(any<BlobId>()) } returns null

        val reponse = restTemplate.exchange<String>(
            localhost("/familie/dokument/api/soknad/barnetilsyn"),
            HttpMethod.GET,
            HttpEntity<String>(headers),
        )

        assertThat(reponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
