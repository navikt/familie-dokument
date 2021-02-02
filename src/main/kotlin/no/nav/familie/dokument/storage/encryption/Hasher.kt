package no.nav.familie.dokument.storage.encryption

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.*

@Component
class Hasher(@Value("\${FAMILIE_DOKUMENT_FNR_SECRET_SALT}")
             val pepper: String) {

    fun lagFnrDigest(fnr: String): String {
        require(pepper.isNotBlank(), { "Pepper kan ikke være tom" })
        require(fnr.isNotBlank(), { "Fnr kan ikke være tom" })
        val s = fnr + pepper
        val instance = MessageDigest.getInstance("SHA-256")
        val digest = instance.digest(s.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }


}
