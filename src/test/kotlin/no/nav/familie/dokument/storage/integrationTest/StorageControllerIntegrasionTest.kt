package no.nav.familie.dokument.storage.integrationTest

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.ApiExceptionHandler
import no.nav.familie.dokument.config.IntegrationTestConfig
import no.nav.familie.dokument.storage.StorageController
import no.nav.familie.dokument.storage.attachment.AttachmentConfiguration
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration
import no.nav.familie.dokument.storage.google.GcpStorageConfiguration
import no.nav.familie.dokument.storage.hentFnr
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals

@AutoConfigureMockMvc
@ContextConfiguration(classes = [StorageController::class,
    AttachmentConfiguration::class,
    EncryptedStorageConfiguration::class,
    GcpStorageConfiguration::class,
    ApiExceptionHandler::class,
    IntegrationTestConfig::class])
@WebMvcTest
@ActiveProfiles("integration-test")
class StorageControllerIntegrasionTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var tokenValidationContextHolderMock: TokenValidationContextHolder

    @Autowired
    lateinit var storageMock: Storage

    @Test
    fun `Skal returnere 400 for vedlegg som overstiger størrelsesgrensen`() {
        val vedlegg= leseVedlegg("ugyldig-2.6m.pdf", "application/pdf")
        mockMvc.perform(multipart("/api/mapper/{bucket}", "familie-dokument-test").file(vedlegg))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun `Skal returnere 500 for vedlegg med ustøttet type`() {
        initMockWithoutArtificialErrors()
        val vedlegg= leseVedlegg("ugyldig.txt", "text")
        mockMvc.perform(multipart("/api/mapper/{bucket}", "familie-dokument-test").file(vedlegg))
                .andExpect(status().isInternalServerError)
    }

    private fun leseVedlegg(navn: String, type: String): MockMultipartFile {
        val name = "file"
        val originalFileName = navn
        val contentType = type
        val content = StorageControllerIntegrasionTest::class.java.getResource("/vedlegg/${navn}").readBytes()
        return MockMultipartFile(name, originalFileName, contentType, content)
    }

    @Test
    fun `Skal returnere 500 hvis Google Storage feil`() {
        val vedlegg= leseVedlegg("gyldig-0.8m.pdf", "application/pdf")

        every { tokenValidationContextHolderMock.hentFnr() } returns TEST_FNR
        every { storageMock.create(any(), any<ByteArray>()) } throws StorageException(HttpStatus.UNAUTHORIZED.value(),
                                                                                      "Unauthorized");
        mockMvc.perform(multipart("/api/mapper/{bucket}", "familie-dokument-test").file(vedlegg))
                .andExpect(status().isInternalServerError)
    }

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun initMockWithoutArtificialErrors(){
        every { tokenValidationContextHolderMock.hentFnr() } returns TEST_FNR

        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        every { storageMock.create(any(), capture(slot)) } answers {
            every { blob.getContent() } returns slot.captured
            blob
        }

        every { storageMock.get(any<BlobId>()) } returns blob
    }

    class RessurData{
        lateinit var data: ByteArray
        lateinit var status: Ressurs.Status
    }

    @Test
    fun `Skal lese vedlegg ut som er lagret`() {
        initMockWithoutArtificialErrors()
        val vedlegg= leseVedlegg("gyldig-0.8m.pdf", "application/pdf")
        val result = mockMvc.perform(multipart("/api/mapper/{bucket}", "familie-dokument-test").file(vedlegg))
                .andExpect(status().isCreated).andReturn()
        val dokumentId= objectMapper.readValue(result.response.contentAsString, Map::class.java)["dokumentId"]

        val output = mockMvc.get("/api/mapper/familie-dokument-test/${dokumentId}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
        }.andReturn().response.contentAsByteArray

        val content = objectMapper.readValue(output, RessurData::class.java)
        assertEquals(Ressurs.Status.SUKSESS, content.status)
        assertEquals(vedlegg.bytes.size, content.data.size)
    }

    @Test
    fun `Skal returnere 404 for å hent ukjent dokument`() {
        every { tokenValidationContextHolderMock.hentFnr() } returns TEST_FNR
        every { storageMock.get(any<BlobId>()) } returns null

        mockMvc.get("/api/mapper/familie-dokument-test/ukjent-id") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound }
        }
    }

    companion object {
        val TEST_FNR = "TestFnr"
    }
}