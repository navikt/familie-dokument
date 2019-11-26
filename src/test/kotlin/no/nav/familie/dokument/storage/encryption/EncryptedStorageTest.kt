package no.nav.familie.dokument.storage.encryption

import io.mockk.*
import no.nav.familie.dokument.SetOidcClaimsWithSubject
import no.nav.familie.dokument.storage.hentFnr
import no.nav.familie.dokument.storage.s3.S3Storage
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.*

class EncryptedStorageTest {

    private val UNENCRYPTED_DATA = "originalStream".toByteArray()
    private val ENCRYPTED_DATA = "encryptedStream".toByteArray()
    private val ENCRYPTED_STREAM = ByteArrayInputStream(ENCRYPTED_DATA)
    private val FNR = "DummyFnr"
    private val DIRECTORY = "directory"
    private val KEY = "UUID"

    @Rule @JvmField
    val claims = SetOidcClaimsWithSubject(FNR)

    private val storage : S3Storage = mockk()
    private val tokenValidationContextHolder : TokenValidationContextHolder = mockk()
    private val encryptor : Encryptor = mockk()

    private val encryptedStorage = EncryptedStorage(tokenValidationContextHolder, storage, encryptor)


    @Before
    fun setUpMockedEncryptor() {

        every { encryptor.encryptedStream(FNR, any())} returns ENCRYPTED_STREAM
        every { encryptor.decrypt(FNR, eq(ENCRYPTED_DATA))} returns UNENCRYPTED_DATA
        every { storage.put(eq(DIRECTORY),eq(KEY),any()) } just Runs

        mockkStatic("no.nav.familie.dokument.storage.ExtensionsKt")

        every {
            tokenValidationContextHolder.hentFnr()
        } returns FNR

    }

    @Test
    fun encrypts_before_put() {

        encryptedStorage.put(DIRECTORY, KEY, ByteArrayInputStream(UNENCRYPTED_DATA))

        verify {
            storage.put(DIRECTORY, KEY,ENCRYPTED_STREAM)
        }
    }

    @Test
    fun encrypts_after_get() {

        every { storage[DIRECTORY, KEY]  } returns Optional.of(ENCRYPTED_DATA)

        val fetchedData = encryptedStorage[DIRECTORY, KEY]

        assertThat(fetchedData).hasValue(UNENCRYPTED_DATA)
    }

}