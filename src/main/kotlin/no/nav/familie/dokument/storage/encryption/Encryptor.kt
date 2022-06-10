package no.nav.familie.dokument.storage.encryption

import java.io.InputStream
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec

class Encryptor(private val secretKeyProvider: SecretKeyProvider) {

    fun encryptedStream(fnr: String, inputStream: InputStream): InputStream {
        return CipherInputStream(inputStream, initCipher(Cipher.ENCRYPT_MODE, fnr))
    }

    fun decrypt(fnr: String, input: ByteArray): ByteArray {
        return transform(fnr, input)
    }

    private fun transform(fnr: String, input: ByteArray): ByteArray {
        try {
            val cipher = initCipher(Cipher.DECRYPT_MODE, fnr)
            return cipher.doFinal(input)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Kunne ikke opprette cipher", e)
        }
    }

    private fun initCipher(cipherTransformation: Int, fnr: String): Cipher {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(cipherTransformation, secretKeyProvider.key(fnr), GCMParameterSpec(128, fnr.toByteArray()))
            return cipher
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Kunne ikke opprette cipher", e)
        }
    }

    companion object {

        private val ALGORITHM = "AES/GCM/NoPadding"
    }
}
