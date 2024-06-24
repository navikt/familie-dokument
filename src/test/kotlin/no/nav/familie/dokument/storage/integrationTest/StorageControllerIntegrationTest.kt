package no.nav.familie.dokument.storage.integrationTest

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.http.client.MultipartBuilder
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.MultiValueMap

@ActiveProfiles("integration-test")
class StorageControllerIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var storageMock: Storage

    @BeforeEach
    fun setup() {
        headers.setBearerAuth(søkerBearerToken())
        headers.contentType = MediaType.MULTIPART_FORM_DATA
    }

    @Test
    internal fun `skal lagre fil med navnet på filen`() {
        initMock()
        val navn = "gyldig-0.8m.pdf"
        val vedlegg = leseVedlegg(navn)

        val response = restTemplate.exchange<Map<String, String>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(vedlegg, headers),
        )

        val filnavn = response.body?.get("filnavn")
        assertThat(filnavn).isEqualTo(navn)
    }

    @Test
    fun `Skal returnere 400 for vedlegg som overstiger størrelsesgrensen`() {
        val vedlegg = leseVedlegg("ugyldig-2.6m.pdf")
        val response = restTemplate.exchange<Map<String, Any>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(vedlegg, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Skal returnere 500 for vedlegg med ustøttet type`() {
        initMock()
        val vedlegg = leseVedlegg("ugyldig.txt")
        val response = restTemplate.exchange<Map<String, Any>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(vedlegg, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `Skal returnere 500 hvis Google Storage feil`() {
        val vedlegg = leseVedlegg("gyldig-0.8m.pdf")

        every { storageMock.create(any(), any<ByteArray>()) } throws StorageException(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
        )
        val response = restTemplate.exchange<Map<String, Any>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(vedlegg, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun leseVedlegg(navn: String): MultiValueMap<String, Any> {
        val content = StorageControllerIntegrationTest::class.java.getResource("/vedlegg/$navn")!!.readBytes()

        return MultipartBuilder()
            .withByteArray("file", navn, content)
            .build()
    }

    private fun initMock() {
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        every { storageMock.get(any<BlobId>()) } returns blob
    }

    class RessursData {

        lateinit var data: ByteArray
        lateinit var status: Ressurs.Status
    }

    @Test
    fun `Skal lese vedlegg ut som er lagret`() {
        initMock()
        val vedlegg = leseVedlegg("gyldig-0.8m.pdf")

        val response = restTemplate.exchange<Map<String, String>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(vedlegg, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        val dokumentId = response.body?.get("dokumentId")

        headers.contentType = MediaType.APPLICATION_JSON
        val responseGet = restTemplate.exchange<String>(
            localhost("/api/mapper/familie-dokument-test/$dokumentId"),
            HttpMethod.GET,
            HttpEntity<String>(headers),
        )

        assertThat(responseGet.statusCode).isEqualTo(HttpStatus.OK)
        val ressursData = objectMapper.readValue(responseGet.body?.toString(), RessursData::class.java)
        assertThat(ressursData.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(ressursData.data.size).isEqualTo((vedlegg.get("file")?.first() as ByteArrayResource).byteArray.size)
    }

    @Test
    fun `Skal kunne hente lagret vedlegg som bytearray uten å pakke inn det i ressurs`() {
        initMock()
        val vedlegg = leseVedlegg("gyldig-0.8m.pdf")

        val response = restTemplate.exchange<Map<String, String>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(vedlegg, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        val dokumentId = response.body?.get("dokumentId")

        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        val responseGet = restTemplate.exchange<String>(
            localhost("/api/mapper/familie-dokument-test/$dokumentId/pdf"),
            HttpMethod.GET,
            HttpEntity<String>(headers),
        )
        assertThat(responseGet.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseGet.body?.toString()?.length).isEqualTo((vedlegg.get("file")?.first() as ByteArrayResource).byteArray.size)
    }

    @Test
    fun `Skal returnere 404 for å hente ukjent dokument`() {
        every { storageMock.get(any<BlobId>()) } returns null

        headers.contentType = MediaType.APPLICATION_JSON
        val responseGet = restTemplate.exchange<String>(
            localhost("/api/mapper/familie-dokument-test/ukjent-id"),
            HttpMethod.GET,
            HttpEntity<String>(headers),
        )

        assertThat(responseGet.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    internal fun `skal returnere 400 dersom tom liste sendes inn`() {
        val emptyMap = HttpHeaders()
        val response = restTemplate.exchange<Map<String, Any>>(
            localhost("/api/mapper/familie-dokument-test"),
            HttpMethod.POST,
            HttpEntity(emptyMap, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
