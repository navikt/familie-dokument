package no.nav.familie.dokument.storage

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.lang.RuntimeException

internal class OvergangstonadControllerTest {

    lateinit var overgangstonadController: OvergangstonadController
    lateinit var storageMock: MellomLagerService

    @BeforeEach
    internal fun setUp() {
        storageMock = mockk<MellomLagerService>()
        val contextHolderMock = mockk<TokenValidationContextHolder>()
        overgangstonadController = OvergangstonadController(storageMock, contextHolderMock, objectMapper)

        every { contextHolderMock.hentFnr() } returns "12345678901"
        every { storageMock.put(any(), any(), any()) } just Runs
    }

    @Test
    internal fun `skal mellomlagre søknad om overgangsstønad`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        val response = overgangstonadController.mellomlagreSøknad(gyldigJson)
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    internal fun `skal feile ved mellomlagring dersom søknaden ikke er gyldig json`() {
        val ugyldigJson = "Jeg gikk en tur på stien"
        assertThrows<IllegalStateException> { overgangstonadController.mellomlagreSøknad(ugyldigJson) }
    }

    @Test
    internal fun `skal feile ved mellomlagring dersom s3 kaster exception`() {
        val json = """{"a": 1}"""
        every { storageMock.put(any(), any(), any()) } throws SdkClientException("Noe gikk galt")
        Assertions.assertThat(overgangstonadController.mellomlagreSøknad(json).statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    internal fun `skal returnere 204 dersom objektet ikke finnes i S3`() {
        val amazonNotFoundException = AmazonS3Exception("Noe gikk galt")
        amazonNotFoundException.statusCode = 404
        every { storageMock.get(any(), any()) } throws amazonNotFoundException
        Assertions.assertThat(overgangstonadController.hentMellomlagretSøknad().statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    internal fun `skal kaste 500 dersom noe uventet feil oppstår`() {
        val uventetFeil = RuntimeException("Noe gikk galt")
        every { storageMock.get(any(), any()) } throws uventetFeil
        Assertions.assertThat(overgangstonadController.hentMellomlagretSøknad().statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}