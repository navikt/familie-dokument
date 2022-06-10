package no.nav.familie.dokument.storage.encryption

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64

@Component
class Hasher(
    @Value("\${FAMILIE_DOKUMENT_FNR_SECRET_SALT}")
    val hemmeligSalt: String
) {

    fun lagFnrHash(fnr: String): String {
        require(hemmeligSalt.isNotBlank(), { "hemmeligSalt kan ikke være tom" })
        require(fnr.isNotBlank(), { "Fnr kan ikke være tom" })
        val s = fnr + hemmeligSalt
        val instance = MessageDigest.getInstance("SHA-256")
        val digest = instance.digest(s.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }
}
