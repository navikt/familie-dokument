package no.nav.familie.dokument.storage

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.security.MessageDigest
import java.util.*

internal class StonadControllerTest {

    lateinit var stonadController: StonadController
    lateinit var storageMock: MellomLagerService

    @BeforeEach
    internal fun setUp() {
        storageMock = mockk<MellomLagerService>()
        val contextHolderMock = mockk<TokenValidationContextHolder>()
        stonadController = StonadController(storageMock, contextHolderMock, objectMapper, Hasher("hammeligSalt"))

        every { contextHolderMock.hentFnr() } returns "12345678901"
        every { storageMock.put(any(), any(), any()) } just Runs
    }

    @Test
    internal fun `skal mellomlagre søknad om overgangsstønad`() {
        val gyldigJson = """ { "søknad": { "feltA": "æØå", "feltB": 1234} } """
        val response = stonadController.mellomlagreSøknad(StonadController.StønadParameter.valueOf("overgangsstonad"), gyldigJson)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    internal fun `skal feile ved mellomlagring dersom søknaden ikke er gyldig json`() {
        val ugyldigJson = "Jeg gikk en tur på stien"
        assertThrows<IllegalArgumentException> {
            stonadController.mellomlagreSøknad(StonadController.StønadParameter.valueOf("overgangsstonad"),
                                               ugyldigJson)
        }
    }



}
