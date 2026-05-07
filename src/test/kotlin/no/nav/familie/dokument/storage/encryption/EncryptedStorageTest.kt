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
    private val unencryptedData = "originalStream".toByteArray()
    private val encryptedData = "encryptedStream".toByteArray()
    private val encryptedStream = ByteArrayInputStream(encryptedData)

    companion object {
        private const val FNR = "DummyFnr"
        private const val DIRECTORY = "directory"
        private const val KEY = "UUID"
    }

    private val storage: GcpStorageWrapper = mockk()
    private val tokenValidationContextHolder: TokenValidationContextHolder = mockk()
    private val encryptor: Encryptor = mockk()

    private val encryptedStorage = EncryptedStorage(tokenValidationContextHolder, storage, encryptor)

    @BeforeEach
    fun setUpMockedEncryptor() {
        every { encryptor.encryptedStream(FNR, any()) } returns encryptedStream
        every { encryptor.decrypt(FNR, eq(encryptedData)) } returns unencryptedData
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
        encryptedStorage.put(DIRECTORY, KEY, ByteArrayInputStream(unencryptedData))

        verify {
            storage.put(DIRECTORY, KEY, encryptedStream)
        }
    }

    @Test
    fun encrypts_after_get() {
        every { storage[DIRECTORY, KEY] } returns encryptedData

        val fetchedData = encryptedStorage[DIRECTORY, KEY]

        assertThat(fetchedData).isEqualTo(unencryptedData)
    }
}
