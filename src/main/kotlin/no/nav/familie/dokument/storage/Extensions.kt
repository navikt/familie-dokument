package no.nav.familie.dokument.storage

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import java.security.MessageDigest
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


fun TokenValidationContextHolder.hentFnr(): String {
    return this.tokenValidationContext.getJwtToken("selvbetjening").subject
}

