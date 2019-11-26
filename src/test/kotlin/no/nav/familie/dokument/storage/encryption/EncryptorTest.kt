package no.nav.familie.dokument.storage.encryption

import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

import org.assertj.core.api.Assertions.assertThat

class EncryptorTest {

    private val secretKeyProvider = SecretKeyProvider("Passphrase eller passord eller kall det hva du vil...")

    private val encryptor = Encryptor(secretKeyProvider)

    @Test
    @Throws(IOException::class)
    fun at_encrypt_og_decrypt_fungerer() {

        val encryptedStream = encryptor.encryptedStream(fnr, ByteArrayInputStream(originalTekst.toByteArray()))

        val encrypted = toByteArray(encryptedStream)
        assertThat(encrypted).isNotEqualTo(originalTekst.toByteArray())

        val decrypted = encryptor.decrypt(fnr, encrypted)
        assertThat(decrypted).isEqualTo(originalTekst.toByteArray())
    }

    @Throws(IOException::class)
    private fun toByteArray(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()

        inputStream.copyTo(buffer,65536)

        return buffer.toByteArray()
    }

    companion object {

        private val originalTekst = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        private val fnr = "dummyfnr"
    }

}
