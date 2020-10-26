package no.nav.familie.dokument.storage

import com.google.cloud.storage.Blob
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.dokument.storage.encryption.EncryptedStorageConfiguration
import no.nav.familie.dokument.storage.google.GcpStorageConfiguration
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import io.mockk.slot
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
@ContextConfiguration(classes = [StonadController::class,
    MellomLagerService::class,
    EncryptedStorageConfiguration::class,
    GcpStorageConfiguration::class,
    RestExceptionHandler::class,
    StonadControllerIntegrationTestConfig::class])
@WebMvcTest
@ActiveProfiles("integration-test")
class StonadControllerIntegrasionTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var tokenValidationContextHolderMock: TokenValidationContextHolder

    @Autowired
    lateinit var storageMock: Storage

    @Test
    fun `Skal lagre søknad som er gyldig json`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        every{tokenValidationContextHolderMock.hentFnr()} returns TEST_FNR
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        var captured = ByteArray(0)
        every{storageMock.create(any(), capture(slot))} answers{
            captured= slot.captured
            blob
        }
        every{blob.getContent()} returns captured

        mockMvc.post("/api/soknad/{stonad}", "barnetilsyn") {
            contentType = MediaType.APPLICATION_JSON
            content = gyldigJson
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isCreated }
        }
    }

    @Test
    fun `Skal returnere 400 for søknad som er ugyldig json`() {
        val ugyldigJson = """ { "søknad""""
        mockMvc.post("/api/soknad/{stonad}", "barnetilsyn") {
            contentType = MediaType.APPLICATION_JSON
            content = ugyldigJson
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest }
        }
    }

    @Test
    fun `Skal returnere 500 hvis Google Storage feil`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        every{tokenValidationContextHolderMock.hentFnr()} returns TEST_FNR
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()
        var captured = ByteArray(0)
        every{storageMock.create(any(), capture(slot))} throws StorageException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
        every{blob.getContent()} returns captured

        mockMvc.post("/api/soknad/{stonad}", "barnetilsyn") {
            contentType = MediaType.APPLICATION_JSON
            content = gyldigJson
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isInternalServerError }
        }
    }

    @Test
    fun `Skal lese søknad som er lagret`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        every{tokenValidationContextHolderMock.hentFnr()} returns TEST_FNR
        val slot = slot<ByteArray>()
        val blob = mockk<Blob>()

        every{storageMock.create(any(), capture(slot))} answers{
            every{blob.getContent()} returns slot.captured
            blob
        }

        every{storageMock.get(any(), any<String>(), *anyVararg()) } returns blob

        mockMvc.post("/api/soknad/{stonad}", "barnetilsyn") {
            contentType = MediaType.APPLICATION_JSON
            content = gyldigJson
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isCreated }
        }

        mockMvc.get("/api/soknad/barnetilsyn") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
            content{json(gyldigJson)}
        }
    }

    @Test
    fun `Skal returnere 204 for å hent ukjent dokument`() {
        every{tokenValidationContextHolderMock.hentFnr()} returns TEST_FNR
        every{storageMock.get(any(), any<String>(), *anyVararg()) } throws StorageException(HttpStatus.NOT_FOUND.value(), "Not Found")

        mockMvc.get("/api/soknad/barnetilsyn") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNoContent }
        }
    }

    companion object{
        val TEST_FNR= "TestFnr"
    }
}

@Profile("integration-test")
@Configuration
class StonadControllerIntegrationTestConfig {

    @Bean
    @Primary
    fun tokenValidationContextHolderMock(): TokenValidationContextHolder {
        val tokenValidationContextHolder =  mockk<TokenValidationContextHolder>()
        every{tokenValidationContextHolder.tokenValidationContext} returns mockk()
        return tokenValidationContextHolder
    }

    @Bean
    @Primary
    fun storageMock(): Storage{
        val storageMock = mockk<Storage>()
        return storageMock
    }
}