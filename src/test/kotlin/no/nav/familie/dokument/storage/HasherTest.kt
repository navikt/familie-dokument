package no.nav.familie.dokument.storage

import no.nav.familie.dokument.storage.encryption.Hasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class HasherTest {

    private val hasher = Hasher("sectretSalt")
    private val testFnr = "test_fnr"

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

    private fun assertThatDigestHarLengde44(fnr: String, hemmeligSalt: String) {
        val testHasher = Hasher(hemmeligSalt)
        val directory = testHasher.lagFnrHash(fnr)
        assertThat(directory).hasSize(44)
        print(directory)
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere noe`() {
        val directory = hasher.lagFnrHash(testFnr)
        assertThat(directory).isNotBlank
    }

    @Test
    internal fun `Hent mappe for bruker må ha hemmelig salt`() {
        val utenHemmligSalt = Hasher("  ")
        assertThrows<IllegalArgumentException> {
            utenHemmligSalt.lagFnrHash(testFnr)
        }
    }

    @Test
    internal fun `Hent mappe for bruker må ha fnr`() {
        assertThrows<IllegalArgumentException> {
            hasher.lagFnrHash("   ")
        }
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere samme directory ved likt hemmelig salt`() {
        val directory = hasher.lagFnrHash(testFnr)
        val directory2 = hasher.lagFnrHash(testFnr)
        assertThat(directory).isEqualTo(directory2)
    }

    @Test
    internal fun `Hent mappe for bruker skal returnere ulikt directory ved ulikt hemmelig salt`() {
        val directory = hasher.lagFnrHash(testFnr)
        val directory2 = hasher.lagFnrHash("test_fnr2")
        assertThat(directory).isNotEqualTo(directory2)
    }
}