package no.nav.familie.dokument.storage.encryption

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.dokument.storage.google.GcpStorageWrapper
import no.nav.familie.dokument.storage.hentFnr
import no.nav.familie.dokument.testutils.ExtensionMockUtil.setUpMockHentFnr
import no.nav.familie.dokument.testutils.ExtensionMockUtil.unmockHentFnr
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class EncryptedStorageTest {

    private val UNENCRYPTED_DATA = "originalStream".toByteArray()
    private val ENCRYPTED_DATA = "encryptedStream".toByteArray()
    private val ENCRYPTED_STREAM = ByteArrayInputStream(ENCRYPTED_DATA)
    private val FNR = "DummyFnr"
    private val DIRECTORY = "directory"
    private val KEY = "UUID"

    private val storage: GcpStorageWrapper = mockk()
    private val tokenValidationContextHolder: TokenValidationContextHolder = mockk()
    private val encryptor: Encryptor = mockk()

    private val encryptedStorage = EncryptedStorage(tokenValidationContextHolder, storage, encryptor)

    @BeforeEach
    fun setUpMockedEncryptor() {
        every { encryptor.encryptedStream(FNR, any()) } returns ENCRYPTED_STREAM
        every { encryptor.decrypt(FNR, eq(ENCRYPTED_DATA)) } returns UNENCRYPTED_DATA
        every { storage.put(eq(DIRECTORY), eq(KEY), any()) } just Runs

        setUpMockHentFnr()

        every {
            tokenValidationContextHolder.hentFnr()
        } returns FNR
    }

    @AfterEach
    internal fun tearDown() {
        unmockHentFnr()
    }

    @Test
    fun encrypts_before_put() {
        encryptedStorage.put(DIRECTORY, KEY, ByteArrayInputStream(UNENCRYPTED_DATA))

        verify {
            storage.put(DIRECTORY, KEY, ENCRYPTED_STREAM)
        }
    }

    @Test
    fun encrypts_after_get() {
        every { storage[DIRECTORY, KEY] } returns ENCRYPTED_DATA

        val fetchedData = encryptedStorage[DIRECTORY, KEY]

        assertThat(fetchedData).isEqualTo(UNENCRYPTED_DATA)
    }
}
