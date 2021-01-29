package no.nav.familie.dokument.storage

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TokenValidationContextHolderExtentionTest {

    private val tokenValidationContextHolderMock = mockk<TokenValidationContextHolder>()

    @BeforeEach
    internal fun setUp() {
        every { tokenValidationContextHolderMock.hentFnr() } returns "test_fnr"
    }

    @Test
    internal fun `Mappenavn for bruker skal være 44 lang`() {
        assertThatDigestHarLengde44("12345678911",
                                    "kjhsfdglkjhsdfglkjhdfsgoiwue4toq43htrw4o8ue0fj3qå08q4ofjewåowe80fjwpe4ofij")

        assertThatDigestHarLengde44("9999999999999",
                                    "983745987345987345987345987345987345987345987345987345987345987345987345987")
        assertThatDigestHarLengde44("9999999999999",
                                    "983745987345987345987345987345987345987345987345987345987345987345987345987")
        assertThatDigestHarLengde44("9999999999999999999999999999",
                                    "983745987345987345987345987345987345987345987345987345987345987345987345987999fhjdfhsdægijep9seån0 guwenpgijsenøgpijsneæ pgnposeæpgroinæpsot")
    }

    private fun assertThatDigestHarLengde44(fnr: String, pepper: String) {
        every { tokenValidationContextHolderMock.hentFnr() } returns fnr
        val directory =
                tokenValidationContextHolderMock.hentFnrHash(pepper)
        Assertions.assertThat(directory).hasSize(44)
        println("Size: ${directory.length}")

    }

    @Test
    internal fun `Hent mappe for bruker skal returnere noe`() {
        val directory = tokenValidationContextHolderMock.hentFnrHash("pepper")
        Assertions.assertThat(directory).isNotBlank
    }

    @Test
    internal fun `Hent mappe for brukermå ha pepper`() {
        assertThrows<IllegalArgumentException> {
            tokenValidationContextHolderMock.hentFnrHash("  ")
        }
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere samme directory ved likt pepper`() {
        val directory = tokenValidationContextHolderMock.hentFnrHash("pepper")
        val directory2 = tokenValidationContextHolderMock.hentFnrHash("pepper")
        Assertions.assertThat(directory).isEqualTo(directory2)
    }


    @Test
    internal fun `Hent mappe for bruker skal returnere ulikt directory ved ulikt pepper`() {
        val directory = tokenValidationContextHolderMock.hentFnrHash("pepper")
        val directory2 = tokenValidationContextHolderMock.hentFnrHash("salt")
        Assertions.assertThat(directory).isNotEqualTo(directory2)
    }
}