package no.nav.familie.dokument.storage

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.familie.sikkerhet.EksternBrukerUtils.hentFnrFraToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

internal class StonadControllerTest {
    private val storageMock = mockk<MellomLagerService>()
    private val stonadController = StonadController(storageMock, jsonMapper, Hasher("hammeligSalt"))

    @BeforeEach
    internal fun setUp() {
        mockkObject(EksternBrukerUtils)
        every { hentFnrFraToken() } returns "12345678901"
        every { storageMock.put(any(), any(), any()) } just Runs
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(EksternBrukerUtils)
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
            stonadController.mellomlagreSøknad(
                StonadController.StønadParameter.valueOf("overgangsstonad"),
                ugyldigJson,
            )
        }
    }
}
