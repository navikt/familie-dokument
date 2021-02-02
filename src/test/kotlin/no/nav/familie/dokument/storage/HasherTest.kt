package no.nav.familie.dokument.storage

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class HasherTest {


    private val hasher = Hasher("pepper")

    private val test_fnr = "test_fnr"



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
        val directory = hasher.lagFnrDigest(test_fnr)

        assertThat(directory).hasSize(44)
        print(directory)
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere noe`() {
        val directory = hasher.lagFnrDigest(test_fnr)
        assertThat(directory).isNotBlank
    }

    @Test
    internal fun `Hent mappe for bruker må ha pepper`() {
        val utenPepper = Hasher("  ")
        assertThrows<IllegalArgumentException> {
            utenPepper.lagFnrDigest(test_fnr)
        }
    }

    @Test
    internal fun `Hent mappe for bruker må ha fnr`() {
        assertThrows<IllegalArgumentException> {
            hasher.lagFnrDigest("   ")
        }
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere samme directory ved likt pepper`() {
        val directory =  hasher.lagFnrDigest(test_fnr)
        val directory2 =  hasher.lagFnrDigest(test_fnr)
        assertThat(directory).isEqualTo(directory2)
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere ulikt directory ved ulikt pepper`() {
        val directory =  hasher.lagFnrDigest(test_fnr)
        val directory2 = hasher.lagFnrDigest("test_fnr2")
        assertThat(directory).isNotEqualTo(directory2)
    }
}