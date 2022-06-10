package no.nav.familie.dokument.storage.encryption

import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecretKeyProvider(private val passphrase: String) {

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun key(salt: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt.toByteArray(), 10000, 256)
        val key = factory.generateSecret(spec)
        return SecretKeySpec(key.encoded, "AES")
    }
}
