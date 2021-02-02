package no.nav.familie.dokument.storage

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import java.security.MessageDigest
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


fun TokenValidationContextHolder.hentFnr(): String {
    return this.tokenValidationContext.getJwtToken("selvbetjening").subject
}

fun TokenValidationContextHolder.hentFnrHash(pepper: String): String {
    require(pepper.isNotBlank(), { "Pepper kan ikke være tom" })
    return lagDigest("${hentFnr()}$pepper}")
}

fun lagDigest(s: String): String {
    val instance = MessageDigest.getInstance("SHA-256")
    val digest = instance.digest(s.toByteArray())
    return Base64.getEncoder().encodeToString(digest)
}