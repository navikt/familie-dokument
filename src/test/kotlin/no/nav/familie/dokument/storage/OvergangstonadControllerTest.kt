package no.nav.familie.dokument.storage

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

internal class OvergangstonadControllerTest {

    lateinit var overgangstonadController: OvergangstonadController

    @BeforeEach
    internal fun setUp() {
        val storageMock = mockk<MellomLagerService>()
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
}