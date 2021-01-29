package no.nav.familie.dokument.storage

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import java.security.MessageDigest
import java.util.*

fun TokenValidationContextHolder.hentFnr(): String {
    return this.tokenValidationContext.getJwtToken("selvbetjening").subject
}

fun TokenValidationContextHolder.hentFnrHash(hemmeligPepper: String): String {
        require(hemmeligPepper.isNotBlank(), { "Pepper(secret salt) kan ikke være tom" })
        val instance = MessageDigest.getInstance("SHA-256")
        instance.update(hemmeligPepper.toByteArray())
        return  Base64.getEncoder().encodeToString(instance.digest(hentFnr().toByteArray()))
}